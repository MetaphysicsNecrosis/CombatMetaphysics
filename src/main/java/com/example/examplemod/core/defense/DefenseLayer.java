package com.example.examplemod.core.defense;

/**
 * Базовый интерфейс для слоев защиты в Layered Defense Model
 */
public interface DefenseLayer {
    /**
     * Обрабатывает событие урона через этот слой защиты
     */
    DefenseLayerResult process(DamageEvent damageEvent);
    
    /**
     * Проверяет, может ли этот слой обработать данное событие урона
     */
    boolean canProcess(DamageEvent damageEvent);
    
    /**
     * Проверяет, активен ли этот слой защиты
     */
    boolean isActive();
    
    /**
     * Получает тип слоя защиты
     */
    LayeredDefenseSystem.DefenseLayerType getLayerType();
    
    /**
     * Получает приоритет слоя (для сортировки)
     */
    int getPriority();
    
    /**
     * Получает описание слоя для отладки
     */
    String getDescription();
}