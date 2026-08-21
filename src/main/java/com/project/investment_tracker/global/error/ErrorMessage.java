package com.project.investment_tracker.global.error;

public final class ErrorMessage {

    public static final String INVESTMENT_PLAN_NOT_FOUND = "투자 계획을 찾을 수 없습니다.";
    public static final String PLAN_ACTION_NOT_FOUND = "계획 액션을 찾을 수 없습니다.";
    public static final String TRADE_NOT_FOUND = "거래 기록을 찾을 수 없습니다.";
    public static final String PLAN_ACTION_NOT_BELONG_TO_PLAN = "해당 투자 계획에 속한 액션이 아닙니다.";
    public static final String INVALID_REQUEST = "잘못된 요청입니다.";
    public static final String ACCOUNT_NOT_FOUND = "계좌를 찾을 수 없습니다.";
    public static final String STOCK_HOLDING_NOT_FOUND = "보유 종목을 찾을 수 없습니다.";
    public static final String INSUFFICIENT_CASH_BALANCE = "현금 잔고가 부족합니다.";
    public static final String INSUFFICIENT_STOCK_QUANTITY = "보유 수량이 부족합니다.";
    public static final String TRADE_HAS_LATER_TRADE = "이후 거래가 있어 수정하거나 삭제할 수 없습니다.";

    private ErrorMessage() {
    }
}
