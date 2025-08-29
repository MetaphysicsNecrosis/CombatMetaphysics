package com.example.examplemod.core.spells.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.HashMap;

/**
 * Контекст применения эффекта - мост между мат. моделью и физической реализацией
 * Содержит ТОЛЬКО конечные вычисленные значения, БЕЗ логики параметров
 */
public class SpellEffectContext {
    
    // Источники эффекта
    private final Entity spellEntity;
    private final Player caster;
    private final Vec3 effectPosition;
    
    // Вычисленные значения (результат мат. модели)
    private final Map<String, Float> numericValues = new HashMap<>();
    private final Map<String, String> stringValues = new HashMap<>();
    private final Map<String, Boolean> booleanValues = new HashMap<>();
    
    public SpellEffectContext(Entity spellEntity, Player caster, Vec3 effectPosition) {
        this.spellEntity = spellEntity;
        this.caster = caster;
        this.effectPosition = effectPosition;
    }
    
    // === Сеттеры для вычисленных значений (вызываются из мат. модели) ===
    
    public SpellEffectContext setValue(String key, float value) {
        numericValues.put(key, value);
        return this;
    }
    
    public SpellEffectContext setValue(String key, String value) {
        stringValues.put(key, value);
        return this;
    }
    
    public SpellEffectContext setValue(String key, boolean value) {
        booleanValues.put(key, value);
        return this;
    }
    
    // === Геттеры для физической реализации ===
    
    public float getFloat(String key, float defaultValue) {
        return numericValues.getOrDefault(key, defaultValue);
    }
    
    public String getString(String key, String defaultValue) {
        return stringValues.getOrDefault(key, defaultValue);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return booleanValues.getOrDefault(key, defaultValue);
    }
    
    public boolean hasValue(String key) {
        return numericValues.containsKey(key) || 
               stringValues.containsKey(key) || 
               booleanValues.containsKey(key);
    }
    
    // === Источники эффекта ===
    
    public Entity getSpellEntity() { return spellEntity; }
    public Player getCaster() { return caster; }
    public Vec3 getEffectPosition() { return effectPosition; }
    
    // === Фабричные методы ===
    
    /**
     * Создать контекст из результатов мат. модели
     */
    public static SpellEffectContext fromComputationResult(Entity spellEntity, 
                                                          Player caster, 
                                                          Vec3 position,
                                                          Map<String, Object> computedValues) {
        SpellEffectContext context = new SpellEffectContext(spellEntity, caster, position);
        
        // Переносим вычисленные значения
        for (Map.Entry<String, Object> entry : computedValues.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            
            if (value instanceof Number num) {
                context.setValue(key, num.floatValue());
            } else if (value instanceof String str) {
                context.setValue(key, str);
            } else if (value instanceof Boolean bool) {
                context.setValue(key, bool);
            }
        }
        
        return context;
    }
}