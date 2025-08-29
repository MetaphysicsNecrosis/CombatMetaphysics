package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр времени существования эффекта
 * Определяет длительность для BEAM, BARRIER, AREA, WAVE
 */
public class DurationParameter extends AbstractSpellParameter<Float> {
    
    public DurationParameter() {
        super("duration", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseDuration = parseFloat(inputValue, 5.0f); // 5 секунд по умолчанию
        if (baseDuration <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalDuration = baseDuration;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "BARRIER" -> {
                finalDuration *= 1.5f; // Барьеры существуют дольше
                // Барьеры получают бонус к прочности от длительности
            }
            case "AREA" -> {
                finalDuration *= 1.3f; // Зоны тоже живут дольше
                // Более частые тики эффекта
            }
            case "BEAM" -> {
                finalDuration *= 1.0f; // Базовая длительность
                // Непрерывный урон/эффект
            }
            case "WAVE" -> {
                finalDuration *= 0.8f; // Волны расширяются быстрее
                // Скорость расширения зависит от времени
            }
            case "WEAPON_ENCHANT", "TOUCH" -> {
                finalDuration *= 2.0f; // Энчанты живут долго
                // Количество зарядов вместо времени
            }
            case "PROJECTILE" -> {
                // Для снарядов duration = максимальное время полета
                finalDuration *= 0.6f;
            }
            case "CHAIN" -> {
                // Время между переходами цепи
                finalDuration *= 0.3f;
            }
            case "INSTANT_POINT" -> {
                // Для отложенных эффектов
                finalDuration *= 0.2f;
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Стабилизирующие элементы увеличивают длительность
        if (context.hasElementalIntensity("earth")) {
            float earthBonus = context.getElementalIntensity("earth") * 0.4f;
            finalDuration *= (1.0f + earthBonus);
        }
        
        if (context.hasElementalIntensity("ice")) {
            float iceBonus = context.getElementalIntensity("ice") * 0.3f;
            finalDuration *= (1.0f + iceBonus);
        }
        
        // Нестабильные элементы уменьшают длительность но усиливают эффект
        if (context.hasElementalIntensity("lightning")) {
            float lightningPenalty = context.getElementalIntensity("lightning") * 0.2f;
            finalDuration *= Math.max(0.5f, 1.0f - lightningPenalty);
        }
        
        if (context.hasElementalIntensity("fire")) {
            float firePenalty = context.getElementalIntensity("fire") * 0.15f;
            finalDuration *= Math.max(0.7f, 1.0f - firePenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Длительность значительно увеличивает расход маны
        float manaCostMultiplier = 1.0f + (finalDuration / baseDuration - 1.0f) * 1.5f;
        
        // Более долгие заклинания имеют больший кулдаун
        float cooldownMultiplier = 1.0f + (finalDuration / baseDuration - 1.0f) * 0.5f;
        
        // Расчет частоты тиков для непрерывных эффектов
        float tickRate = calculateTickRate(formType, finalDuration);
        
        // Барьеры получают бонус прочности
        float durabilityBonus = 0.0f;
        if ("BARRIER".equals(formType)) {
            durabilityBonus = finalDuration * 10.0f; // 10 HP за секунду
        }
        
        // Энчанты получают количество зарядов
        float charges = 0.0f;
        if ("WEAPON_ENCHANT".equals(formType) || "TOUCH".equals(formType)) {
            charges = Math.max(1, finalDuration / 5.0f); // 1 заряд на 5 секунд
        }
        
        // Длительные эффекты сложнее прервать
        float interruptResistanceBonus = Math.min(0.5f, finalDuration / 20.0f);
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("duration", finalDuration)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("cooldown_multiplier", cooldownMultiplier)
            .putValue("tick_rate", tickRate)
            .putValue("barrier_durability_bonus", durabilityBonus)
            .putValue("enchant_charges", charges)
            .putValue("interrupt_resistance_bonus", interruptResistanceBonus)
            .putValue("max_lifetime_ticks", finalDuration * 20.0f) // В тиках для Minecraft
            .build();
    }
    
    /**
     * Расчет частоты срабатывания эффекта
     */
    private float calculateTickRate(String formType, float duration) {
        return switch (formType) {
            case "AREA" -> Math.max(1.0f, duration / 4.0f); // Каждые 0.25 секунды для зон
            case "BEAM" -> Math.max(2.0f, duration / 2.0f); // Каждые 0.1 секунды для лучей
            case "BARRIER" -> Math.max(0.5f, duration / 10.0f); // Каждые 2 секунды для барьеров
            case "WAVE" -> Math.max(5.0f, duration * 2.0f); // Часто для расширения волны
            default -> 1.0f; // Раз в секунду по умолчанию
        };
    }
}