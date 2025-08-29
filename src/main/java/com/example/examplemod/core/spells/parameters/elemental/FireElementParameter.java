package com.example.examplemod.core.spells.parameters.elemental;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

import java.util.Map;

/**
 * Параметр интенсивности огня
 * Добавляет огненные эффекты: поджог, расплавление, взрывы
 */
public class FireElementParameter extends AbstractSpellParameter<Float> {
    
    public FireElementParameter() {
        super("fire_intensity", Float.class);
    }
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        float baseIntensity = parseFloat(inputValue, 0.0f);
        if (baseIntensity <= 0) return emptyResult();
        
        // === ПРИМЕНЕНИЕ ВЗАИМОДЕЙСТВИЙ С ДРУГИМИ ЭЛЕМЕНТАМИ ===
        float finalIntensity = ElementalInteractionMatrix.calculateResultingIntensity(
            ElementalInteractionMatrix.FIRE, context.getElementalMix()
        );
        
        // Базовая интенсивность как стартовая точка
        finalIntensity = Math.max(finalIntensity, baseIntensity);
        
        // === ВЛИЯНИЕ НА ОСНОВНЫЕ ПАРАМЕТРЫ ===
        
        // Огонь увеличивает урон
        float damageMultiplier = 1.0f + finalIntensity * 0.3f;
        
        // Огонь увеличивает скорость (горячие газы)
        float speedMultiplier = 1.0f + finalIntensity * 0.2f;
        
        // Огонь увеличивает радиус эффекта
        float radiusMultiplier = 1.0f + finalIntensity * 0.25f;
        
        // Огонь снижает прочность (нестабильность)
        float durabilityMultiplier = Math.max(0.5f, 1.0f - finalIntensity * 0.15f);
        
        // === ОГНЕННЫЕ ЭФФЕКТЫ ===
        
        // Поджог целей
        float fireDuration = finalIntensity * 3.0f; // Секунды горения
        float fireTickDamage = finalIntensity * 0.5f; // Урон за тик
        
        // Расплавление блоков
        boolean canMeltBlocks = finalIntensity >= 0.5f;
        float meltRadius = canMeltBlocks ? finalIntensity * 2.0f : 0.0f;
        float maxBlockHardness = finalIntensity * 4.0f;
        
        // Взрывные эффекты при высокой интенсивности
        boolean canExplode = finalIntensity >= 0.8f;
        float explosionRadius = canExplode ? finalIntensity * 1.5f : 0.0f;
        float explosionDamage = canExplode ? finalIntensity * 20.0f : 0.0f;
        
        // Создание огненных следов
        boolean leavesFire = finalIntensity >= 0.3f;
        float fireTrailDuration = leavesFire ? finalIntensity * 5.0f : 0.0f;
        
        // === СИНЕРГИИ С ДРУГИМИ ЭЛЕМЕНТАМИ ===
        
        Map<String, Float> elementalMix = context.getElementalMix();
        
        // Огонь + Воздух = Огненная буря
        if (elementalMix.containsKey(ElementalInteractionMatrix.AIR)) {
            float airIntensity = elementalMix.get(ElementalInteractionMatrix.AIR);
            float stormPower = finalIntensity * airIntensity * 0.5f;
            
            return buildResult()
                .putValue("fire_intensity", finalIntensity)
                .putValue("damage_multiplier", damageMultiplier * 1.3f)
                .putValue("speed_multiplier", speedMultiplier * 1.5f)
                .putValue("radius_multiplier", radiusMultiplier * 1.4f)
                .putValue("fire_storm_power", stormPower)
                .putValue("creates_fire_tornado", stormPower >= 1.0f ? 1.0f : 0.0f)
                .build();
        }
        
        // Огонь + Земля = Лава
        if (elementalMix.containsKey(ElementalInteractionMatrix.EARTH)) {
            float earthIntensity = elementalMix.get(ElementalInteractionMatrix.EARTH);
            float lavaPower = finalIntensity * earthIntensity * 0.6f;
            
            return buildResult()
                .putValue("fire_intensity", finalIntensity)
                .putValue("damage_multiplier", damageMultiplier)
                .putValue("durability_multiplier", durabilityMultiplier * 1.5f) // Лава прочнее огня
                .putValue("lava_power", lavaPower)
                .putValue("creates_lava_pools", lavaPower >= 0.8f ? 1.0f : 0.0f)
                .putValue("lava_flow_range", lavaPower * 3.0f)
                .build();
        }
        
