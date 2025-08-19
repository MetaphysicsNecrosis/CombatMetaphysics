package com.example.examplemod.core.pipeline;

import java.util.UUID;

/**
 * Приостановленное действие (для QTE, каналирования и т.д.)
 */
public interface SuspendedAction {
    UUID getId();
    UUID getPlayerId();
    String getType();
    void tick();
    boolean isTimedOut();
    void resume(Object result);
    void timeout();
    long getCreationTime();
    long getTimeoutDuration();
}