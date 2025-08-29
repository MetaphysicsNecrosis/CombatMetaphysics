package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр количества рикошетов
 * Определяет отскоки для PROJECTILE, CHAIN
 */
public class BounceCountParameter extends AbstractSpellParameter<Float> {
    
    public BounceCountParameter() {
        super("bounce_count", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseBounceCount = parseFloat(inputValue, 0.0f);
        if (baseBounceCount < 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        int finalBounceCount = Math.round(baseBounceCount);
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "PROJECTILE" -> {
                // Снаряды могут отскакивать от поверхностей
                // Каждый отскок меняет направление
            }
            case "CHAIN" -> {
                // Для цепей - дополнительные переходы
                finalBounceCount = Math.min(finalBounceCount, 8);
            }
            case "BEAM" -> {
                finalBounceCount = Math.min(finalBounceCount, 3); // Лучи могут отражаться
                // Отражение от зеркальных поверхностей
            }
            case "WAVE" -> {
                finalBounceCount = Math.min(finalBounceCount, 2); // Волны отражаются от препятствий
            }
            default -> {
                finalBounceCount = 0; // Остальные формы не отскакивают
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Эластичные элементы улучшают отскоки
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 1.5f;
            finalBounceCount += Math.round(airBonus);
        }
        
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 1.0f;
            finalBounceCount += Math.round(lightningBonus);
        }
        
        // Тяжелые элементы ухудшают отскоки
        if (context.hasElementalIntensity("earth")) {
            float earthPenalty = context.getElementalIntensity("earth") * 0.5f;
            finalBounceCount = Math.max(0, finalBounceCount - Math.round(earthPenalty));
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Отскоки увеличивают расход маны
        float manaCostMultiplier = 1.0f + finalBounceCount * 0.3f;
        
        // Сохранение скорости после отскока
        float speedRetention = calculateSpeedRetention(finalBounceCount, formType);
        
        // Сохранение урона после каждого отскока
        float damageRetention = calculateDamageRetention(finalBounceCount, formType);
        
        // Отскоки могут увеличивать общую дальность
        float totalRangeMultiplier = 1.0f + finalBounceCount * 0.4f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ОТСКОКОВ ===
        
        // Множественные отскоки могут создавать хаос
        boolean createsChaos = finalBounceCount >= 4;
        float chaosRadius = createsChaos ? finalBounceCount * 0.5f : 0.0f;
        
        // Отскоки могут искать цели автоматически
        boolean autoTarget = finalBounceCount >= 2;
        float targetingRange = autoTarget ? 5.0f + finalBounceCount : 0.0f;
        
        // Каждый отскок может менять элементальный тип
        boolean canMutateElement = finalBounceCount >= 3;
        
        // Отскоки влияют на критический шанс
        float critChancePerBounce = 0.03f;
        float totalCritBonus = finalBounceCount * critChancePerBounce;
        
        // === ГЕОМЕТРИЧЕСКИЕ ЭФФЕКТЫ ===
        
        // Угол отражения может варьироваться
        float reflectionAngleVariance = Math.min(45.0f, finalBounceCount * 5.0f);
        
        // Отскоки могут создавать дополнительные снаряды
        boolean canSplit = finalBounceCount >= 5;
        int splitCount = canSplit ? Math.max(0, finalBounceCount - 4) : 0;
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("bounce_count", (float) finalBounceCount)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("speed_retention", speedRetention)
            .putValue("damage_retention", damageRetention)
            .putValue("total_range_multiplier", totalRangeMultiplier)
            .putValue("creates_chaos", createsChaos ? 1.0f : 0.0f)
            .putValue("chaos_radius", chaosRadius)
            .putValue("auto_target", autoTarget ? 1.0f : 0.0f)
            .putValue("targeting_range", targetingRange)
            .putValue("can_mutate_element", canMutateElement ? 1.0f : 0.0f)
            .putValue("crit_chance_bonus", totalCritBonus)
            .putValue("reflection_angle_variance", reflectionAngleVariance)
            .putValue("can_split", canSplit ? 1.0f : 0.0f)
            .putValue("split_count", (float) splitCount)
            .putValue("bounce_count", (float) finalBounceCount) // Основное значение
            .putValue("bounce_speed_retention", speedRetention) // Для SpellEntity
            .build();
    }
    
    /**
     * Расчет сохранения скорости после отскока
     */
    private float calculateSpeedRetention(int bounceCount, String formType) {
        if (bounceCount == 0) return 1.0f;
        
        return switch (formType) {
            case "PROJECTILE" -> Math.max(0.4f, 1.0f - bounceCount * 0.1f); // Снаряды теряют скорость
            case "BEAM" -> Math.max(0.8f, 1.0f - bounceCount * 0.05f); // Лучи почти не теряют
            case "CHAIN" -> Math.max(0.6f, 1.0f - bounceCount * 0.08f); // Цепи умеренно
            case "WAVE" -> Math.max(0.7f, 1.0f - bounceCount * 0.1f); // Волны стабильны
            default -> 1.0f;
        };
    }
    
    /**
     * Расчет сохранения урона после отскока
     */
    private float calculateDamageRetention(int bounceCount, String formType) {
        if (bounceCount == 0) return 1.0f;
        
        return switch (formType) {
            case "PROJECTILE" -> Math.max(0.3f, 1.0f - bounceCount * 0.12f);
            case "BEAM" -> Math.max(0.6f, 1.0f - bounceCount * 0.08f);
            case "CHAIN" -> Math.max(0.5f, 1.0f - bounceCount * 0.1f);
            case "WAVE" -> Math.max(0.8f, 1.0f - bounceCount * 0.05f);
            default -> 1.0f;
        };
    }
}