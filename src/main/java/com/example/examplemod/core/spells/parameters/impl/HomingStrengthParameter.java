package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр силы самонаведения
 * Определяет способность следовать за целью для PROJECTILE, CHAIN
 */
public class HomingStrengthParameter extends AbstractSpellParameter<Float> {
    
    public HomingStrengthParameter() {
        super("homing_strength", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseHomingStrength = parseFloat(inputValue, 0.0f);
        if (baseHomingStrength < 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalHomingStrength = Math.min(baseHomingStrength, 1.0f); // Максимум 100%
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "PROJECTILE" -> {
                // Снаряды могут поворачивать к цели
                // Сила определяет резкость поворотов
            }
            case "CHAIN" -> {
                finalHomingStrength *= 1.5f; // Цепи лучше ищут цели
                finalHomingStrength = Math.min(finalHomingStrength, 1.0f);
            }
            case "BEAM" -> {
                finalHomingStrength *= 0.3f; // Лучи могут слегка изгибаться
                // Изгиб луча к ближайшим целям
            }
            case "WAVE" -> {
                finalHomingStrength *= 0.5f; // Волны могут искажаться к целям
                // Неравномерное расширение к группам целей
            }
            default -> {
                finalHomingStrength = 0.0f; // Остальные формы не наводятся
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Ментальные элементы улучшают наведение
        if (context.hasElementalIntensity("spirit")) {
            float spiritBonus = context.getElementalIntensity("spirit") * 0.4f;
            finalHomingStrength = Math.min(1.0f, finalHomingStrength + spiritBonus);
        }
        
        // Воздушные элементы улучшают маневренность
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 0.3f;
            finalHomingStrength = Math.min(1.0f, finalHomingStrength + airBonus);
        }
        
        // Молния дает точность
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 0.25f;
            finalHomingStrength = Math.min(1.0f, finalHomingStrength + lightningBonus);
        }
        
        // Тяжелые элементы ухудшают маневренность
        if (context.hasElementalIntensity("earth")) {
            float earthPenalty = context.getElementalIntensity("earth") * 0.2f;
            finalHomingStrength = Math.max(0.0f, finalHomingStrength - earthPenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Самонаведение увеличивает расход маны
        float manaCostMultiplier = 1.0f + finalHomingStrength * 0.8f;
        
        // Наведение улучшает точность
        float accuracyBonus = finalHomingStrength * 0.3f;
        
        // Но может снижать скорость из-за поворотов
        float speedPenalty = finalHomingStrength * 0.15f;
        
        // Время каста увеличивается для точной настройки
        float castTimeMultiplier = 1.0f + finalHomingStrength * 0.2f;
        
        // === ПАРАМЕТРЫ НАВЕДЕНИЯ ===
        
        // Радиус обнаружения целей
        float detectionRadius = 5.0f + finalHomingStrength * 15.0f;
        
        // Максимальный угол поворота за тик
        float maxTurnAngle = finalHomingStrength * 10.0f; // Градусы
        
        // Предсказание движения цели
        float predictionStrength = finalHomingStrength * 0.5f;
        
        // Время "памяти" о цели после потери видимости
        float targetMemoryTime = finalHomingStrength * 3.0f; // Секунды
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ===
        
        // Сильное наведение может переключать цели
        boolean canSwitchTargets = finalHomingStrength >= 0.6f;
        
        // Очень сильное наведение может обходить препятствия
        boolean canAvoidObstacles = finalHomingStrength >= 0.8f;
        float obstacleAvoidanceRange = canAvoidObstacles ? finalHomingStrength * 3.0f : 0.0f;
        
        // Наведение может создавать спиральные траектории
        boolean canSpiral = finalHomingStrength >= 0.4f;
        float spiralRadius = canSpiral ? finalHomingStrength * 2.0f : 0.0f;
        
        // Групповое наведение - поиск скоплений целей
        boolean groupTargeting = finalHomingStrength >= 0.7f;
        
        // === ОГРАНИЧЕНИЯ НАВЕДЕНИЯ ===
        
        // Минимальная дистанция для активации наведения
        float minHomingDistance = Math.max(2.0f, 5.0f - finalHomingStrength * 3.0f);
        
        // Максимальная дистанция наведения
        float maxHomingDistance = detectionRadius;
        
        // Эффективность падает на больших дистанциях
        float distanceEfficiencyFalloff = 1.0f - finalHomingStrength * 0.3f;
        
        // Наведение теряет эффективность при высокой скорости
        float speedEfficiencyFalloff = Math.max(0.3f, 1.0f - finalHomingStrength * 0.2f);
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("homing_strength", finalHomingStrength)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("accuracy_bonus", accuracyBonus)
            .putValue("speed_penalty", speedPenalty)
            .putValue("cast_time_multiplier", castTimeMultiplier)
            .putValue("detection_radius", detectionRadius)
            .putValue("max_turn_angle", maxTurnAngle)
            .putValue("prediction_strength", predictionStrength)
            .putValue("target_memory_time", targetMemoryTime)
            .putValue("can_switch_targets", canSwitchTargets ? 1.0f : 0.0f)
            .putValue("can_avoid_obstacles", canAvoidObstacles ? 1.0f : 0.0f)
            .putValue("obstacle_avoidance_range", obstacleAvoidanceRange)
            .putValue("can_spiral", canSpiral ? 1.0f : 0.0f)
            .putValue("spiral_radius", spiralRadius)
            .putValue("group_targeting", groupTargeting ? 1.0f : 0.0f)
            .putValue("min_homing_distance", minHomingDistance)
            .putValue("max_homing_distance", maxHomingDistance)
            .putValue("distance_efficiency_falloff", distanceEfficiencyFalloff)
            .putValue("speed_efficiency_falloff", speedEfficiencyFalloff)
            .putValue("homing_strength", finalHomingStrength) // Основное значение
            .build();
    }
}