        // Огонь + Молния = Плазма
        if (elementalMix.containsKey(ElementalInteractionMatrix.LIGHTNING)) {
            float lightningIntensity = elementalMix.get(ElementalInteractionMatrix.LIGHTNING);
            float plasmaPower = finalIntensity * lightningIntensity * 0.8f;
            
            return buildResult()
                .putValue("fire_intensity", finalIntensity)
                .putValue("damage_multiplier", damageMultiplier * 1.6f)
                .putValue("piercing_power", plasmaPower * 2.0f)
                .putValue("plasma_power", plasmaPower)
                .putValue("creates_plasma", plasmaPower >= 0.6f ? 1.0f : 0.0f)
                .putValue("plasma_temperature", plasmaPower * 1000.0f)
                .build();
        }
        
        // === АНТАГОНИСТИЧЕСКИЕ ВЗАИМОДЕЙСТВИЯ ===
        
        // Огонь vs Вода - подавление
        if (elementalMix.containsKey(ElementalInteractionMatrix.WATER)) {
            float waterIntensity = elementalMix.get(ElementalInteractionMatrix.WATER);
            float suppressionFactor = ElementalInteractionMatrix.getInteractionCoefficient(
                ElementalInteractionMatrix.WATER, ElementalInteractionMatrix.FIRE
            );
            
            float suppression = waterIntensity * (suppressionFactor - 1.0f) * 0.2f;
            finalIntensity = Math.max(0.1f, finalIntensity - suppression);
            
            // Создает пар при взаимодействии
            float steamPower = Math.min(waterIntensity, finalIntensity) * 0.5f;
            
            return buildResult()
                .putValue("fire_intensity", finalIntensity)
                .putValue("damage_multiplier", damageMultiplier * 0.7f) // Ослаблено водой
                .putValue("steam_power", steamPower)
                .putValue("creates_steam", steamPower >= 0.3f ? 1.0f : 0.0f)
                .putValue("visibility_reduction", steamPower * 0.4f)
                .build();
        }
        
        // Огонь vs Лед - расплавление
        if (elementalMix.containsKey(ElementalInteractionMatrix.ICE)) {
            float iceIntensity = elementalMix.get(ElementalInteractionMatrix.ICE);
            float meltingPower = finalIntensity * ElementalInteractionMatrix.getInteractionCoefficient(
                ElementalInteractionMatrix.FIRE, ElementalInteractionMatrix.ICE
            );
            
            return buildResult()
                .putValue("fire_intensity", finalIntensity)
                .putValue("damage_multiplier", damageMultiplier * 1.2f) // Бонус против льда
                .putValue("melting_power", meltingPower)
                .putValue("ice_melting_range", meltingPower * 2.0f)
                .putValue("creates_water_pools", meltingPower >= 1.0f ? 1.0f : 0.0f)
                .build();
        }
        
        // === БАЗОВЫЕ ОГНЕННЫЕ ЭФФЕКТЫ ===
        
        return buildResult()
                .putValue("fire_intensity", finalIntensity)
            .putValue("damage_multiplier", damageMultiplier)
            .putValue("speed_multiplier", speedMultiplier)
            .putValue("radius_multiplier", radiusMultiplier)
            .putValue("durability_multiplier", durabilityMultiplier)
            .putValue("fire_duration", fireDuration)
            .putValue("fire_tick_damage", fireTickDamage)
            .putValue("can_melt_blocks", canMeltBlocks ? 1.0f : 0.0f)
            .putValue("melt_radius", meltRadius)
            .putValue("max_block_hardness", maxBlockHardness)
            .putValue("can_explode", canExplode ? 1.0f : 0.0f)
            .putValue("explosion_radius", explosionRadius)
            .putValue("explosion_damage", explosionDamage)
            .putValue("leaves_fire", leavesFire ? 1.0f : 0.0f)
            .putValue("fire_trail_duration", fireTrailDuration)
            .putValue("fire_intensity", finalIntensity) // Основное значение
            .build();
    }
    
    // Метод для добавления элементальных маркеров к результату
    private SpellComputationResult.Builder addElementalMarkers(SpellComputationResult.Builder builder, float intensity) {
        return builder
            .putValue("elemental_type", ElementalInteractionMatrix.FIRE)
            .putValue("elemental_intensity", intensity)
            .putValue("is_elemental", 1.0f);
    }
}