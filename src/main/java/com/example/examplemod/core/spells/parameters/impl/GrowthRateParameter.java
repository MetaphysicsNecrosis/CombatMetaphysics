package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр скорости увеличения зоны/барьера/волны
 * Определяет динамический рост для WAVE, BARRIER, AREA
 */
public class GrowthRateParameter extends AbstractSpellParameter<Float> {
    
    public GrowthRateParameter() {
        super("growth_rate", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseGrowthRate = parseFloat(inputValue, 0.0f);
        if (baseGrowthRate < 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalGrowthRate = baseGrowthRate;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "WAVE" -> {
                finalGrowthRate *= 1.0f; // Основное применение волн
                // Радиус увеличивается каждый тик
            }
            case "AREA" -> {
                finalGrowthRate *= 0.6f; // Зоны растут медленнее
                // Постепенное расширение области эффекта
            }
            case "BARRIER" -> {
                finalGrowthRate *= 0.8f; // Барьеры могут расти
                // Увеличение размера или восстановление
            }
            case "PROJECTILE" -> {
                finalGrowthRate *= 0.3f; // Снаряды могут увеличиваться в полете
                // Растущий размер поражения
            }
            case "INSTANT_POINT" -> {
                finalGrowthRate *= 2.0f; // Мгновенные взрывы расширяются быстро
                // Волна от взрыва
            }
            default -> {
                finalGrowthRate = 0.0f; // Остальные формы не растут
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Расширяющие элементы
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 0.5f;
            finalGrowthRate += airBonus;
        }
        
        if (context.hasElementalIntensity("fire")) {
            float fireBonus = context.getElementalIntensity("fire") * 0.4f;
            finalGrowthRate += fireBonus;
        }
        
        // Природа растет естественно
        if (context.hasElementalIntensity("nature")) {
            float natureBonus = context.getElementalIntensity("nature") * 0.6f;
            finalGrowthRate += natureBonus;
        }
        
        // Сдерживающие элементы
        if (context.hasElementalIntensity("ice")) {
            float icePenalty = context.getElementalIntensity("ice") * 0.3f;
            finalGrowthRate = Math.max(0, finalGrowthRate - icePenalty);
        }
        
        if (context.hasElementalIntensity("earth")) {
            float earthPenalty = context.getElementalIntensity("earth") * 0.2f;
            finalGrowthRate = Math.max(0, finalGrowthRate - earthPenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Рост значительно увеличивает расход маны
        float manaCostMultiplier = 1.0f + finalGrowthRate * 1.5f;
        
        // Растущие заклинания требуют постоянного контроля
        float maintenanceCostMultiplier = 1.0f + finalGrowthRate * 1.0f;
        
        // Быстрый рост может быть нестабильным
        float stabilityPenalty = finalGrowthRate * 0.2f;
        
        // === МЕХАНИКИ РОСТА ===
        
        // Максимальный размер, до которого может вырасти
        float maxGrowthSize = calculateMaxSize(formType, finalGrowthRate);
        
        // Скорость роста за тик
        float growthPerTick = finalGrowthRate * 0.05f; // 5% от параметра за тик
        
        // Ускорение роста (рост может ускоряться)
        float growthAcceleration = finalGrowthRate * 0.01f;
        
        // Время до максимального размера
        float timeToMaxSize = maxGrowthSize / Math.max(0.1f, growthPerTick);
        
        // === ТИПЫ РОСТА ===
        
        // Линейный рост
        boolean linearGrowth = finalGrowthRate <= 0.5f;
        
        // Экспоненциальный рост
        boolean exponentialGrowth = finalGrowthRate > 0.5f;
        float exponentialFactor = exponentialGrowth ? finalGrowthRate - 0.5f : 0.0f;
        
        // Пульсирующий рост
        boolean pulsingGrowth = finalGrowthRate > 1.0f;
        float pulseFrequency = pulsingGrowth ? finalGrowthRate * 0.5f : 0.0f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ РОСТА ===
        
        // Быстрый рост может поглощать другие заклинания
        boolean canAbsorbSpells = finalGrowthRate >= 0.8f;
        float absorptionRate = canAbsorbSpells ? finalGrowthRate * 0.3f : 0.0f;
        
        // Очень быстрый рост может создавать давление
        boolean createsPressure = finalGrowthRate >= 1.2f;
        float pressureDamage = createsPressure ? finalGrowthRate * 5.0f : 0.0f;
        
        // Рост может выталкивать сущности
        boolean pushesEntities = finalGrowthRate >= 0.4f;
        float pushForce = pushesEntities ? finalGrowthRate * 2.0f : 0.0f;
        
        // Рост может разрушать блоки
        boolean breaksBlocks = finalGrowthRate >= 1.0f;
        float blockBreakingForce = breaksBlocks ? finalGrowthRate * 1.5f : 0.0f;
        
        // === ОГРАНИЧЕНИЯ РОСТА ===
        
        // Окружающие препятствия могут ограничивать рост
        float obstructionResistance = finalGrowthRate * 0.4f;
        
        // Рост может быть остановлен контрзаклинаниями
        float growthInterruptResistance = Math.min(0.8f, finalGrowthRate * 0.3f);
        
        // Энергия для роста может иссякнуть
        float energyDepletion = finalGrowthRate * 0.1f;
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("growth_rate", finalGrowthRate)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("maintenance_cost_multiplier", maintenanceCostMultiplier)
            .putValue("stability_penalty", stabilityPenalty)
            .putValue("max_growth_size", maxGrowthSize)
            .putValue("growth_per_tick", growthPerTick)
            .putValue("growth_acceleration", growthAcceleration)
            .putValue("time_to_max_size", timeToMaxSize)
            .putValue("linear_growth", linearGrowth ? 1.0f : 0.0f)
            .putValue("exponential_growth", exponentialGrowth ? 1.0f : 0.0f)
            .putValue("exponential_factor", exponentialFactor)
            .putValue("pulsing_growth", pulsingGrowth ? 1.0f : 0.0f)
            .putValue("pulse_frequency", pulseFrequency)
            .putValue("can_absorb_spells", canAbsorbSpells ? 1.0f : 0.0f)
            .putValue("absorption_rate", absorptionRate)
            .putValue("creates_pressure", createsPressure ? 1.0f : 0.0f)
            .putValue("pressure_damage", pressureDamage)
            .putValue("pushes_entities", pushesEntities ? 1.0f : 0.0f)
            .putValue("push_force", pushForce)
            .putValue("breaks_blocks", breaksBlocks ? 1.0f : 0.0f)
            .putValue("block_breaking_force", blockBreakingForce)
            .putValue("obstruction_resistance", obstructionResistance)
            .putValue("growth_interrupt_resistance", growthInterruptResistance)
            .putValue("energy_depletion", energyDepletion)
            .putValue("growth_rate", finalGrowthRate) // Основное значение для SpellEntity
            .build();
    }
    
    /**
     * Расчет максимального размера для разных форм
     */
    private float calculateMaxSize(String formType, float growthRate) {
        float baseMaxSize = switch (formType) {
            case "WAVE" -> 20.0f; // Волны могут быть очень большими
            case "AREA" -> 15.0f; // Зоны ограничены
            case "BARRIER" -> 12.0f; // Барьеры умеренные
            case "PROJECTILE" -> 5.0f; // Снаряды малы
            case "INSTANT_POINT" -> 10.0f; // Взрывы средние
            default -> 0.0f;
        };
        
        return baseMaxSize * (1.0f + growthRate * 2.0f);
    }
}