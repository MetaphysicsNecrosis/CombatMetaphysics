package com.example.examplemod.core.spells.parameters;

import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Базовый класс для всех параметров заклинаний
 * Реализует общую логику вычислений и результатов
 */
public abstract class AbstractSpellParameter<T> implements ISpellParameter {
    
    protected final String parameterName;
    protected final Class<T> valueType;
    
    protected AbstractSpellParameter(String parameterName, Class<T> valueType) {
        this.parameterName = parameterName;
        this.valueType = valueType;
    }
    
    @Override
    public String getKey() {
        return parameterName;
    }
    
    @Override
    public Class<T> getValueType() {
        return valueType;
    }
    
    /**
     * Абстрактный метод вычисления параметра
     * Должен быть реализован в подклассах
     */
    @Override
    public abstract SpellComputationResult compute(SpellComputationContext context, Object value);
    
    /**
     * Парсинг float значения с fallback
     */
    protected float parseFloat(Object value, float defaultValue) {
        if (value == null) return defaultValue;
        
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Парсинг int значения с fallback
     */
    protected int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Создать пустой результат
     */
    protected SpellComputationResult emptyResult() {
        return new SpellComputationResult();
    }
    
    /**
     * Builder для создания результатов
     */
    protected SpellComputationResult.Builder buildResult() {
        return SpellComputationResult.builder();
    }
    
    /**
     * Создать простой результат с одним значением
     */
    protected SpellComputationResult simpleResult(String key, Object value) {
        return SpellComputationResult.builder()
            .putValue(key, value)
            .build();
    }
}