package com.example.examplemod.client.qte;

/**
 * OSU-style hit point с точными timing windows для QTE системы
 * SINGLEPLAYER: Вся обработка локальная, без сетевой синхронизации
 */
public class QTEHitPoint {
    
    // OSU-style timing windows (в миллисекундах от target time)
    private static final long PERFECT_WINDOW = 25;    // ±25ms = PERFECT (1.0 эффективность)
    private static final long GREAT_WINDOW = 50;      // ±50ms = GREAT (0.9 эффективность)
    private static final long GOOD_WINDOW = 100;      // ±100ms = GOOD (0.7 эффективность)
    private static final long OK_WINDOW = 150;        // ±150ms = OK (0.5 эффективность)
    private static final long MISS_WINDOW = 200;      // ±200ms = MISS (0.0 эффективность)
    
    // Основные параметры hit point
    private final long targetTime;          // ТОЧНОЕ время попадания (мс от начала QTE)
    private final int keyCode;              // Требуемая клавиша (GLFW код)
    private final float x, y;               // Позиция на экране (для будущих фич)
    
    // Состояние и результаты
    private HitResult result = HitResult.WAITING;
    private float efficiency = 0f;          // Эффективность 0.0-1.0 (для магической системы)
    private long actualHitTime = -1;        // Фактическое время нажатия
    private long timingDifference = 0;      // Отклонение от идеального времени
    private boolean processed = false;      // Был ли обработан input
    
    public enum HitResult {
        WAITING,        // Ожидает нажатия
        PERFECT,        // ±25ms - 1.0 эффективность - ЗОЛОТОЙ
        GREAT,          // ±50ms - 0.9 эффективность - ЗЕЛЕНЫЙ
        GOOD,           // ±100ms - 0.7 эффективность - ЖЕЛТЫЙ  
        OK,             // ±150ms - 0.5 эффективность - ОРАНЖЕВЫЙ
        MISS,           // ±200ms+ или не нажал - 0.0 эффективность
        TOO_EARLY       // Нажал до появления круга
    }
    
    public QTEHitPoint(long targetTime, int keyCode) {
        this(targetTime, keyCode, 0, 0); // Центральная позиция по умолчанию
    }
    
    public QTEHitPoint(long targetTime, int keyCode, float x, float y) {
        this.targetTime = targetTime;
        this.keyCode = keyCode;
        this.x = x;
        this.y = y;
    }
    
    /**
     * SINGLEPLAYER: Локальная обработка input без сетевой задержки
     * @param inputTime время нажатия клавиши (System.currentTimeMillis())
     * @param pressedKey код нажатой клавиши
     * @return результат попадания
     */
    public HitResult processInput(long inputTime, int pressedKey) {
        if (processed || result != HitResult.WAITING) {
            return result; // Уже обработан
        }
        
        // Проверяем правильность клавиши
        if (pressedKey != keyCode) {
            return result; // Неправильная клавиша - игнорируем
        }
        
        this.actualHitTime = inputTime;
        this.timingDifference = Math.abs(inputTime - targetTime);
        this.processed = true;
        
        // Проверяем слишком раннее нажатие (до появления круга)
        if (inputTime < targetTime - MISS_WINDOW) {
            this.result = HitResult.TOO_EARLY;
            this.efficiency = 0f;
        } 
        // Определяем точность попадания
        else if (timingDifference <= PERFECT_WINDOW) {
            this.result = HitResult.PERFECT;
            this.efficiency = 1.0f;
        } else if (timingDifference <= GREAT_WINDOW) {
            this.result = HitResult.GREAT; 
            this.efficiency = 0.9f;
        } else if (timingDifference <= GOOD_WINDOW) {
            this.result = HitResult.GOOD;
            this.efficiency = 0.7f;
        } else if (timingDifference <= OK_WINDOW) {
            this.result = HitResult.OK;
            this.efficiency = 0.5f;
        } else {
            this.result = HitResult.MISS;
            this.efficiency = 0f;
        }
        
        return this.result;
    }
    
    /**
     * Проверяет, истекло ли время для попадания (автоматический MISS)
     * @param currentTime текущее время
     * @return true если time window истек
     */
    public boolean hasExpired(long currentTime) {
        return !processed && currentTime > targetTime + MISS_WINDOW;
    }
    
