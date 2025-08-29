package com.example.examplemod.core.spells.parameters;

/**
 * Типы маны в двойной системе ресурсов
 * 
 * Согласно концепту:
 * - Мана Инициации: фиксированная стоимость для начала каста (безвозвратно)
 * - Мана Усиления: переменная стоимость во время QTE (определяет силу заклинания)
 */
public enum ManaType {
    
    /**
     * Мана Инициации — фиксированная стоимость для начала каста
     * - Тратится безвозвратно при активации заклинания
     * - Без неё невозможно начать каст
     * - Рассчитывается как функция от формы и базовых параметров
     */
    INITIATION("initiation_mana"),
    
    /**
     * Мана Усиления — переменный ресурс, расходуемый во время QTE
     * - Количество потраченной маны определяет силу заклинания
     * - Ограничена только максимальным запасом игрока
     * - Может тратиться постоянно для поддерживаемых заклинаний
     */
    AMPLIFICATION("amplification_mana"),
    
    /**
     * Специальный тип для поддерживания заклинаний
     * Мана/сек для BEAM, BARRIER, AREA
     */
    CHANNEL_PER_SECOND("channel_mana_per_second");
    
    private final String id;
    
    ManaType(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    /**
     * Получить базовый коэффициент стоимости для этого типа маны
     */
    public float getBaseCostMultiplier() {
        return switch (this) {
            case INITIATION -> 1.0f;        // Базовая стоимость
            case AMPLIFICATION -> 0.5f;     // Дешевле, но может тратиться много
            case CHANNEL_PER_SECOND -> 0.3f; // Самая дешёвая за тик
        };
    }
    
    /**
     * Проверить, может ли этот тип маны восстанавливаться во время каста
     */
    public boolean canRecoverDuringCast() {
        return switch (this) {
            case INITIATION -> false;           // Тратится безвозвратно
            case AMPLIFICATION -> false;        // Тратится активно
            case CHANNEL_PER_SECOND -> true;    // Может восстанавливаться между тиками
        };
    }
}