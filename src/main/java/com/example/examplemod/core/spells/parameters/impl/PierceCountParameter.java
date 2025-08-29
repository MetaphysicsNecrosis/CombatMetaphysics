package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр количества пробиваемых целей/объектов
 * Определяет сколько целей может пройти PROJECTILE, BEAM
 */
public class PierceCountParameter extends AbstractSpellParameter<Float> {
    
    public PierceCountParameter() {
        super("pierce_count", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float basePierceCount = parseFloat(inputValue, 0.0f); // По умолчанию не пробивает
        if (basePierceCount < 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        int finalPierceCount = Math.round(basePierceCount);
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "PROJECTILE" -> {
                // Снаряды могут пробивать цели
                // Каждое пробитие снижает урон
            }
            case "BEAM" -> {
                finalPierceCount *= 2; // Лучи пробивают лучше
                // Непрерывное пробитие по линии
            }
            case "CHAIN" -> {
                // Для цепей - максимальное количество переходов
                finalPierceCount = Math.min(finalPierceCount, 10);
            }
            case "WAVE" -> {
                finalPierceCount = Math.max(finalPierceCount, 100); // Волны проходят через всё
                // Не теряют силу от пробития
            }
            case "AREA", "BARRIER", "INSTANT_POINT" -> {
                // Статичные формы не пробивают
                finalPierceCount = 0;
            }
            case "TOUCH", "WEAPON_ENCHANT" -> {
                // Контактные формы пробивают ограниченно
                finalPierceCount = Math.min(finalPierceCount, 3);
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Проникающие элементы увеличивают пробитие
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 2.0f;
            finalPierceCount += Math.round(lightningBonus);
        }
        
        if (context.hasElementalIntensity("air")) {
            float airBonus = context.getElementalIntensity("air") * 1.5f;
            finalPierceCount += Math.round(airBonus);
        }
        
        // Физические элементы дают бонус против брони
        if (context.hasElementalIntensity("earth")) {
            float earthBonus = context.getElementalIntensity("earth") * 1.0f;
            finalPierceCount += Math.round(earthBonus);
        }
        
        // Энергетические элементы проходят через магическую защиту
        if (context.hasElementalIntensity("fire")) {
            float fireBonus = context.getElementalIntensity("fire") * 1.2f;
            finalPierceCount += Math.round(fireBonus);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Пробитие значительно увеличивает расход маны
        float pierceMultiplier = 1.0f + finalPierceCount * 0.4f;
        float manaCostMultiplier = pierceMultiplier;
        
        // Пробивающие заклинания сложнее контролировать
        float controlDifficulty = finalPierceCount * 0.1f;
        
        // Урон уменьшается с каждым пробитием
        float damageRetention = calculateDamageRetention(finalPierceCount, formType);
        
        // Пробитие может увеличивать дальность
        float rangeBonus = finalPierceCount * 0.1f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ПРОБИТИЯ ===
        
        // Множественные попадания могут создавать комбо
        boolean canCombo = finalPierceCount >= 3;
        float comboMultiplier = canCombo ? 1.0f + (finalPierceCount - 2) * 0.1f : 1.0f;
        
        // Пробивающие заклинания могут игнорировать определенную защиту
        float armorIgnore = Math.min(0.5f, finalPierceCount * 0.05f);
        
        // Очень пробивающие заклинания могут разрушать блоки
        boolean canBreakBlocks = finalPierceCount >= 5;
        float blockBreakingPower = Math.max(0, finalPierceCount - 4) * 0.5f;
        
        // Пробитие влияет на критический удар
        float critChanceBonus = Math.min(0.2f, finalPierceCount * 0.02f);
        
        // === ОГРАНИЧЕНИЯ И БАЛАНС ===
        
        // Максимальное пробитие ограничено
        int maxPierceCount = Math.min(finalPierceCount, getMaxPierceForForm(formType));
        
        // Эффективность падает с количеством пробитий
        float efficiency = Math.max(0.1f, 1.0f - (maxPierceCount * 0.05f));
        
        // Стабильность заклинания падает
        float stabilityPenalty = maxPierceCount * 0.03f;
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("pierce_count", (float) maxPierceCount)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("control_difficulty", controlDifficulty)
            .putValue("damage_retention", damageRetention)
            .putValue("range_bonus", rangeBonus)
            .putValue("can_combo", canCombo ? 1.0f : 0.0f)
            .putValue("combo_multiplier", comboMultiplier)
            .putValue("armor_ignore", armorIgnore)
            .putValue("can_break_blocks", canBreakBlocks ? 1.0f : 0.0f)
            .putValue("block_breaking_power", blockBreakingPower)
            .putValue("crit_chance_bonus", critChanceBonus)
            .putValue("efficiency", efficiency)
            .putValue("stability_penalty", stabilityPenalty)
            .putValue("pierce_count", (float) maxPierceCount) // Основное значение
            .build();
    }
    
    /**
     * Расчет сохранения урона после каждого пробития
     */
    private float calculateDamageRetention(int pierceCount, String formType) {
        if (pierceCount == 0) return 1.0f;
        
        return switch (formType) {
            case "BEAM" -> Math.max(0.3f, 1.0f - pierceCount * 0.1f); // Лучи теряют мало урона
            case "PROJECTILE" -> Math.max(0.2f, 1.0f - pierceCount * 0.15f); // Снаряды теряют больше
            case "CHAIN" -> Math.max(0.5f, 1.0f - pierceCount * 0.08f); // Цепи стабильны
            case "WAVE" -> 1.0f; // Волны не теряют силу
            default -> Math.max(0.1f, 1.0f - pierceCount * 0.2f);
        };
    }
    
    /**
     * Максимальное пробитие для формы
     */
    private int getMaxPierceForForm(String formType) {
        return switch (formType) {
            case "BEAM" -> 20; // Лучи могут пробить много целей
            case "PROJECTILE" -> 10; // Снаряды ограничены
            case "CHAIN" -> 15; // Цепи могут быть длинными
            case "WAVE" -> 100; // Волны проходят через всё
            case "TOUCH", "WEAPON_ENCHANT" -> 5; // Контакт ограничен
            default -> 0; // Остальные не пробивают
        };
    }
}