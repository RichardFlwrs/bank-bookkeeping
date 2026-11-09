#!/usr/bin/env python3
"""Run one card purchase and read the books after it.

Calls the four endpoints that show the walk:

    POST /api/purchases
    GET  /api/purchases/{purchaseId}
    GET  /api/ledger/accounts/{code}
    GET  /api/audit?traceId=

The API must already be running and seeded (./mvnw spring-boot:run).
Each run sends a new Idempotency-Key, so balances are compared to a
snapshot taken just before the purchase.

    python3 e2e/purchase-walk/purchase_walk.py
    python3 e2e/purchase-walk/purchase_walk.py --amount 45.00 --card-token card_ada_orquidea

A later Locust file can import the Call builders below and send them
with HttpUser.client. This script stays a single-user check.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass
from decimal import Decimal

DEFAULT_BASE_URL = "http://localhost:8086"
REQUEST_TIMEOUT_SECONDS = 300
DEFAULT_CARD_TOKEN = "card_ada_orquidea"
DEFAULT_AMOUNT = "45.00"
FEE_THRESHOLD = Decimal("40.00")
FEE = Decimal("1.50")
ZERO = Decimal("0.00")
CENT = Decimal("0.01")

# Accounts the purchase moves. Cash (1000) is untouched and is not read.
WATCHED_ACCOUNTS = ("2100", "2200", "2300", "4100")
CUSTOMER = "2100"
MERCHANT = "2200"
SUSPENSE = "2300"
INCOME = "4100"

STAGES = (
    ("authorize", "AUTHORIZED"),
    ("clear", "CLEARED"),
    ("postCustomer", "SETTLED"),
    ("settleMerchant", "POSTED"),
)
AUDIT_ACTIONS = (
    "PURCHASE_AUTHORIZED",
    "PURCHASE_CLEARED",
    "PURCHASE_CUSTOMER_POSTED",
    "PURCHASE_SETTLED",
)


@dataclass(frozen=True)
class Call:
    """One HTTP call. Locust can send method/path/body/headers/query as-is."""

    method: str
    path: str
    body: dict | None = None
    headers: dict | None = None
    query: dict | None = None

    def label(self) -> str:
        if self.query:
            return f"{self.method} {self.path}?{urllib.parse.urlencode(self.query)}"
        return f"{self.method} {self.path}"


def post_purchase(card_token: str, amount: str, idempotency_key: str) -> Call:
    return Call(
        "POST",
        "/api/purchases",
        body={"cardToken": card_token, "amount": amount},
        headers={"Idempotency-Key": idempotency_key},
    )


def get_purchase(purchase_id: str) -> Call:
    return Call("GET", f"/api/purchases/{urllib.parse.quote(purchase_id, safe='')}")


def get_account(code: str) -> Call:
    return Call("GET", f"/api/ledger/accounts/{urllib.parse.quote(code, safe='')}")


def get_audit(trace_id: str) -> Call:
    return Call("GET", "/api/audit", query={"traceId": trace_id})


def fee_for(amount: Decimal) -> Decimal:
    if amount >= FEE_THRESHOLD:
        return FEE
    return ZERO


def money(value: str) -> Decimal:
    return Decimal(value).quantize(CENT)


class ApiError(Exception):
    def __init__(self, call: Call, status: int, body: str):
        self.call = call
        self.status = status
        self.body = body
        super().__init__(f"{call.label()} -> {status} {body}")


class BankApi:
    def __init__(self, base_url: str, timeout: float = REQUEST_TIMEOUT_SECONDS):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def send(self, call: Call):
        url = self.base_url + call.path
        if call.query:
            url += "?" + urllib.parse.urlencode(call.query)
        headers = {"Accept": "application/json"}
        if call.headers:
            headers.update(call.headers)
        data = None
        if call.body is not None:
            data = json.dumps(call.body).encode()
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(url, data=data, headers=headers, method=call.method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                return response.status, json.loads(response.read().decode())
        except urllib.error.HTTPError as exc:
            raise ApiError(call, exc.code, exc.read().decode()) from exc
        except urllib.error.URLError as exc:
            raise SystemExit(
                f"Cannot reach {self.base_url} ({exc.reason}). Start the API with ./mvnw spring-boot:run"
            ) from exc


def check_walk(amount: Decimal, before: dict, trail: dict, detail: dict, after: dict, audit: list) -> list[str]:
    problems: list[str] = []
    fee = fee_for(amount)
    merchant_credit = amount - fee
    customer = before[CUSTOMER]

    steps = trail.get("steps") or []
    if trail.get("status") != "POSTED":
        problems.append(f"purchase status is {trail.get('status')}, expected POSTED")
    if len(steps) != 4:
        problems.append(f"trail has {len(steps)} steps, expected 4")

    expected_available = money(customer["available"]) - amount
    expected_posted_after = money(customer["posted"]) - amount
    for index, (stage, status) in enumerate(STAGES):
        if index >= len(steps):
            break
        step = steps[index]
        if step.get("stage") != stage or step.get("purchaseStatus") != status:
            problems.append(
                f"step {index} is {step.get('stage')}/{step.get('purchaseStatus')}, expected {stage}/{status}"
            )
        if money(step["available"]) != expected_available:
            problems.append(f"{stage} available is {step['available']}, expected {expected_available}")
        expected_posted = money(customer["posted"]) if index < 2 else expected_posted_after
        if money(step["posted"]) != expected_posted:
            problems.append(f"{stage} posted is {step['posted']}, expected {expected_posted}")

    if detail.get("purchaseId") != trail.get("purchaseId"):
        problems.append("purchase detail id does not match the trail")
    if detail.get("traceId") != trail.get("traceId"):
        problems.append("purchase detail traceId does not match the trail")
    if detail.get("status") != "POSTED":
        problems.append(f"detail status is {detail.get('status')}, expected POSTED")
    hold = detail.get("hold") or {}
    clearing = detail.get("clearingItem") or {}
    settlement = detail.get("settlement") or {}
    if hold.get("status") != "RELEASED":
        problems.append(f"hold status is {hold.get('status')}, expected RELEASED")
    if not clearing or money(clearing["amount"]) != amount:
        problems.append(f"clearing amount is {clearing.get('amount')}, expected {amount}")
    if not settlement or money(settlement["amount"]) != merchant_credit:
        problems.append(f"settlement amount is {settlement.get('amount')}, expected {merchant_credit}")
    if not settlement or money(settlement["feeAmount"]) != fee:
        problems.append(f"settlement fee is {settlement.get('feeAmount')}, expected {fee}")

    expect = {
        CUSTOMER: -amount,
        MERCHANT: merchant_credit,
        SUSPENSE: ZERO,
        INCOME: fee,
    }
    for code, delta in expect.items():
        posted_delta = money(after[code]["posted"]) - money(before[code]["posted"])
        available_delta = money(after[code]["available"]) - money(before[code]["available"])
        if posted_delta != delta:
            problems.append(f"account {code} posted moved {posted_delta}, expected {delta}")
        if available_delta != delta:
            problems.append(f"account {code} available moved {available_delta}, expected {delta}")

    trace_id = trail.get("traceId")
    purchase_id = trail.get("purchaseId")
    actions = set()
    for event in audit:
        actions.add(event.get("action"))
        if event.get("traceId") != trace_id or event.get("purchaseId") != purchase_id:
            problems.append(f"audit {event.get('id')} is not for this purchase")
        if event.get("actor") != "purchase":
            problems.append(f"audit {event.get('id')} actor is {event.get('actor')}, expected purchase")
    missing = [action for action in AUDIT_ACTIONS if action not in actions]
    if missing:
        problems.append("audit is missing " + ", ".join(missing))
    return problems


def print_report(trail: dict, detail: dict, before: dict, after: dict, audit: list) -> None:
    print("\nPOST /api/purchases")
    print(f"  purchaseId {trail['purchaseId']}")
    print(f"  traceId    {trail['traceId']}")
    print(f"  status     {trail['status']}")
    for step in trail["steps"]:
        print(
            f"  {step['stage']:<14} {step['purchaseStatus']:<12} "
            f"available {step['available']:>10}  posted {step['posted']:>10}"
        )

    settlement = detail.get("settlement") or {}
    hold = detail.get("hold") or {}
    print(f"\nGET /api/purchases/{detail['purchaseId']}")
    print(
        f"  hold {hold.get('status')}  "
        f"clearing {((detail.get('clearingItem') or {}).get('amount'))}  "
        f"settlement {settlement.get('amount')}  fee {settlement.get('feeAmount')}"
    )

    print("\nGET /api/ledger/accounts/{code}")
    for code in WATCHED_ACCOUNTS:
        print(
            f"  {code}  posted {before[code]['posted']} -> {after[code]['posted']}"
            f"   available {before[code]['available']} -> {after[code]['available']}"
        )

    print(f"\nGET /api/audit?traceId={trail['traceId']}")
    for event in audit:
        print(f"  {event['action']:<28} {event['aggregateType']}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Call the four purchase-walk endpoints once.")
    parser.add_argument("--base-url", default=os.environ.get("BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--card-token", default=DEFAULT_CARD_TOKEN)
    parser.add_argument("--amount", default=DEFAULT_AMOUNT)
    parser.add_argument(
        "--idempotency-key",
        default=f"e2e-{uuid.uuid4()}",
        help="Defaults to a new key so the walk is not replayed.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    amount = money(args.amount)
    api = BankApi(args.base_url)

    before = {}
    for code in WATCHED_ACCOUNTS:
        _status, before[code] = api.send(get_account(code))

    purchase_call = post_purchase(args.card_token, format(amount, "f"), args.idempotency_key)
    try:
        _status, trail = api.send(purchase_call)
    except ApiError as exc:
        print(exc, file=sys.stderr)
        return 1

    _status, detail = api.send(get_purchase(trail["purchaseId"]))
    after = {}
    for code in WATCHED_ACCOUNTS:
        _status, after[code] = api.send(get_account(code))
    _status, audit = api.send(get_audit(trail["traceId"]))

    print_report(trail, detail, before, after, audit)
    problems = check_walk(amount, before, trail, detail, after, audit)
    if problems:
        print("\nFailed:")
        for problem in problems:
            print(f"  {problem}")
        return 1
    print("\nPurchase walk matches the four endpoints.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
