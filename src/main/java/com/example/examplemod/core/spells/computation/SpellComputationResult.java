package com.example.examplemod.core.spells.computation;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Thread-safe результат вычисления параметра заклинания
 * Содержит данные для применения в Main Thread или передачи в Collision/Aggregation потоки
 */
public class SpellComputationResult {
    
    private final UUID spellInstanceId;
    private final String parameterKey;
    private final Map<String, Object> computedValues = new ConcurrentHashMap<>();
    private final Map<String, String> collisionModifications = new ConcurrentHashMap<>();
    private final Map<String, Object> formModifications = new ConcurrentHashMap<>();
    
    // Флаги для pipeline
    private volatile boolean needsMainThreadApplication = false;
    private volatile boolean needsCollisionUpdate = false;
    private volatile boolean needsVisualsUpdate = false;
    
    public SpellComputationResult(UUID spellInstanceId, String parameterKey) {
        this.spellInstanceId = spellInstanceId;
        this.parameterKey = parameterKey;
    }
    
    /**
     * Конструктор по умолчанию для простых вычислений
     */
    public SpellComputationResult() {
        this.spellInstanceId = UUID.randomUUID();
        this.parameterKey = "unknown";
    }
    
    // === Методы для записи результатов (Worker Thread) ===
    
    /**
     * Добавить вычисленное значение
     */
    public SpellComputationResult putComputedValue(String key, Object value) {
        computedValues.put(key, value);
        return this;
    }
    
    /**
     * Добавить модификацию коллизии для Collision Thread
     */
    public SpellComputationResult addCollisionModification(String property, String modification) {
        collisionModifications.put(property, modification);
        needsCollisionUpdate = true;
        return this;
    }
    
    /**
     * Добавить модификацию формы для Main Thread
     */
    public SpellComputationResult addFormModification(String property, Object value) {
        formModifications.put(property, value);
        needsMainThreadApplication = true;
        return this;
    }
    
    /**
     * Пометить что нужно обновить визуальные эффекты
     */
    public SpellComputationResult markNeedsVisualsUpdate() {
        needsVisualsUpdate = true;
        return this;
    }
    
    // === Геттеры (thread-safe) ===
    
    public UUID getSpellInstanceId() { return spellInstanceId; }
    public String getParameterKey() { return parameterKey; }
    
    public Map<String, Object> getComputedValues() {
        return new HashMap<>(computedValues);
    }
    
    public Map<String, String> getCollisionModifications() {
        return new HashMap<>(collisionModifications);
    }
    
    public Map<String, Object> getFormModifications() {
        return new HashMap<>(formModifications);
    }
    
    public boolean needsMainThreadApplication() { return needsMainThreadApplication; }
    public boolean needsCollisionUpdate() { return needsCollisionUpdate; }
    public boolean needsVisualsUpdate() { return needsVisualsUpdate; }
    
    /**
     * Получить вычисленное значение как число
     */
    public float getComputedFloat(String key, float defaultValue) {
        Object value = computedValues.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }
    
    /**
     * Получить вычисленное значение как строку
     */
    public String getComputedString(String key, String defaultValue) {
        Object value = computedValues.get(key);
        return value instanceof String str ? str : defaultValue;
    }
    
    /**
     * Проверить наличие вычисленного значения
     */
    public boolean hasComputedValue(String key) {
        return computedValues.containsKey(key);
    }
    
    /**
     * Статический метод для создания Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return String.format("SpellComputationResult{spell=%s, param=%s, values=%d, collisions=%d, forms=%d}", 
                           spellInstanceId, parameterKey, 
                           computedValues.size(), collisionModifications.size(), formModifications.size());
    }
    
    /**
     * Builder для SpellComputationResult
     */
    public static class Builder {
        private SpellComputationResult result = new SpellComputationResult();
        
        public Builder putValue(String key, Object value) {
            result.putComputedValue(key, value);
            return this;
        }
        
        public Builder addCollisionMod(String property, String modification) {
            result.addCollisionModification(property, modification);
            return this;
        }
        
        public Builder addFormMod(String property, Object value) {
            result.addFormModification(property, value);
            return this;
        }
        
        public Builder needsVisuals() {
            result.markNeedsVisualsUpdate();
            return this;
        }
        
        public SpellComputationResult build() {
            return result;
        }
    }
}