    /**
     * Принудительно устанавливает MISS если время истекло
     * @param currentTime текущее время
     */
    public void forceExpireIfNeeded(long currentTime) {
        if (hasExpired(currentTime) && result == HitResult.WAITING) {
            this.result = HitResult.MISS;
            this.efficiency = 0f;
            this.processed = true;
            this.actualHitTime = currentTime;
            this.timingDifference = currentTime - targetTime;
        }
    }
    
    /**
     * Вычисляет прогресс приближения круга (для визуализации)
     * @param currentTime текущее время
     * @param approachDuration время приближения круга (обычно 2000ms)
     * @return прогресс от 0.0 (начало) до 1.0 (достиг цели)
     */
    public float getApproachProgress(long currentTime, long approachDuration) {
        long approachStartTime = targetTime - approachDuration;
        
        if (currentTime <= approachStartTime) {
            return 0f; // Еще не начал приближаться
        }
        if (currentTime >= targetTime) {
            return 1f; // Достиг цели
        }
        
        return (float)(currentTime - approachStartTime) / approachDuration;
    }
    
    /**
     * Определяет, должен ли быть виден hit point в данный момент
     * @param currentTime текущее время
     * @param approachDuration время приближения
     * @param displayAfterMiss время показа после промаха
     * @return true если должен отображаться
     */
    public boolean shouldBeVisible(long currentTime, long approachDuration, long displayAfterMiss) {
        long approachStartTime = targetTime - approachDuration;
        long hideTime = targetTime + displayAfterMiss;
        
        return currentTime >= approachStartTime && currentTime <= hideTime;
    }
    
    /**
     * Возвращает цвет для отображения результата
     * @return RGB цвет в формате 0xRRGGBB
     */
    public int getResultColor() {
        return switch (result) {
            case PERFECT -> 0xFFD700;      // Золотой
            case GREAT -> 0x00DD00;        // Ярко-зеленый
            case GOOD -> 0xFFFF00;         // Желтый
            case OK -> 0xFF8800;           // Оранжевый
            case MISS, TOO_EARLY -> 0xFF0000; // Красный
            case WAITING -> 0xFFFFFF;      // Белый (ожидание)
        };
    }
    
    /**
     * Возвращает текстовое описание результата для UI
     */
    public String getResultText() {
        return switch (result) {
            case PERFECT -> "PERFECT!";
            case GREAT -> "GREAT";
            case GOOD -> "GOOD";
            case OK -> "OK";
            case MISS -> "MISS";
            case TOO_EARLY -> "TOO EARLY";
            case WAITING -> "";
        };
    }
    
    /**
     * Возвращает размер круга для результата (для анимации)
     */
    public float getResultScale() {
        return switch (result) {
            case PERFECT -> 1.5f;      // Самый большой
            case GREAT -> 1.3f;
            case GOOD -> 1.1f;
            case OK -> 1.0f;
            case MISS, TOO_EARLY -> 0.8f; // Самый маленький
            case WAITING -> 1.0f;
        };
    }
    
    // === ГЕТТЕРЫ ===
    
    public long getTargetTime() { return targetTime; }
    public int getKeyCode() { return keyCode; }
    public float getX() { return x; }
    public float getY() { return y; }
    public HitResult getResult() { return result; }
    public float getEfficiency() { return efficiency; }
    public long getActualHitTime() { return actualHitTime; }
    public long getTimingDifference() { return timingDifference; }
    public boolean isProcessed() { return processed; }
    
    /**
     * Возвращает оценку в процентах для UI (0-100%)
     */
    public int getScorePercentage() {
        return Math.round(efficiency * 100);
    }
    
    /**
     * Проверяет, был ли это успешный hit (не MISS и не TOO_EARLY)
     */
    public boolean isSuccessfulHit() {
        return result != HitResult.MISS && result != HitResult.TOO_EARLY && result != HitResult.WAITING;
    }
    
    
    @Override
    public String toString() {
        return String.format("QTEHitPoint{targetTime=%d, key=%d, result=%s, efficiency=%.2f, timingDiff=%d}", 
                           targetTime, keyCode, result, efficiency, timingDifference);
    }
}