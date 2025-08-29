package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр скорости перемещения
 * Определяет скорость для PROJECTILE, WAVE, CHAIN
 */
public class SpeedParameter extends AbstractSpellParameter<Float> {
    
    public SpeedParameter() {
        super("speed", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseSpeed = parseFloat(inputValue, 1.0f); // Базовая скорость
        if (baseSpeed <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalSpeed = baseSpeed;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "PROJECTILE" -> {
                finalSpeed *= 1.0f; // Базовая скорость снаряда
                // Влияет на время полета и точность
            }
            case "WAVE" -> {
                finalSpeed *= 0.4f; // Волны расширяются медленнее
                // Определяет скорость роста радиуса
            }
            case "CHAIN" -> {
                finalSpeed *= 2.0f; // Цепи перемещаются быстро
                // Задержка между переходами
            }
            case "BEAM" -> {
                finalSpeed = 100.0f; // Лучи мгновенные
                // Скорость не влияет на лучи
            }
            case "AREA", "BARRIER" -> {
                finalSpeed = 0.0f; // Статичные формы
                // Скорость не применима
            }
            case "TOUCH", "WEAPON_ENCHANT" -> {
                finalSpeed *= 1.5f; // Скорость активации
                // Влияет на время отклика
            }
            case "INSTANT_POINT" -> {
                finalSpeed = 1000.0f; // Мгновенные эффекты
                // Мгновенное появление в точке
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Быстрые элементы увеличивают скорость
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 0.8f;
            finalSpeed *= (1.0f + lightningBonus);
        }
        
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 0.6f;
            finalSpeed *= (1.0f + airBonus);
        }
        
        if (context.hasElementalIntensity("fire")) {
            float fireBonus = context.getElementalIntensity("fire") * 0.4f;
            finalSpeed *= (1.0f + fireBonus);
        }
        
        // Медленные элементы уменьшают скорость но увеличивают силу
        if (context.hasElementalIntensity("earth")) {
            float earthPenalty = context.getElementalIntensity("earth") * 0.4f;
            finalSpeed *= Math.max(0.3f, 1.0f - earthPenalty);
        }
        
        if (context.hasElementalIntensity("ice")) {
            float icePenalty = context.getElementalIntensity("ice") * 0.3f;
            finalSpeed *= Math.max(0.5f, 1.0f - icePenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Скорость увеличивает расход маны
        float speedMultiplier = finalSpeed / baseSpeed;
        float manaCostMultiplier = 1.0f + (speedMultiplier - 1.0f) * 0.3f;
        
        // Быстрые заклинания сложнее контролировать
        float controlDifficulty = Math.max(0, (speedMultiplier - 1.0f) * 0.4f);
        
        // Скорость влияет на точность попадания
        float accuracyModifier = calculateAccuracyModifier(speedMultiplier);
        
        // Расчет времени достижения цели
        float timeToTarget = calculateTimeToTarget(context, finalSpeed);
        
        // Кинетический урон от скорости
        float kineticDamageBonus = Math.max(0, (speedMultiplier - 1.0f) * 0.2f);
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ СКОРОСТИ ===
        
        // Очень быстрые заклинания могут проходить через препятствия
        boolean canPierceWeak = finalSpeed > 5.0f;
        float piercingPower = Math.max(0, finalSpeed - 3.0f) * 0.3f;
        
        // Быстрые снаряды создают воздушные потоки
        float airDisturbance = Math.max(0, finalSpeed - 2.0f) * 0.1f;
        
        // Медленные заклинания более стабильны
        float stabilityBonus = Math.max(0, (2.0f - finalSpeed) * 0.2f);
        
        // Скорость влияет на самонаведение
        float homingEfficiency = Math.max(0.2f, 2.0f / Math.max(1.0f, finalSpeed));
        
        // === ТРАЕКТОРНЫЕ ЭФФЕКТЫ ===
        
        // Высокая скорость может вызывать отклонения
        float trajectoryStability = Math.max(0.5f, 2.0f / speedMultiplier);
        
        // Скорость влияет на возможность отскоков
        float bounceRetention = Math.max(0.3f, 1.0f / speedMultiplier);
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("speed", finalSpeed)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("control_difficulty", controlDifficulty)
            .putValue("accuracy_modifier", accuracyModifier)
            .putValue("time_to_target", timeToTarget)
            .putValue("kinetic_damage_bonus", kineticDamageBonus)
            .putValue("can_pierce_weak", canPierceWeak ? 1.0f : 0.0f)
            .putValue("piercing_power", piercingPower)
            .putValue("air_disturbance", airDisturbance)
            .putValue("stability_bonus", stabilityBonus)
            .putValue("homing_efficiency", homingEfficiency)
            .putValue("trajectory_stability", trajectoryStability)
            .putValue("bounce_retention", bounceRetention)
            .putValue("movement_speed", finalSpeed) // Основное значение для SpellEntity
            .build();
    }
    
    /**
     * Влияние скорости на точность
     */
    private float calculateAccuracyModifier(float speedMultiplier) {
        if (speedMultiplier < 1.0f) {
            return (1.0f - speedMultiplier) * 0.2f; // Медленные = точнее
        } else {
            return -Math.max(0, (speedMultiplier - 2.0f) * 0.15f); // Быстрые = менее точные
        }
    }
    
    /**
     * Расчет времени достижения цели
     */
    private float calculateTimeToTarget(SpellComputationContext context, float speed) {
        // Попытка получить дальность из контекста
        float estimatedRange = 20.0f; // По умолчанию
        
        if (speed <= 0) return Float.MAX_VALUE;
        return estimatedRange / speed;
    }
}