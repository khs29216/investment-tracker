package com.project.investment_tracker.entity;

public enum ActionStatus {
    PENDING,     // 연결된 거래 수량이 0
    IN_PROGRESS, // 일부 수량 실행
    EXECUTED     // 목표 수량 이상 실행
}
