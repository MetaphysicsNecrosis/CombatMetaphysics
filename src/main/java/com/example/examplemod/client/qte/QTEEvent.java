package com.example.examplemod.client.qte;

import java.util.List;
import java.util.UUID;

public class QTEEvent {
    private final UUID id;
    private final QTEType type;
    private final long startTime;
    private final long duration;
    private final List<Integer> expectedKeys;
    private final int chainPosition;
    private final float difficultyMultiplier;
    
    // Состояние QTE
    private boolean isActive;
    private boolean isCompleted;
    private float score;
    private int currentKeyIndex;
    
    public QTEEvent(UUID id, QTEType type, long duration, List<Integer> expectedKeys, int chainPosition) {
        this.id = id;
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.expectedKeys = expectedKeys;
        this.chainPosition = chainPosition;
        this.difficultyMultiplier = type.getDifficultyMultiplier(chainPosition);
        
        this.isActive = true;
        this.isCompleted = false;
        this.score = 0f;
        this.currentKeyIndex = 0;
    }
    
    public boolean processKeyInput(int keyCode) {
        if (!isActive || isCompleted) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > duration) {
            // Timeout
            complete(false);
            return false;
        }
        
        return switch (type) {
            case SEQUENCE -> processSequenceInput(keyCode);
            case TIMING -> processTimingInput(keyCode, currentTime);
            case RAPID -> processRapidInput(keyCode);
            case PRECISION -> processPrecisionInput(keyCode, currentTime);
        };
    }
    
    private boolean processSequenceInput(int keyCode) {
        if (currentKeyIndex >= expectedKeys.size()) {
            return false;
        }
        
        if (expectedKeys.get(currentKeyIndex) == keyCode) {
            currentKeyIndex++;
            if (currentKeyIndex >= expectedKeys.size()) {
                // Последовательность завершена
                float timeRatio = 1.0f - (float)(System.currentTimeMillis() - startTime) / duration;
                score = Math.max(0.5f, timeRatio) * 100f;
                complete(true);
            }
            return true;
        } else {
            // Неправильная клавиша
            complete(false);
            return false;
        }
    }
    
    private boolean processTimingInput(int keyCode, long currentTime) {
        if (expectedKeys.isEmpty() || expectedKeys.get(0) != keyCode) {
            return false;
        }
        
        // Идеальное время - 75% от общего времени
        long perfectTime = startTime + (duration * 3 / 4);
        long timeDiff = Math.abs(currentTime - perfectTime);
        long tolerance = duration / 8; // 12.5% допуск
        
        if (timeDiff <= tolerance) {
            score = Math.max(50f, 100f - (float)timeDiff / tolerance * 50f);
            complete(true);
            return true;
        } else {
            complete(false);
            return false;
        }
    }
    
    private boolean processRapidInput(int keyCode) {
        if (expectedKeys.isEmpty() || expectedKeys.get(0) != keyCode) {
            return false;
        }
        
        currentKeyIndex++; // Считаем нажатия
        
        // Проверяем, достигли ли цели
        int targetPresses = Math.max(3, (int)(difficultyMultiplier * 5));
        if (currentKeyIndex >= targetPresses) {
            long timeElapsed = System.currentTimeMillis() - startTime;
            float timeRatio = 1.0f - (float)timeElapsed / duration;
            score = Math.max(50f, timeRatio * 100f);
            complete(true);
        }
        
        return true;
    }
    
    private boolean processPrecisionInput(int keyCode, long currentTime) {
        if (expectedKeys.isEmpty() || expectedKeys.get(0) != keyCode) {
            return false;
        }
        
        // Precision zone - средние 30% времени
        long zoneStart = startTime + duration * 35 / 100;
        long zoneEnd = startTime + duration * 65 / 100;
        
        if (currentTime >= zoneStart && currentTime <= zoneEnd) {
            // В зоне точности
            long zoneCenter = (zoneStart + zoneEnd) / 2;
            long centerDiff = Math.abs(currentTime - zoneCenter);
            long maxDiff = (zoneEnd - zoneStart) / 2;
            
            score = Math.max(70f, 100f - (float)centerDiff / maxDiff * 30f);
            complete(true);
            return true;
        } else {
            // Вне зоны
            complete(false);
            return false;
        }
    }
    
    private void complete(boolean success) {
        isActive = false;
        isCompleted = true;
        if (!success) {
            score = 0f;
        }
    }
    
    public void forceTimeout() {
        complete(false);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration;
    }
    
    public float getProgress() {
        return switch (type) {
            case SEQUENCE -> (float)currentKeyIndex / expectedKeys.size();
            case TIMING, PRECISION -> {
                long elapsed = System.currentTimeMillis() - startTime;
                yield Math.min(1.0f, (float)elapsed / duration);
            }
            case RAPID -> {
                int targetPresses = Math.max(3, (int)(difficultyMultiplier * 5));
                yield Math.min(1.0f, (float)currentKeyIndex / targetPresses);
            }
        };
    }
    
    // Getters
    public UUID getId() { return id; }
    public QTEType getType() { return type; }
    public long getStartTime() { return startTime; }
    public long getDuration() { return duration; }
    public List<Integer> getExpectedKeys() { return expectedKeys; }
    public int getChainPosition() { return chainPosition; }
    public boolean isActive() { return isActive; }
    public boolean isCompleted() { return isCompleted; }
    public float getScore() { return score; }
    public int getCurrentKeyIndex() { return currentKeyIndex; }
    public float getDifficultyMultiplier() { return difficultyMultiplier; }
}