package com.example.examplemod.core;

public enum PlayerState {
    IDLE,
    PREPARING,
    CASTING,
    QTE_ACTIVE,
    COOLDOWN,
    INTERRUPTED;

    public boolean canTransitionTo(PlayerState newState) {
        return switch (this) {
            case IDLE -> newState == PREPARING;
            case PREPARING -> newState == CASTING || newState == INTERRUPTED;
            case CASTING -> newState == QTE_ACTIVE || newState == COOLDOWN || newState == INTERRUPTED;
            case QTE_ACTIVE -> newState == CASTING || newState == COOLDOWN || newState == INTERRUPTED;
            case COOLDOWN -> newState == IDLE;
            case INTERRUPTED -> newState == IDLE;
        };
    }

    public boolean isActive() {
        return this != IDLE && this != INTERRUPTED;
    }

    public boolean canBeInterrupted() {
        return this == PREPARING || this == CASTING || this == QTE_ACTIVE;
    }
}