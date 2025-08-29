package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр интенсивности срабатывания
 * Определяет частоту применения эффектов для AREA, BEAM, BARRIER
 */
public class TickRateParameter extends AbstractSpellParameter<Float> {
    
    public TickRateParameter() {
        super("tick_rate", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseTickRate = parseFloat(inputValue, 1.0f); // 1 раз в секунду по умолчанию
        if (baseTickRate <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalTickRate = baseTickRate;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "BEAM" -> {
                finalTickRate *= 4.0f; // Лучи срабатывают очень часто
                // Непрерывное воздействие
            }
            case "AREA" -> {
                finalTickRate *= 1.0f; // Базовая частота для зон
                // Периодические эффекты в области
            }
            case "BARRIER" -> {
                finalTickRate *= 0.5f; // Барьеры обновляются реже
                // Регенерация, проверка целостности
            }
            case "WAVE" -> {
                finalTickRate *= 2.0f; // Волны пульсируют
                // Каждая пульсация - эффект
            }
            case "WEAPON_ENCHANT" -> {
                finalTickRate *= 0.3f; // Энчанты срабатывают при атаках
                // Не постоянное действие
            }
            case "TOUCH" -> {
                finalTickRate *= 1.5f; // Контактное воздействие активное
                // При касании активируется часто
            }
            default -> {
                finalTickRate = 0.0f; // Остальные формы не имеют тиков
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Быстрые элементы увеличивают частоту
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 0.8f;
            finalTickRate *= (1.0f + lightningBonus);
        }
        
        if (context.hasElementalIntensity("fire")) {
            float fireBonus = context.getElementalIntensity("fire") * 0.4f;
            finalTickRate *= (1.0f + fireBonus);
        }
        
        // Медленные элементы снижают частоту
        if (context.hasElementalIntensity("ice")) {
            float icePenalty = context.getElementalIntensity("ice") * 0.3f;
            finalTickRate *= Math.max(0.2f, 1.0f - icePenalty);
        }
        
        if (context.hasElementalIntensity("earth")) {
            float earthPenalty = context.getElementalIntensity("earth") * 0.2f;
            finalTickRate *= Math.max(0.5f, 1.0f - earthPenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Высокая частота значительно увеличивает расход маны
        float tickMultiplier = finalTickRate / baseTickRate;
        float manaCostMultiplier = 1.0f + (tickMultiplier - 1.0f) * 2.0f;
        
        // Частые тики требуют больше вычислительных ресурсов
        float processingLoad = tickMultiplier * 1.5f;
        
        // Высокая частота может перегружать систему
        float stabilityPenalty = Math.max(0, (tickMultiplier - 2.0f) * 0.3f);
        
        // === РАСЧЕТ ИНТЕРВАЛОВ ===
        
        // Интервал между тиками в игровых тиках (20 тиков = 1 секунда)
        float tickInterval = 20.0f / finalTickRate;
        
        // Минимальный интервал для стабильности
        float minInterval = 1.0f; // 20 раз в секунду максимум
        tickInterval = Math.max(tickInterval, minInterval);
        
        // Реальная частота после ограничений
        float actualTickRate = 20.0f / tickInterval;
        
        // === ТИПЫ СРАБАТЫВАНИЯ ===
        
        // Постоянные тики
        boolean constantTicking = actualTickRate >= 2.0f;
        
        // Пульсирующие тики
        boolean pulseTicking = actualTickRate < 2.0f && actualTickRate >= 0.5f;
        
        // Редкие тики
        boolean rareTicking = actualTickRate < 0.5f;
        
        // === ЭФФЕКТЫ ЧАСТОТЫ ===
        
        // Высокая частота позволяет более плавное воздействие
        float smoothness = Math.min(1.0f, actualTickRate / 5.0f);
        
        // Частые тики могут накапливать эффекты
        boolean canAccumulate = actualTickRate >= 3.0f;
        float accumulationFactor = canAccumulate ? actualTickRate * 0.1f : 0.0f;
        
        // Очень частые тики могут создавать резонанс
        boolean createsResonance = actualTickRate >= 8.0f;
        float resonanceAmplification = createsResonance ? (actualTickRate - 7.0f) * 0.2f : 0.0f;
        
        // === АДАПТИВНАЯ ЧАСТОТА ===
        
        // Частота может изменяться в зависимости от ситуации
        boolean adaptiveRate = actualTickRate >= 1.0f;
        float adaptationSpeed = adaptiveRate ? actualTickRate * 0.15f : 0.0f;
        
        // Частота может зависеть от количества целей
        boolean targetScaling = actualTickRate >= 2.0f;
        float targetScalingFactor = targetScaling ? 0.1f : 0.0f;
        
        // === ОПТИМИЗАЦИИ ===
        
        // Группировка тиков для производительности
        boolean batchTicks = actualTickRate >= 5.0f;
        int batchSize = batchTicks ? Math.max(2, (int)(actualTickRate / 3.0f)) : 1;
        
        // Пропуск тиков при низкой активности
        boolean canSkipTicks = actualTickRate >= 3.0f;
        float skipThreshold = canSkipTicks ? 0.1f : 0.0f;
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("tick_rate", actualTickRate)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("processing_load", processingLoad)
            .putValue("stability_penalty", stabilityPenalty)
            .putValue("tick_interval", tickInterval)
            .putValue("constant_ticking", constantTicking ? 1.0f : 0.0f)
            .putValue("pulse_ticking", pulseTicking ? 1.0f : 0.0f)
            .putValue("rare_ticking", rareTicking ? 1.0f : 0.0f)
            .putValue("smoothness", smoothness)
            .putValue("can_accumulate", canAccumulate ? 1.0f : 0.0f)
            .putValue("accumulation_factor", accumulationFactor)
            .putValue("creates_resonance", createsResonance ? 1.0f : 0.0f)
            .putValue("resonance_amplification", resonanceAmplification)
            .putValue("adaptive_rate", adaptiveRate ? 1.0f : 0.0f)
            .putValue("adaptation_speed", adaptationSpeed)
            .putValue("target_scaling", targetScaling ? 1.0f : 0.0f)
            .putValue("target_scaling_factor", targetScalingFactor)
            .putValue("batch_ticks", batchTicks ? 1.0f : 0.0f)
            .putValue("batch_size", (float) batchSize)
            .putValue("can_skip_ticks", canSkipTicks ? 1.0f : 0.0f)
            .putValue("skip_threshold", skipThreshold)
            .putValue("effect_tick_rate", actualTickRate) // Основное значение
            .build();
    }
}