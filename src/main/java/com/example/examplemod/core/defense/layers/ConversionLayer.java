package com.example.examplemod.core.defense.layers;

import com.example.examplemod.core.defense.DefenseLayer;
import com.example.examplemod.core.defense.DefenseLayerResult;
import com.example.examplemod.core.defense.DamageEvent;
import com.example.examplemod.core.defense.DamageType;
import com.example.examplemod.core.defense.LayeredDefenseSystem;

/**
 * Conversion Layer - конвертирует один тип урона в другой
 */
public class ConversionLayer implements DefenseLayer {
    private final DamageType fromType;
    private final DamageType toType;
    private final float conversionRatio;
    private final String description;
    private boolean active = true;
    
    public ConversionLayer(DamageType fromType, DamageType toType, float ratio) {
        this.fromType = fromType;
        this.toType = toType;
        this.conversionRatio = Math.max(0, ratio);
        this.description = String.format("Convert %s to %s (%.1fx ratio)", 
                                        fromType.name(), toType.name(), ratio);
    }
    
    @Override
    public DefenseLayerResult process(DamageEvent damageEvent) {
        if (!canProcess(damageEvent)) {
            return DefenseLayerResult.noEffect(this);
        }
        
        float originalDamage = damageEvent.getDamage();
        float convertedDamage = originalDamage * conversionRatio;
        
        // Создаем событие конвертированного урона
        DamageEvent convertedEvent = DamageEvent.builder(damageEvent)
                .damage(convertedDamage)
                .damageType(toType)
                .converted(true)
                .build();
        
        return DefenseLayerResult.builder()
                .layer(this)
                .success(true)
                .damageCompletelyBlocked(false)
                .modifiedDamageEvent(convertedEvent)
                .description(String.format("Converted %.1f %s damage to %.1f %s damage", 
                           originalDamage, fromType.name(), 
                           convertedDamage, toType.name()))
                .build();
    }
    
    @Override
    public boolean canProcess(DamageEvent damageEvent) {
        return isActive() && 
               damageEvent.getDamageType() == fromType &&
               !damageEvent.isConverted(); // Не конвертируем уже конвертированный урон
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public LayeredDefenseSystem.DefenseLayerType getLayerType() {
        return LayeredDefenseSystem.DefenseLayerType.CONVERSION;
    }
    
    @Override
    public int getPriority() {
        return 500; // Средний приоритет
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public DamageType getFromType() {
        return fromType;
    }
    
    public DamageType getToType() {
        return toType;
    }
    
    public float getConversionRatio() {
        return conversionRatio;
    }
}