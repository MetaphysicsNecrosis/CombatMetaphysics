package com.example.examplemod.core.spells.parameters;

import java.util.HashMap;
import java.util.Map;

/**
 * Все параметры заклинаний согласно Concept.txt
 * Разделены на категории: численные (настраиваемые), вычисляемые и фиксированные
 */
public class SpellParameters {
    
    private final Map<String, Object> parameters = new HashMap<>();
    
    // ЧИСЛЕННЫЕ ПАРАМЕТРЫ (настраиваются игроком)
    public static final String DAMAGE = "damage";
    public static final String HEALING = "healing";
    public static final String DURATION = "duration";
    public static final String RANGE = "range";
    public static final String RADIUS = "radius";
    public static final String SIZE = "size";
    
    // Геометрические параметры
    public static final String SPREAD_ANGLE = "spread_angle";
    public static final String PIERCE_COUNT = "pierce_count";
    public static final String BOUNCE_COUNT = "bounce_count";
    public static final String HOMING_STRENGTH = "homing_strength";
    public static final String WAVE_BEND = "wave_bend";
    
    // Временные параметры
    public static final String ENCHANTMENT_DURATION = "enchantment_duration";
    public static final String INSTANT_DELAY = "instant_delay";
    public static final String TICK_RATE = "tick_rate";
    
    // Прочность и защита
    public static final String DURABILITY = "durability";
    public static final String HP = "hp";
    public static final String PENETRATION = "penetration";
    
    // Динамические параметры
    public static final String GROWTH_RATE = "growth_rate";
    public static final String GEOMETRY_SIZE = "geometry_size";
    
    // Особые параметры прохождения
    public static final String GHOST_DURABILITY = "ghost_durability";
    public static final String PHANTOM_DURABILITY = "phantom_durability";
    public static final String PHYSICAL_DURABILITY = "physical_durability";
    
    // ВЫЧИСЛЯЕМЫЕ ПАРАМЕТРЫ
    public static final String MANA_COST = "mana_cost";
    public static final String CHANNEL_COST_PER_SECOND = "channel_cost_per_second";
    public static final String CAST_TIME = "cast_time";
    public static final String COOLDOWN = "cooldown";
    public static final String SPEED = "speed";
    public static final String AMPLIFY_FACTOR = "amplify_factor";
    public static final String INTERRUPT_RESISTANCE = "interrupt_resistance";
    public static final String ACCURACY_REQUIREMENT = "accuracy_requirement";
    
    // ФИКСИРОВАННЫЕ ПАРАМЕТРЫ
    public static final String GEOMETRY_TYPE = "geometry_type";
    public static final String REFLECT_CHANCE = "reflect_chance";
    public static final String CRIT_CHANCE = "crit_chance";
    public static final String REFLECT_CHANCE_MODIFIER = "reflect_chance_modifier";
    public static final String CRIT_CHANCE_MODIFIER = "crit_chance_modifier";
    
    // ЭЛЕМЕНТАЛЬНЫЕ ПАРАМЕТРЫ
    public static final String FIRE = "fire";
    public static final String ICE = "ice";
    public static final String LIGHTNING = "lightning";
    public static final String WATER = "water";
    public static final String EARTH = "earth";
    public static final String SPIRIT = "spirit";
    public static final String NATURE = "nature";
    public static final String ARCANE = "arcane";
    
    // Значения по умолчанию
    public static float getDefaultValue(String parameter) {
        return switch (parameter) {
            case DAMAGE -> 10.0f;
            case HEALING -> 5.0f;
            case DURATION -> 5.0f;
            case RANGE -> 20.0f;
            case RADIUS, SIZE -> 3.0f;
            case SPEED -> 1.0f;
            case DURABILITY, HP -> 100.0f;
            default -> 0.0f;
        };
    }
    
    // Проверка настраиваемости игроком
    public static boolean isPlayerConfigurable(String parameter) {
        return switch (parameter) {
            case DAMAGE, HEALING, DURATION, RANGE, RADIUS, SIZE,
                 SPREAD_ANGLE, PIERCE_COUNT, BOUNCE_COUNT, HOMING_STRENGTH, WAVE_BEND,
                 ENCHANTMENT_DURATION, INSTANT_DELAY, TICK_RATE,
                 DURABILITY, HP, PENETRATION,
                 GROWTH_RATE, GEOMETRY_SIZE -> true;
            default -> false;
        };
    }
    
    // Проверка элементальности
    public static boolean isElemental(String parameter) {
        return switch (parameter) {
            case FIRE, ICE, LIGHTNING, WATER, EARTH, SPIRIT, NATURE, ARCANE -> true;
            default -> false;
        };
    }
    
    // МЕТОДЫ ДОСТУПА К ПАРАМЕТРАМ
    
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
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
    
    public double getDouble(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }
    
    public String getString(String key, String defaultValue) {
        Object value = parameters.get(key);
        return value instanceof String str ? str : defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        return value instanceof Boolean bool ? bool : defaultValue;
    }
    
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    public void removeParameter(String key) {
        parameters.remove(key);
    }
    
    public void clear() {
        parameters.clear();
    }
    
    public Map<String, Object> getAllParameters() {
        return new HashMap<>(parameters);
    }
    
    public SpellParameters copy() {
        SpellParameters copy = new SpellParameters();
        copy.parameters.putAll(this.parameters);
        return copy;
    }
}