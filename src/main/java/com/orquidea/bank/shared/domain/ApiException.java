package com.orquidea.bank.shared.domain;

import com.orquidea.bank.shared.api.ApiErrorBody;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String field;
    private final Map<String, Object> params;

    public ApiException(HttpStatus status, String code) {
        this(status, code, null, null);
    }

    public ApiException(HttpStatus status, String code, String field, Map<String, Object> params) {
        super(code);
        this.status = status;
        this.code = code;
        this.field = field;
        this.params = params;
    }

    public static ApiException notFound(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, code);
    }

    public static ApiException badRequest(String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, code);
    }

    public static ApiException unprocessable(String code) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code);
    }

    public static ApiException unprocessable(String code, String field, Map<String, Object> params) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, field, params);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public ApiErrorBody toBody() {
        return ApiErrorBody.of(code, field, params);
    }
}
