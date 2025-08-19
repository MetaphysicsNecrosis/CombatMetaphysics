package com.example.examplemod.core.defense.layers;

import com.example.examplemod.core.defense.DefenseLayer;
import com.example.examplemod.core.defense.DefenseLayerResult;
import com.example.examplemod.core.defense.DamageEvent;
import com.example.examplemod.core.defense.DamageType;
import com.example.examplemod.core.defense.LayeredDefenseSystem;

/**
 * Resistance Layer - процентное снижение урона
 * Приоритет: 2
 */
public class ResistanceLayer implements DefenseLayer {
    private final DamageType damageType;
    private final float resistancePercentage; // 0.0 - 1.0 (0% - 100%)
    private final String description;
    private boolean active = true;
    
    public ResistanceLayer(DamageType damageType, float resistancePercentage) {
        this.damageType = damageType;
        this.resistancePercentage = Math.max(0.0f, Math.min(1.0f, resistancePercentage));
        this.description = String.format("%.0f%% resistance to %s", 
                this.resistancePercentage * 100, damageType.name());
    }
    
    @Override
    public DefenseLayerResult process(DamageEvent damageEvent) {
        if (!canProcess(damageEvent)) {
            return DefenseLayerResult.noEffect(this);
        }
        
        float originalDamage = damageEvent.getDamage();
        float damageReduction = originalDamage * resistancePercentage;
        float finalDamage = originalDamage - damageReduction;
        
        DamageEvent modifiedEvent = DamageEvent.builder(damageEvent)
                .damage(finalDamage)
                .build();
        
        return DefenseLayerResult.builder()
                .layer(this)
                .success(true)
                .damageCompletelyBlocked(finalDamage <= 0)
                .modifiedDamageEvent(modifiedEvent)
                .damageReduced(damageReduction)
                .description(String.format("Reduced %.1f damage (%.0f%% resistance)", 
                        damageReduction, resistancePercentage * 100))
                .build();
    }
    
    @Override
    public boolean canProcess(DamageEvent damageEvent) {
        return isActive() && 
               (damageType == DamageType.ALL || damageEvent.getDamageType() == damageType) &&
               damageEvent.getDamage() > 0;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public LayeredDefenseSystem.DefenseLayerType getLayerType() {
        return LayeredDefenseSystem.DefenseLayerType.RESISTANCE;
    }
    
    @Override
    public int getPriority() {
        return 900; // Высокий приоритет, но ниже Immunity
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public DamageType getDamageType() {
        return damageType;
    }
    
    public float getResistancePercentage() {
        return resistancePercentage;
    }
}