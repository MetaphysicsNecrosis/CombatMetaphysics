package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

import java.util.Map;

/**
 * Параметр пробивной способности против защиты
 * Определяет игнорирование брони, магических щитов
 */
public class PenetrationParameter extends AbstractSpellParameter<Float> {
    
    public PenetrationParameter() {
        super("penetration", Float.class);
    }
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float basePenetration = parseFloat(inputValue, 0.0f);
        if (basePenetration < 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalPenetration = basePenetration;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "PROJECTILE" -> {
                finalPenetration *= 1.2f; // Снаряды лучше пробивают
                // Кинетическая энергия
            }
            case "BEAM" -> {
                finalPenetration *= 1.5f; // Лучи отлично пробивают
                // Концентрированная энергия
            }
            case "INSTANT_POINT" -> {
                finalPenetration *= 0.8f; // Взрывы хуже против брони
                // Рассеянная энергия
            }
            case "TOUCH" -> {
                finalPenetration *= 1.3f; // Прямой контакт эффективен
                // Точечное воздействие
            }
            case "CHAIN" -> {
                finalPenetration *= 0.7f; // Цепи теряют силу пробития
                // Энергия рассеивается между целями
            }
            case "WAVE" -> {
                finalPenetration *= 0.5f; // Волны плохо пробивают
                // Широкое воздействие
            }
            case "AREA", "BARRIER" -> {
                finalPenetration *= 0.3f; // Зональные эффекты слабо пробивают
            }
            case "WEAPON_ENCHANT" -> {
                finalPenetration *= 1.4f; // Зависит от оружия
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Проникающие элементы
        if (context.hasElementalIntensity("lightning")) {
            float lightningBonus = context.getElementalIntensity("lightning") * 0.8f;
            finalPenetration += lightningBonus;
        }
        
        if (context.hasElementalIntensity("light")) {
            float lightBonus = context.getElementalIntensity("light") * 0.6f;
            finalPenetration += lightBonus;
        }
        
        // Физические элементы
        if (context.hasElementalIntensity("earth")) {
            float earthBonus = context.getElementalIntensity("earth") * 0.4f;
            finalPenetration += earthBonus;
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Пробитие увеличивает расход маны
        float manaCostMultiplier = 1.0f + finalPenetration * 0.7f;
        
        // Требует больше времени каста для фокусировки
        float castTimeMultiplier = 1.0f + finalPenetration * 0.2f;
        
        // Пробивающие заклинания менее стабильны
        float stabilityPenalty = finalPenetration * 0.15f;
        
        // === ТИПЫ ПРОБИТИЯ ===
        
        // Пробитие физической брони
        float armorPenetration = finalPenetration * 0.8f;
        
        // Пробитие магических щитов
        float magicPenetration = finalPenetration * 1.0f;
        
        // Пробитие элементальных сопротивлений
        float elementalPenetration = finalPenetration * 0.6f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ===
        
        // Высокое пробитие может игнорировать иммунитеты
        boolean canIgnoreImmunity = finalPenetration >= 2.0f;
        float immunityBypass = canIgnoreImmunity ? (finalPenetration - 1.5f) * 0.2f : 0.0f;
        
        // Пробитие может создавать "дыры" в защите
        boolean createsSecurityHoles = finalPenetration >= 1.5f;
        float securityHoleDuration = createsSecurityHoles ? finalPenetration * 2.0f : 0.0f;
        
        // Очень высокое пробитие может разрушать защитные заклинания
        boolean canDispelShields = finalPenetration >= 2.5f;
        float dispelPower = canDispelShields ? finalPenetration * 0.5f : 0.0f;
        
        // Пробитие увеличивает критический шанс против защищенных целей
        float critVsArmoredBonus = finalPenetration * 0.1f;
        
        // === ОГРАНИЧЕНИЯ ===
        
        // Максимальное пробитие ограничено
        float maxPenetration = Math.min(finalPenetration, 5.0f);
        
        // Эффективность против разных типов защиты
        float vsLightArmor = Math.min(1.0f, maxPenetration * 0.4f);
        float vsMediumArmor = Math.min(1.0f, maxPenetration * 0.25f);
        float vsHeavyArmor = Math.min(1.0f, maxPenetration * 0.15f);
        
        // Против магической защиты
        float vsMagicShields = Math.min(1.0f, maxPenetration * 0.3f);
        float vsBarriers = Math.min(1.0f, maxPenetration * 0.2f);
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("penetration", finalPenetration)
            .putValue("penetration", maxPenetration)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("cast_time_multiplier", castTimeMultiplier)
            .putValue("stability_penalty", stabilityPenalty)
            .putValue("armor_penetration", armorPenetration)
            .putValue("magic_penetration", magicPenetration)
            .putValue("elemental_penetration", elementalPenetration)
            .putValue("can_ignore_immunity", canIgnoreImmunity ? 1.0f : 0.0f)
            .putValue("immunity_bypass", immunityBypass)
            .putValue("creates_security_holes", createsSecurityHoles ? 1.0f : 0.0f)
            .putValue("security_hole_duration", securityHoleDuration)
            .putValue("can_dispel_shields", canDispelShields ? 1.0f : 0.0f)
            .putValue("dispel_power", dispelPower)
            .putValue("crit_vs_armored_bonus", critVsArmoredBonus)
            .putValue("vs_light_armor", vsLightArmor)
            .putValue("vs_medium_armor", vsMediumArmor)
            .putValue("vs_heavy_armor", vsHeavyArmor)
            .putValue("vs_magic_shields", vsMagicShields)
            .putValue("vs_barriers", vsBarriers)
            .putValue("penetration_power", maxPenetration) // Основное значение
            .build();
    }
}