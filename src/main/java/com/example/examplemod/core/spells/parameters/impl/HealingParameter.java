package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр базового исцеления
 * Вычисляет итоговую силу лечения на основе различных факторов
 */
public class HealingParameter extends AbstractSpellParameter<Float> {
    
    public HealingParameter() {
        super("healing", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseHealing = parseFloat(inputValue, 0.0f);
        if (baseHealing <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalHealing = baseHealing;
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Лечащие элементы усиливают
        if (context.hasElementalIntensity("light")) {
            float lightBonus = context.getElementalIntensity("light") * 0.3f;
            finalHealing *= (1.0f + lightBonus);
        }
        
        if (context.hasElementalIntensity("water")) {
            float waterBonus = context.getElementalIntensity("water") * 0.25f;
            finalHealing *= (1.0f + waterBonus);
        }
        
        // Разрушающие элементы ослабляют лечение
        if (context.hasElementalIntensity("shadow")) {
            float shadowPenalty = context.getElementalIntensity("shadow") * 0.4f;
            finalHealing *= Math.max(0.1f, 1.0f - shadowPenalty);
        }
        
        if (context.hasElementalIntensity("fire")) {
            float firePenalty = context.getElementalIntensity("fire") * 0.2f;
            finalHealing *= Math.max(0.5f, 1.0f - firePenalty);
        }
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "TOUCH" -> finalHealing *= 1.5f; // Прямой контакт усиливает лечение
            case "AREA" -> finalHealing *= 0.8f;  // Зональное лечение слабее
            case "INSTANT_POINT" -> finalHealing *= 1.2f; // Мгновенное лечение эффективнее
            case "BEAM" -> finalHealing *= 0.9f;  // Лучевое лечение немного слабее
            case "PROJECTILE" -> finalHealing *= 0.7f; // Дальнобойное лечение слабее
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Лечение увеличивает расход маны
        float manaCostMultiplier = 1.0f + (finalHealing / baseHealing - 1.0f) * 0.8f;
        
        // Время каста увеличивается с силой лечения
        float castTimeMultiplier = 1.0f + (finalHealing / 100.0f) * 0.1f;
        
        // Лечение создает святое свечение - увеличивает размер эффекта
        float sizeBonus = finalHealing * 0.02f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ===
        
        // Мощное лечение может удалять негативные эффекты
        boolean canDispelNegative = finalHealing >= 50.0f;
        
        // Очень мощное лечение может временно увеличивать максимальное здоровье
        float temporaryHPBonus = Math.max(0, finalHealing - 100.0f) * 0.5f;
        
        // Лечение имеет шанс критического эффекта
        float critChanceBonus = Math.min(0.2f, finalHealing / 500.0f);
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("healing", finalHealing)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("cast_time_multiplier", castTimeMultiplier)
            .putValue("effect_size_bonus", sizeBonus)
            .putValue("can_dispel_negative", canDispelNegative ? 1.0f : 0.0f)
            .putValue("temporary_hp_bonus", temporaryHPBonus)
            .putValue("healing_crit_chance_bonus", critChanceBonus)
            .putValue("healing_power", finalHealing) // Основное значение
            .build();
    }
}