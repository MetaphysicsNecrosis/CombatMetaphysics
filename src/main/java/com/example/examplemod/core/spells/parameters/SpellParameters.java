package com.example.examplemod.core.spells.parameters;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Параметры заклинания - все числовые и поведенческие характеристики
 * Thread-safe implementation для использования в многопоточной среде
 */
public class SpellParameters {
    private final Map<String, Object> parameters = new ConcurrentHashMap<>();
    
    // Численные параметры (по умолчанию)
    public static final String MANA_COST = "mana_cost";
    public static final String CHANNEL_COST_PER_SECOND = "channel_cost_per_second";
    public static final String CAST_TIME = "cast_time";
    public static final String COOLDOWN = "cooldown";
    public static final String SPEED = "speed";
    public static final String TICK_RATE = "tick_rate";
    public static final String AMPLIFY_FACTOR = "amplify_factor";
    public static final String INTERRUPT_RESISTANCE = "interrupt_resistance";
    public static final String ACCURACY_REQUIREMENT = "accuracy_requirement";
    
    // Игрок настраиваемые параметры
    public static final String DAMAGE = "damage";
    public static final String HEALING = "healing";
    public static final String DURATION = "duration";
    public static final String RANGE = "range";
    public static final String RADIUS = "radius";
    public static final String SPREAD_ANGLE = "spread_angle";
    public static final String PIERCE_COUNT = "pierce_count";
    public static final String BOUNCE_COUNT = "bounce_count";
    public static final String HOMING_STRENGTH = "homing_strength";
    public static final String WAVE_BEND = "wave_bend";
    public static final String ENCHANTMENT_DURATION = "enchantment_duration";
    public static final String DURABILITY = "durability";
    public static final String PENETRATION = "penetration";
    public static final String GROWTH_RATE = "growth_rate";
    public static final String GEOMETRY_SIZE = "geometry_size";
    public static final String INSTANT_DELAY = "instant_delay";
    
    // Фиксированные параметры
    public static final String GEOMETRY_TYPE = "geometry_type";
    public static final String REFLECT_CHANCE = "reflect_chance";
    public static final String CRIT_CHANCE = "crit_chance";

    public SpellParameters() {}

    public SpellParameters(Map<String, Object> initialParameters) {
        if (initialParameters != null) {
            this.parameters.putAll(initialParameters);
        }
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) return null;
        
        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        
        throw new IllegalArgumentException(
            String.format("Parameter %s is not of type %s, got %s", 
                         key, type.getSimpleName(), value.getClass().getSimpleName()));
    }

    public float getFloat(String key, float defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof String str) {
            return str;
        }
        return defaultValue;
    }

    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    public SpellParameters copy() {
        return new SpellParameters(new HashMap<>(parameters));
    }

    public Map<String, Object> getAllParameters() {
        return new HashMap<>(parameters);
    }
}