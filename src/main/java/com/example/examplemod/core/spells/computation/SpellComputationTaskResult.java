package com.example.examplemod.core.spells.computation;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Результат выполнения SpellComputationTask
 * Содержит агрегированные результаты всех параметров заклинания
 * 
 * Используется для передачи данных в следующие этапы pipeline:
 * Spell Computation Pool -> [РЕЗУЛЬТАТ] -> Collision Thread -> Aggregation Thread -> Main Thread
 */
public class SpellComputationTaskResult {
    
    private final UUID spellInstanceId;
    private final long computationDurationNanos;
    private final long timestamp;
    
    // Результаты от параметров
    private final List<SpellComputationResult> parameterResults = new ArrayList<>();
    private final Map<String, Object> aggregatedValues = new ConcurrentHashMap<>();
    
    // Данные для последующих этапов pipeline
    private final Map<String, String> collisionModifications = new ConcurrentHashMap<>();
    private final Map<String, Object> formModifications = new ConcurrentHashMap<>();
    private final List<String> visualEffects = new ArrayList<>();
    
    // Статистика и флаги
    private volatile boolean hasErrors = false;
    private volatile boolean needsCollisionUpdate = false;
    private volatile boolean needsMainThreadApplication = false;
    private volatile boolean needsVisualsUpdate = false;
    
    private int successfulParameters = 0;
    private int errorParameters = 0;
    
    public SpellComputationTaskResult(UUID spellInstanceId, long computationDurationNanos) {
        this.spellInstanceId = spellInstanceId;
        this.computationDurationNanos = computationDurationNanos;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Добавить результат от параметра
     */
    public void addParameterResult(SpellComputationResult result) {
        parameterResults.add(result);
        
        // Агрегируем данные для следующих этапов
        collisionModifications.putAll(result.getCollisionModifications());
        formModifications.putAll(result.getFormModifications());
        
        // Агрегируем вычисленные значения
        aggregatedValues.putAll(result.getComputedValues());
        
        // Обновляем флаги
        if (result.needsCollisionUpdate()) {
            needsCollisionUpdate = true;
        }
        
        if (result.needsMainThreadApplication()) {
            needsMainThreadApplication = true;
        }
        
        if (result.needsVisualsUpdate()) {
            needsVisualsUpdate = true;
        }
        
        // Проверяем на ошибки
        if (result.hasComputedValue("error")) {
            hasErrors = true;
            errorParameters++;
        } else {
            successfulParameters++;
        }
    }
    
    /**
     * Вычислить агрегированную статистику
     */
    public void computeAggregateStats() {
        // Общий урон со всех параметров
        float totalDamage = 0.0f;
        float totalDamageMultiplier = 1.0f;
        
        for (SpellComputationResult result : parameterResults) {
            if (result.hasComputedValue("final_damage")) {
                totalDamage += result.getComputedFloat("final_damage", 0.0f);
            }
            
            if (result.hasComputedValue("damage_multiplier")) {
                float multiplier = result.getComputedFloat("damage_multiplier", 1.0f);
                totalDamageMultiplier *= multiplier;
            }
        }
        
        aggregatedValues.put("total_damage", totalDamage);
        aggregatedValues.put("total_damage_multiplier", totalDamageMultiplier);
        
        // Вычисляем "силу" заклинания для визуальных эффектов
        float spellPower = calculateSpellPower();
        aggregatedValues.put("spell_power", spellPower);
        
        // Определяем приоритет визуальных эффектов
        if (spellPower > 100.0f) {
            visualEffects.add("epic_effects");
        } else if (spellPower > 50.0f) {
            visualEffects.add("enhanced_effects");
        } else {
            visualEffects.add("basic_effects");
        }
    }
    
    /**
     * Вычислить общую "силу" заклинания
     */
    private float calculateSpellPower() {
        float power = 0.0f;
        
        // Учитываем урон
        power += getAggregatedFloat("total_damage", 0.0f);
        
        // Учитываем размер
        if (hasAggregatedValue("geometry_size")) {
            float size = getAggregatedFloat("geometry_size", 1.0f);
            power += size * 10.0f;
        }
        
        // Учитываем элементальные эффекты
        for (SpellComputationResult result : parameterResults) {
            if (result.getParameterKey().startsWith("elemental_")) {
                power += 15.0f; // Каждый элемент +15 к силе
            }
        }
        
        return power;
    }
    
    // === Геттеры ===
    
    public UUID getSpellInstanceId() { return spellInstanceId; }
    public long getComputationDurationNanos() { return computationDurationNanos; }
    public long getTimestamp() { return timestamp; }
    
    public List<SpellComputationResult> getParameterResults() {
        return new ArrayList<>(parameterResults);
    }
    
    public Map<String, Object> getAggregatedValues() {
        return new HashMap<>(aggregatedValues);
    }
    
    public Map<String, String> getCollisionModifications() {
        return new HashMap<>(collisionModifications);
    }
    
    public Map<String, Object> getFormModifications() {
        return new HashMap<>(formModifications);
    }
    
    public List<String> getVisualEffects() {
        return new ArrayList<>(visualEffects);
    }
    
    // Флаги для pipeline
    public boolean hasErrors() { return hasErrors; }
    public boolean needsCollisionUpdate() { return needsCollisionUpdate; }
    public boolean needsMainThreadApplication() { return needsMainThreadApplication; }
    public boolean needsVisualsUpdate() { return needsVisualsUpdate; }
    
    // Статистика
    public int getSuccessfulParameters() { return successfulParameters; }
    public int getErrorParameters() { return errorParameters; }
    public int getTotalParameters() { return parameterResults.size(); }
    
    // Утилиты для работы с агрегированными значениями
    public boolean hasAggregatedValue(String key) {
        return aggregatedValues.containsKey(key);
    }
    
    public float getAggregatedFloat(String key, float defaultValue) {
        Object value = aggregatedValues.get(key);
        return value instanceof Number number ? number.floatValue() : defaultValue;
    }
    
    public String getAggregatedString(String key, String defaultValue) {
        Object value = aggregatedValues.get(key);
        return value instanceof String str ? str : defaultValue;
    }
    
    // === Статические методы ===
    
    /**
     * Создать результат с ошибкой
     */
    public static SpellComputationTaskResult error(UUID spellId, Exception error, long duration) {
        SpellComputationTaskResult result = new SpellComputationTaskResult(spellId, duration);
        result.hasErrors = true;
        result.aggregatedValues.put("error", error.getMessage());
        result.aggregatedValues.put("error_type", error.getClass().getSimpleName());
        return result;
    }
    
    @Override
    public String toString() {
        return String.format(
            "SpellComputationTaskResult{spell=%s, duration=%.2fms, params=%d/%d, errors=%s}",
            spellInstanceId, computationDurationNanos / 1_000_000.0, 
            successfulParameters, getTotalParameters(), hasErrors
        );
    }
}