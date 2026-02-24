package com.hae.shop.common;

public enum ErrorCode {
    COMMON_SYSTEM_ERROR("C001", "시스템 오류가 발생했습니다."),
    INVALID_INPUT("C002", "잘못된 입력값입니다."),
    UNAUTHORIZED("C003", "인증이 필요합니다."),
    FORBIDDEN("C004", "접근 권한이 없습니다."),
    NOT_FOUND("C005", "리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE("C006", "이미 존재하는 리소스입니다."),

    MEMBER_NOT_FOUND("M001", "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS("M002", "이미 가입된 이메일입니다."),
    INVALID_PASSWORD("M003", "비밀번호가 일치하지 않습니다."),
    INVALID_CREDENTIALS("M004", "아이디 또는 비밀번호가 올바르지 않습니다."),

    PRODUCT_NOT_FOUND("P001", "상품을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK("P002", "재고가 부족합니다."),
    PRODUCT_NOT_ACTIVE("P003", "판매 중인 상품이 아닙니다."),

    ORDER_NOT_FOUND("O001", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_EXISTS("O002", "이미 처리된 주문입니다."),
    PAYMENT_FAILED("O003", "결제에 실패했습니다."),
    INVALID_ORDER_STATUS("O004", "주문 상태가 올바르지 않습니다."),
    IDEMPOTENCY_KEY_CONFLICT("O005", "중복 요청입니다."),
    PAYMENT_CANCEL_FAILED("O006", "결제 취소에 실패했습니다."),

    COUPON_NOT_FOUND("CP001", "쿠폰을 찾을 수 없습니다."),
    COUPON_EXPIRED("CP002", "만료된 쿠폰입니다."),
    COUPON_NOT_AVAILABLE("CP003", "사용 불가능한 쿠폰입니다."),
    COUPON_ALREADY_USED("CP004", "이미 사용된 쿠폰입니다."),
    COUPON_QUOTA_EXCEEDED("CP005", "쿠폰 수량이 소진되었습니다."),
    MIN_PURCHASE_NOT_MET("CP006", "최소 구매 금액을 충족하지 않았습니다."),
    PAYMENT_GATEWAY_ERROR("PG001", "결제 gateway 오류가 발생했습니다."),
    PAYMENT_TIMEOUT("PG002", "결제 시간이 초과되었습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
