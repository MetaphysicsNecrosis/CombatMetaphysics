package com.example.examplemod.core.spells.forms.instant;

/**
 * Описание отложенного эффекта для мгновенных заклинаний
 * Используется для создания последовательностей эффектов
 */
public class InstantEffect {
    
    public final String type;        // Тип эффекта (FIRE, ICE, DAMAGE, HEAL, etc.)
    public final int delay;          // Задержка в тиках до применения
    public final float intensity;    // Интенсивность эффекта
    
    public InstantEffect(String type, int delay) {
        this(type, delay, 1.0f);
    }
    
    public InstantEffect(String type, int delay, float intensity) {
        this.type = type;
        this.delay = delay;
        this.intensity = intensity;
    }
    
    @Override
    public String toString() {
        return String.format("InstantEffect{type='%s', delay=%d, intensity=%.2f}", 
                           type, delay, intensity);
    }
}