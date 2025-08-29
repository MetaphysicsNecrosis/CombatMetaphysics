package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр радиуса/размера зоны воздействия
 * Определяет размер для AREA, BARRIER, WAVE, взрывов PROJECTILE
 */
public class RadiusParameter extends AbstractSpellParameter<Float> {
    
    public RadiusParameter() {
        super("radius", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseRadius = parseFloat(inputValue, 3.0f); // 3 блока по умолчанию
        if (baseRadius <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalRadius = baseRadius;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "AREA" -> {
                finalRadius *= 1.0f; // Базовый радиус зоны
                // Круглая область постоянного эффекта
            }
            case "BARRIER" -> {
                finalRadius *= 1.2f; // Барьеры немного больше
                // Может быть сферой, куполом или стеной
            }
            case "WAVE" -> {
                finalRadius *= 0.5f; // Начальный радиус волны
                // Растет со временем до максимального размера
            }
            case "PROJECTILE" -> {
                finalRadius *= 0.8f; // Радиус взрыва при попадании
                // Используется для AOE снарядов
            }
            case "INSTANT_POINT" -> {
                finalRadius *= 1.1f; // Радиус мгновенного эффекта
                // Взрывы, исцеляющие всплески
            }
            case "BEAM" -> {
                finalRadius *= 0.3f; // Толщина луча
                // Цилиндрическая область поражения
            }
            case "CHAIN" -> {
                finalRadius *= 0.4f; // Радиус поиска следующей цели
                // Определяет область для цепной реакции
            }
            case "TOUCH" -> {
                finalRadius = Math.min(finalRadius, 1.5f); // Ограничен касанием
            }
            case "WEAPON_ENCHANT" -> {
                finalRadius *= 0.6f; // Дополнительный радиус атак оружия
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Расширяющие элементы увеличивают радиус
        if (context.hasElementalIntensity("fire")) {
            float fireBonus = context.getElementalIntensity("fire") * 0.4f;
            finalRadius *= (1.0f + fireBonus);
        }
        
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 0.3f;
            finalRadius *= (1.0f + airBonus);
        }
        
        // Концентрирующие элементы уменьшают радиус но увеличивают интенсивность
        if (context.hasElementalIntensity("lightning")) {
            float lightningPenalty = context.getElementalIntensity("lightning") * 0.25f;
            finalRadius *= Math.max(0.6f, 1.0f - lightningPenalty);
        }
        
        if (context.hasElementalIntensity("ice")) {
            float icePenalty = context.getElementalIntensity("ice") * 0.15f;
            finalRadius *= Math.max(0.8f, 1.0f - icePenalty);
        }
        
        // Стабилизирующие элементы дают бонус к форме
        float shapeStability = 1.0f;
        if (context.hasElementalIntensity("earth")) {
            shapeStability += context.getElementalIntensity("earth") * 0.2f;
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Радиус экспоненциально увеличивает расход маны (площадь/объем)
        float areaMultiplier = (finalRadius / baseRadius);
        float manaCostMultiplier = 1.0f + (areaMultiplier * areaMultiplier - 1.0f) * 0.8f;
        
        // Большие области требуют больше времени каста
        float castTimeMultiplier = 1.0f + (areaMultiplier - 1.0f) * 0.4f;
        
        // Расчет количества целей в области
        float expectedTargets = calculateTargetCapacity(finalRadius);
        
        // Урон/лечение может распределяться по целям
        float perTargetEfficiency = Math.max(0.3f, 1.0f / Math.max(1.0f, expectedTargets * 0.3f));
        
        // Большие области сложнее поддерживать
        float maintenanceCostMultiplier = 1.0f + (areaMultiplier - 1.0f) * 0.6f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ===
        
        // Очень большие области получают градиентный эффект
        boolean hasGradient = finalRadius > 8.0f;
        float gradientFalloff = hasGradient ? 0.3f : 0.0f;
        
        // Размер влияет на видимость и прерываемость
        float visibilityRadius = finalRadius * 1.5f;
        float interruptResistance = Math.min(0.3f, finalRadius * 0.05f);
        
        // Волны получают скорость расширения
        float expansionRate = 0.0f;
        if ("WAVE".equals(formType)) {
            expansionRate = Math.max(0.5f, baseRadius / 10.0f); // Блоков в тик
        }
        
        // Барьеры получают дополнительную прочность от размера
        float sizeHPBonus = 0.0f;
        if ("BARRIER".equals(formType)) {
            sizeHPBonus = finalRadius * finalRadius * 5.0f; // Пропорционально площади
        }
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("radius", finalRadius)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("cast_time_multiplier", castTimeMultiplier)
            .putValue("maintenance_cost_multiplier", maintenanceCostMultiplier)
            .putValue("expected_targets", expectedTargets)
            .putValue("per_target_efficiency", perTargetEfficiency)
            .putValue("shape_stability", shapeStability)
            .putValue("has_gradient_effect", hasGradient ? 1.0f : 0.0f)
            .putValue("gradient_falloff", gradientFalloff)
            .putValue("visibility_radius", visibilityRadius)
            .putValue("interrupt_resistance", interruptResistance)
            .putValue("expansion_rate", expansionRate)
            .putValue("size_hp_bonus", sizeHPBonus)
            .putValue("spell_size", finalRadius) // Основное значение для SpellEntity
            .putValue("effect_radius", finalRadius) // Для системы эффектов
            .build();
    }
    
    /**
     * Примерное количество целей в области радиуса
     */
    private float calculateTargetCapacity(float radius) {
        // Приблизительная плотность целей: 1 на 4 квадратных блока
        float area = (float) (Math.PI * radius * radius);
        return Math.max(1.0f, area / 4.0f);
    }
}