package com.example.examplemod.core.spells.parameters.impl;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;

import java.util.Map;

/**
 * Параметр прочности/HP объекта
 * Определяет "здоровье" для BARRIER, физических барьеров
 */
public class DurabilityParameter extends AbstractSpellParameter<Float> {
    
    public DurabilityParameter() {
        super("durability", Float.class);
    }
    
    @Override
    public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseDurability = parseFloat(inputValue, 100.0f); // 100 HP по умолчанию
        if (baseDurability <= 0) return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();
        
        // === БАЗОВАЯ ФОРМУЛА ===
        float finalDurability = baseDurability;
        
        // === МОДИФИКАТОРЫ ОТ ФОРМЫ ===
        
        String formType = context.getFormType();
        switch (formType) {
            case "BARRIER" -> {
                finalDurability *= 1.0f; // Базовая прочность барьера
                // Может восстанавливаться или разрушаться
            }
            case "AREA" -> {
                finalDurability *= 0.3f; // Зоны менее прочные
                // Скорее время существования чем HP
            }
            case "PROJECTILE" -> {
                finalDurability *= 0.1f; // "HP" снаряда против контрзаклинаний
                // Защита от dispel и прерываний
            }
            case "BEAM" -> {
                finalDurability *= 0.2f; // Стабильность луча
                // Сопротивление прерываниям
            }
            case "WAVE" -> {
                finalDurability *= 0.5f; // Устойчивость волны
                // Способность проходить через препятствия
            }
            default -> {
                finalDurability *= 0.05f; // Минимальная "прочность"
            }
        }
        
        // === МОДИФИКАТОРЫ ОТ ЭЛЕМЕНТОВ ===
        
        // Защитные элементы увеличивают прочность
        if (context.hasElementalIntensity("earth")) {
            float earthBonus = context.getElementalIntensity("earth") * 0.8f;
            finalDurability *= (1.0f + earthBonus);
        }
        
        if (context.hasElementalIntensity("ice")) {
            float iceBonus = context.getElementalIntensity("ice") * 0.6f;
            finalDurability *= (1.0f + iceBonus);
        }
        
        if (context.hasElementalIntensity("light")) {
            float lightBonus = context.getElementalIntensity("light") * 0.4f;
            finalDurability *= (1.0f + lightBonus);
        }
        
        // Разрушительные элементы снижают прочность но увеличивают урон
        if (context.hasElementalIntensity("fire")) {
            float firePenalty = context.getElementalIntensity("fire") * 0.2f;
            finalDurability *= Math.max(0.5f, 1.0f - firePenalty);
        }
        
        if (context.hasElementalIntensity("lightning")) {
            float lightningPenalty = context.getElementalIntensity("lightning") * 0.3f;
            finalDurability *= Math.max(0.4f, 1.0f - lightningPenalty);
        }
        
        // === ВЛИЯНИЕ НА ДРУГИЕ ПАРАМЕТРЫ ===
        
        // Прочность значительно увеличивает расход маны
        float durabilityMultiplier = finalDurability / baseDurability;
        float manaCostMultiplier = 1.0f + (durabilityMultiplier - 1.0f) * 1.2f;
        
        // Прочные объекты требуют больше времени создания
        float castTimeMultiplier = 1.0f + (durabilityMultiplier - 1.0f) * 0.5f;
        
        // Увеличенная поддерживающая стоимость
        float maintenanceCostMultiplier = durabilityMultiplier * 0.8f;
        
        // === ТИПЫ ПРОЧНОСТИ ===
        
        // Физическая прочность против обычного урона
        float physicalDurability = finalDurability;
        
        // Магическая прочность против заклинаний
        float magicalDurability = finalDurability * 0.8f;
        
        // Элементальная прочность
        float elementalDurability = finalDurability * 0.6f;
        
        // === МЕХАНИКИ ВОССТАНОВЛЕНИЯ ===
        
        // Скорость естественной регенерации
        float naturalRegenRate = Math.max(0, finalDurability * 0.01f); // 1% в секунду
        
        // Может ли восстанавливаться от маны
        boolean canRegenFromMana = finalDurability >= 200.0f;
        float manaRegenRate = canRegenFromMana ? finalDurability * 0.05f : 0.0f;
        
        // Пороговое значение для критического состояния
        float criticalThreshold = finalDurability * 0.25f;
        
        // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ПРОЧНОСТИ ===
        
        // Очень прочные объекты могут отражать атаки
        boolean canReflect = finalDurability >= 500.0f;
        float reflectionChance = canReflect ? Math.min(0.3f, finalDurability / 2000.0f) : 0.0f;
        
        // Прочные барьеры могут поглощать заклинания
        boolean canAbsorb = "BARRIER".equals(formType) && finalDurability >= 300.0f;
        float absorptionCapacity = canAbsorb ? finalDurability * 0.2f : 0.0f;
        
        // При разрушении может создавать осколки
        boolean createDebris = finalDurability >= 150.0f;
        float debrisCount = createDebris ? Math.min(10, finalDurability / 50.0f) : 0.0f;
        
        // Взрыв при разрушении
        boolean explodeOnDestroy = finalDurability >= 400.0f;
        float explosionRadius = explodeOnDestroy ? finalDurability / 200.0f : 0.0f;
        
        // === СОПРОТИВЛЕНИЯ ===
        
        // Сопротивление прерываниям
        float interruptResistance = Math.min(0.8f, finalDurability / 500.0f);
        
        // Сопротивление dispel эффектам
        float dispelResistance = Math.min(0.6f, finalDurability / 800.0f);
        
        // Сопротивление изменениям формы
        float transformResistance = Math.min(0.5f, finalDurability / 1000.0f);
        
        return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()
            .putValue("durability", finalDurability)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("cast_time_multiplier", castTimeMultiplier)
            .putValue("maintenance_cost_multiplier", maintenanceCostMultiplier)
            .putValue("physical_durability", physicalDurability)
            .putValue("magical_durability", magicalDurability)
            .putValue("elemental_durability", elementalDurability)
            .putValue("natural_regen_rate", naturalRegenRate)
            .putValue("can_regen_from_mana", canRegenFromMana ? 1.0f : 0.0f)
            .putValue("mana_regen_rate", manaRegenRate)
            .putValue("critical_threshold", criticalThreshold)
            .putValue("can_reflect", canReflect ? 1.0f : 0.0f)
            .putValue("reflection_chance", reflectionChance)
            .putValue("can_absorb", canAbsorb ? 1.0f : 0.0f)
            .putValue("absorption_capacity", absorptionCapacity)
            .putValue("create_debris", createDebris ? 1.0f : 0.0f)
            .putValue("debris_count", debrisCount)
            .putValue("explode_on_destroy", explodeOnDestroy ? 1.0f : 0.0f)
            .putValue("explosion_radius", explosionRadius)
            .putValue("interrupt_resistance", interruptResistance)
            .putValue("dispel_resistance", dispelResistance)
            .putValue("transform_resistance", transformResistance)
            .putValue("barrier_durability", finalDurability) // Для барьеров
            .putValue("spell_hp", finalDurability) // Основное значение
            .build();
    }
}