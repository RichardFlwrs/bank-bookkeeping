package com.orquidea.bank.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorBody(
        String code,
        String field,
        Map<String, Object> params
) {
    public static ApiErrorBody of(String code) {
        return new ApiErrorBody(code, null, null);
    }

    public static ApiErrorBody of(String code, String field, Map<String, Object> params) {
        return new ApiErrorBody(code, field, params == null || params.isEmpty() ? null : params);
    }
}
