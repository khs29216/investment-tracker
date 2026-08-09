package com.project.investment_tracker.entity;

public enum ActionStatus {
    PENDING,    // 아직 실행 안 됨
    EXECUTED,   // 실행됨
    SKIPPED,    // 조건은 충족하지만, 실행하지 않음
    CANCELLED   // 해당 액션 취소
}
