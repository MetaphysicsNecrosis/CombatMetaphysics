package com.example.examplemod.client.qte;

/**
 * Результат выполнения QTE с информацией об эффективности
 * SINGLEPLAYER: Используется для локальной обработки магических комбо
 */
public class QTEResult {
    
    public enum EfficiencyLevel {
        BONUS(1.5f, "Bonus efficiency (+50%)"),
        NORMAL(1.0f, "Normal efficiency"),
        REDUCED(0.7f, "Reduced efficiency (-30%)"),
        FAILED(0f, "Combo failed");
        
        private final float multiplier;
        private final String description;
        
        EfficiencyLevel(float multiplier, String description) {
            this.multiplier = multiplier;
            this.description = description;
        }
        
        public float getMultiplier() { return multiplier; }
        public String getDescription() { return description; }
    }
    
    private final boolean success;
    private final float score;                    // Оценка от 0.0 до 1.0
    private final EfficiencyLevel efficiency;
    private final String nextSpell;             // Следующее заклинание в комбо (может быть null)
    private final float manaCostMultiplier;     // Модификатор стоимости маны
    private final String message;              // Описание результата для UI
    
    public QTEResult(boolean success, float score, EfficiencyLevel efficiency, 
                    String nextSpell, float manaCostMultiplier, String message) {
        this.success = success;
        this.score = Math.max(0f, Math.min(1f, score)); // Ограничиваем 0-1
        this.efficiency = efficiency;
        this.nextSpell = nextSpell;
        this.manaCostMultiplier = manaCostMultiplier;
        this.message = message;
    }
    
    // === СТАТИЧЕСКИЕ МЕТОДЫ СОЗДАНИЯ ===
    
    /**
     * Создает успешный результат QTE
     */
    public static QTEResult success(float score, EfficiencyLevel efficiency, String nextSpell) {
        // Вычисляем модификатор стоимости маны на основе эффективности
        float manaCost = switch (efficiency) {
            case BONUS -> 0.8f;      // -20% стоимость маны
            case NORMAL -> 1.0f;     // Обычная стоимость
            case REDUCED -> 1.2f;    // +20% стоимость маны
            case FAILED -> 1.5f;     // +50% штраф (не должно вызываться для success)
        };
        
        String message = String.format("QTE Success! Score: %.0f%% - %s", 
                                     score * 100, efficiency.getDescription());
        
        return new QTEResult(true, score, efficiency, nextSpell, manaCost, message);
    }
    
    /**
     * Создает провальный результат QTE
     */
    public static QTEResult failure(float score, String reason) {
        String message = String.format("QTE Failed: %s (Score: %.0f%%)", reason, score * 100);
        return new QTEResult(false, score, EfficiencyLevel.FAILED, null, 0f, message);
    }
    
    /**
     * Создает результат прерванного QTE
     */
    public static QTEResult interrupted(String reason) {
        String message = "QTE Interrupted: " + reason;
        return new QTEResult(false, 0f, EfficiencyLevel.FAILED, null, 0f, message);
    }
    
    // === ГЕТТЕРЫ ===
    
    public boolean isSuccess() { return success; }
    public float getScore() { return score; }
    public EfficiencyLevel getEfficiency() { return efficiency; }
    public String getNextSpell() { return nextSpell; }
    public float getManaCostMultiplier() { return manaCostMultiplier; }
    public String getMessage() { return message; }
    
    /**
     * Возвращает оценку в процентах (0-100)
     */
    public int getScorePercentage() {
        return Math.round(score * 100);
    }
    
    /**
     * Проверяет, может ли продолжаться комбо
     */
    public boolean canContinueCombo() {
        return success && nextSpell != null && !nextSpell.isEmpty();
    }
    
    /**
     * Возвращает множитель эффективности заклинания
     */
    public float getSpellEfficiencyMultiplier() {
        return efficiency.getMultiplier();
    }
    
    /**
     * Возвращает цвет для отображения результата в UI
     */
    public int getDisplayColor() {
        return switch (efficiency) {
            case BONUS -> 0xFFD700;      // Золотой
            case NORMAL -> 0x00FF00;     // Зеленый
            case REDUCED -> 0xFFFF00;    // Желтый
            case FAILED -> 0xFF0000;     // Красный
        };
    }
    
    /**
     * Возвращает краткое описание результата для быстрого отображения
     */
    public String getShortDescription() {
        if (!success) {
            return "FAILED";
        }
        
        return switch (efficiency) {
            case BONUS -> "PERFECT!";
            case NORMAL -> "SUCCESS";
            case REDUCED -> "OK";
            case FAILED -> "FAILED";
        };
    }
    
    /**
     * Возвращает общую эффективность QTE (для CombatClientManager)
     */
    public float getOverallEfficiency() {
        return efficiency.multiplier;
    }
    
    
    @Override
    public String toString() {
        return String.format("QTEResult{success=%s, score=%.2f, efficiency=%s, nextSpell='%s'}", 
                           success, score, efficiency, nextSpell);
    }
}