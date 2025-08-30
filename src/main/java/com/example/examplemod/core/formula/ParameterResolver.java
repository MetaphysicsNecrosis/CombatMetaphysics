package com.example.examplemod.core.formula;

import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class ParameterResolver {
    
    public Map<String, Double> resolveParameters(SpellParameters spellParams, Player caster) {
        Map<String, Double> resolved = new HashMap<>();
        
        resolved.putAll(getSpellParameters(spellParams));
        resolved.putAll(getPlayerAttributes(caster));
        resolved.putAll(getWorldModifiers(caster));
        
        return resolved;
    }
    
    private Map<String, Double> getSpellParameters(SpellParameters params) {
        Map<String, Double> spellParams = new HashMap<>();
        
        spellParams.put("base_damage", (double) params.getFloat("damage", 0.0f));
        spellParams.put("base_healing", (double) params.getFloat("healing", 0.0f));
        spellParams.put("base_range", (double) params.getFloat("range", 0.0f));
        spellParams.put("base_radius", (double) params.getFloat("radius", 0.0f));
        spellParams.put("base_speed", (double) params.getFloat("speed", 0.0f));
        spellParams.put("base_duration", (double) params.getFloat("duration", 0.0f));
        spellParams.put("base_durability", (double) params.getFloat("durability", 0.0f));
        spellParams.put("bounce_count", (double) params.getFloat("bounce_count", 0.0f));
        spellParams.put("pierce_count", (double) params.getFloat("pierce_count", 0.0f));
        spellParams.put("penetration", (double) params.getFloat("penetration", 0.0f));
        spellParams.put("homing_strength", (double) params.getFloat("homing_strength", 0.0f));
        
        spellParams.put("fire_element", (double) params.getFloat("fire", 0.0f));
        spellParams.put("ice_element", (double) params.getFloat("ice", 0.0f));
        spellParams.put("lightning_element", (double) params.getFloat("lightning", 0.0f));
        spellParams.put("earth_element", (double) params.getFloat("earth", 0.0f));
        spellParams.put("water_element", (double) params.getFloat("water", 0.0f));
        
        return spellParams;
    }
    
    private Map<String, Double> getPlayerAttributes(Player player) {
        Map<String, Double> attributes = new HashMap<>();
        
        attributes.put("player_level", (double) player.experienceLevel);
        attributes.put("player_health", (double) player.getHealth());
        attributes.put("player_max_health", (double) player.getMaxHealth());
        attributes.put("player_mana", getManaAttribute(player));
        attributes.put("player_max_mana", getMaxManaAttribute(player));
        
        attributes.put("spell_power", getSpellPowerAttribute(player));
        attributes.put("magic_resistance", getMagicResistanceAttribute(player));
        attributes.put("cast_speed", getCastSpeedAttribute(player));
        attributes.put("mana_efficiency", getManaEfficiencyAttribute(player));
        
        return attributes;
    }
    
    private Map<String, Double> getWorldModifiers(Player player) {
        Map<String, Double> modifiers = new HashMap<>();
        
        modifiers.put("time_of_day", getTimeModifier(player));
        modifiers.put("weather", getWeatherModifier(player));
        modifiers.put("biome", getBiomeModifier(player));
        modifiers.put("dimension", getDimensionModifier(player));
        
        modifiers.put("global_magic_level", 1.0);
        modifiers.put("antimagic_field", 0.0);
        modifiers.put("mana_density", 1.0);
        
        return modifiers;
    }
    
    private double getManaAttribute(Player player) {
        return 100.0;
    }
    
    private double getMaxManaAttribute(Player player) {
        return 100.0 + player.experienceLevel * 10.0;
    }
    
    private double getSpellPowerAttribute(Player player) {
        return 1.0 + player.experienceLevel * 0.05;
    }
    
    private double getMagicResistanceAttribute(Player player) {
        return 0.0;
    }
    
    private double getCastSpeedAttribute(Player player) {
        return 1.0;
    }
    
    private double getManaEfficiencyAttribute(Player player) {
        return 1.0;
    }
    
    private double getTimeModifier(Player player) {
        long dayTime = player.level().getDayTime() % 24000;
        if (dayTime >= 13000 && dayTime <= 23000) {
            return 1.2;
        }
        return 1.0;
    }
    
    private double getWeatherModifier(Player player) {
        if (player.level().isRaining()) {
            return 0.9;
        }
        if (player.level().isThundering()) {
            return 1.3;
        }
        return 1.0;
    }
    
    private double getBiomeModifier(Player player) {
        return 1.0;
    }
    
    private double getDimensionModifier(Player player) {
        return switch (player.level().dimension().location().toString()) {
            case "minecraft:the_nether" -> 1.5;
            case "minecraft:the_end" -> 0.8;
            default -> 1.0;
        };
    }
}