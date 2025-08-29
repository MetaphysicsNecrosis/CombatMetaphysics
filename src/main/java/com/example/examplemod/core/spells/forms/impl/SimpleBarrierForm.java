package com.example.examplemod.core.spells.forms.impl;

import com.example.examplemod.core.spells.forms.SpellForm;
import com.example.examplemod.core.spells.forms.SpellFormType;
import com.example.examplemod.core.spells.forms.PersistenceType;
import com.example.examplemod.core.spells.parameters.SpellParameters;

/**
 * Простая реализация барьера для тестирования
 */
public class SimpleBarrierForm implements SpellForm {
    
    @Override
    public SpellFormType getType() {
        return SpellFormType.BARRIER;
    }

    @Override
    public boolean isCompatibleWith(String parameterType) {
        // Барьеры совместимы с защитными параметрами
        return switch (parameterType) {
            case SpellParameters.DURATION,
                 SpellParameters.RADIUS,
                 SpellParameters.DURABILITY,
                 SpellParameters.CHANNEL_COST_PER_SECOND,
                 SpellParameters.INTERRUPT_RESISTANCE -> true;
            case SpellParameters.SPEED,
                 SpellParameters.RANGE,
                 SpellParameters.PIERCE_COUNT,
                 SpellParameters.BOUNCE_COUNT -> false; // Не подходит для барьеров
            default -> false;
        };
    }

    @Override
    public float getBaseManaInitiationCost() {
        return 25f; // Барьеры дороже снарядов
    }

    @Override
    public float getBaseManaAmplificationCost() {
        return 15f; // Может быть значительно усилен
    }

    @Override
    public int getBaseCastTime() {
        return 40; // 2 секунды (40 тиков)
    }

    @Override
    public boolean requiresQTE() {
        return false; // Простые барьеры не требуют QTE
    }

    @Override
    public PersistenceType getDefaultPersistenceType() {
        return PersistenceType.PHYSICAL; // Барьеры блокируют всё
    }

    @Override
    public String toString() {
        return "SimpleBarrierForm{type=BARRIER}";
    }
}