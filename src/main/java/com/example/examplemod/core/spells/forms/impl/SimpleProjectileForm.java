package com.example.examplemod.core.spells.forms.impl;

import com.example.examplemod.core.spells.forms.SpellForm;
import com.example.examplemod.core.spells.forms.SpellFormType;
import com.example.examplemod.core.spells.forms.PersistenceType;
import com.example.examplemod.core.spells.parameters.SpellParameters;

/**
 * Простая реализация формы снаряда для тестирования
 */
public class SimpleProjectileForm implements SpellForm {
    
    @Override
    public SpellFormType getType() {
        return SpellFormType.PROJECTILE;
    }

    @Override
    public boolean isCompatibleWith(String parameterType) {
        // Снаряды совместимы с большинством параметров
        return switch (parameterType) {
            case SpellParameters.DAMAGE,
                 SpellParameters.RANGE,
                 SpellParameters.SPEED,
                 SpellParameters.PIERCE_COUNT,
                 SpellParameters.BOUNCE_COUNT,
                 SpellParameters.HOMING_STRENGTH,
                 SpellParameters.GEOMETRY_SIZE -> true;
            case SpellParameters.HEALING,
                 SpellParameters.CHANNEL_COST_PER_SECOND,
                 SpellParameters.WAVE_BEND,
                 SpellParameters.SPREAD_ANGLE -> false; // Не подходит для снарядов
            default -> false;
        };
    }

    @Override
    public float getBaseManaInitiationCost() {
        return 10f; // Базовая стоимость снаряда
    }

    @Override
    public float getBaseManaAmplificationCost() {
        return 5f; // Может быть усилен через QTE
    }

    @Override
    public int getBaseCastTime() {
        return 20; // 1 секунда (20 тиков)
    }

    @Override
    public boolean requiresQTE() {
        return false; // Простые снаряды не требуют QTE
    }

    @Override
    public PersistenceType getDefaultPersistenceType() {
        return PersistenceType.PHYSICAL; // Снаряды по умолчанию физические
    }

    @Override
    public String toString() {
        return "SimpleProjectileForm{type=PROJECTILE}";
    }
}