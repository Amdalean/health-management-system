package com.hms.main.tbk.client;

/**
 * TOP 网关业务异常
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
public class TopApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    private final String subCode;

    public TopApiException(String code, String subCode, String message) {
        super(message);
        this.code = code;
        this.subCode = subCode;
    }

    public String getCode() {
        return code;
    }

    public String getSubCode() {
        return subCode;
    }
}
