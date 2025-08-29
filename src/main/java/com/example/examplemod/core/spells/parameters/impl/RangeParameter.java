package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр дистанции действия
 * Определяет дальность для PROJECTILE, BEAM, CHAIN
 */
public class RangeParameter extends AbstractSpellParameter<Float> {
    
    public RangeParameter() {
        super("range", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseRange = parseFloat(inputValue, 20.0f); // 20 блоков по умолчанию
        if (baseRange <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalRange = baseRange;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "PROJECTILE" -> {
                finalRange *= 1.0f; // Базовая дальность
            }
            case "BEAM" -> {
                finalRange *= 1.2f; // Лучи летят дальше
                // Луч проверяет препятствия по всей длине
            }
            case "CHAIN" -> {
                finalRange *= 0.7f; // Дальность перехода между целями
                // Каждый переход может иметь свою дальность
            }
            case "AREA", "BARRIER" -> {
                finalRange *= 0.4f; // Дальность размещения зоны/барьера
                // Не влияет на размер, только на дальность каста
            }
            case "WAVE" -> {
                finalRange *= 0.6f; // Дальность начальной точки волны
                // Волна может расшириться за пределы начальной дальности
            }
            case "TOUCH" -> {
                finalRange = Math.min(finalRange, 3.0f); // Контакт ограничен
            }
            case "WEAPON_ENCHANT" -> {
                finalRange = 1.0f; // Только на собственное оружие
            }
            case "INSTANT_POINT" -> {
                finalRange *= 0.8f; // Дальность размещения мгновенного эффекта
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Проникающие элементы увеличивают дальность
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 0.5f;
            finalRange *= (1.0f + lightningBonus);
        }
        
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 0.4f;
            finalRange *= (1.0f + airBonus);
        }
        
        // Тяжелые элементы уменьшают дальность
        if (context.hasElementalIntensity("earth")) {
            float earthPenalty = context.getElementalIntensity("earth") * 0.3f;
            finalRange *= Math.max(0.5f, 1.0f - earthPenalty);
        }
        
        if (context.hasElementalIntensity("water")) {
            float waterPenalty = context.getElementalIntensity("water") * 0.15f;
            finalRange *= Math.max(0.7f, 1.0f - waterPenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Дальность увеличивает расход маны
        float manaCostMultiplier = 1.0f + (finalRange / baseRange - 1.0f) * 0.6f;
        
        // Дальние заклинания требуют больше времени каста
        float castTimeMultiplier = 1.0f + (finalRange / baseRange - 1.0f) * 0.3f;
        
        // Расчет скорости для достижения цели
        float calculatedSpeed = calculateSpeed(formType, finalRange);
        
        // Дальние заклинания менее точные
        float accuracyPenalty = Math.max(0, (finalRange - 30.0f) * 0.01f);
        
        // Дальность влияет на время полета
        float maxFlightTime = finalRange / Math.max(1.0f, calculatedSpeed);
        
        // Видимость заклинания на дальних дистанциях
        boolean requiresLineOfSight = finalRange > 50.0f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ===
        
        // Очень дальние заклинания получают проникающую способность
        float piercingBonus = Math.max(0, finalRange - 40.0f) * 0.1f;
        
        // Дальние лучи могут проходить через слабые препятствия
        float penetrationPower = 0.0f;
        if ("BEAM".equals(formType) && finalRange > 30.0f) {
            penetrationPower = (finalRange - 30.0f) * 0.5f;
        }
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("range", finalRange)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("cast_time_multiplier", castTimeMultiplier)
            .putValue("calculated_speed", calculatedSpeed)
            .putValue("accuracy_penalty", accuracyPenalty)
            .putValue("max_flight_time", maxFlightTime)
            .putValue("requires_line_of_sight", requiresLineOfSight ? 1.0f : 0.0f)
            .putValue("piercing_bonus", piercingBonus)
            .putValue("penetration_power", penetrationPower)
            .putValue("max_range", finalRange) // Основное значение
            .build();
    }
    
    /**
     * Расчет оптимальной скорости для достижения дальности
     */
    private float calculateSpeed(String formType, float range) {
        return switch (formType) {
            case "PROJECTILE" -> Math.max(5.0f, range / 3.0f); // Быстрые снаряды
            case "BEAM" -> range * 10.0f; // Мгновенные лучи
            case "CHAIN" -> range / 2.0f; // Умеренная скорость перехода
            case "WAVE" -> range / 5.0f; // Медленное расширение
            default -> range / 4.0f; // Средняя скорость
        };
    }
}