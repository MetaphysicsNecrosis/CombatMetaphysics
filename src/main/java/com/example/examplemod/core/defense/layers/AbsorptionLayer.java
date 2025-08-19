package com.example.examplemod.core.defense.layers;

import com.example.examplemod.core.defense.DefenseLayer;
import com.example.examplemod.core.defense.DefenseLayerResult;
import com.example.examplemod.core.defense.DamageEvent;
import com.example.examplemod.core.defense.DamageType;
import com.example.examplemod.core.defense.LayeredDefenseSystem;

/**
 * Absorption Layer - поглощает фиксированное количество урона
 */
public class AbsorptionLayer implements DefenseLayer {
    private final DamageType damageType;
    private float remainingAbsorption;
    private final float maxAbsorption;
    private final String description;
    private boolean active = true;
    
    public AbsorptionLayer(DamageType damageType, float amount) {
        this.damageType = damageType;
        this.remainingAbsorption = amount;
        this.maxAbsorption = amount;
        this.description = "Absorption: " + amount + " " + damageType.name() + " damage";
    }
    
    @Override
    public DefenseLayerResult process(DamageEvent damageEvent) {
        if (!canProcess(damageEvent)) {
            return DefenseLayerResult.noEffect(this);
        }
        
        float incomingDamage = damageEvent.getDamage();
        float absorbed = Math.min(incomingDamage, remainingAbsorption);
        float remainingDamage = incomingDamage - absorbed;
        
        remainingAbsorption -= absorbed;
        if (remainingAbsorption <= 0) {
            active = false; // Исчерпали поглощение
        }
        
        DamageEvent modifiedEvent = DamageEvent.builder(damageEvent)
                .damage(remainingDamage)
                .build();
        
        return DefenseLayerResult.builder()
                .layer(this)
                .success(absorbed > 0)
                .damageCompletelyBlocked(remainingDamage <= 0)
                .modifiedDamageEvent(modifiedEvent)
                .description(String.format("Absorbed %.1f damage, %.1f remaining", 
                           absorbed, remainingAbsorption))
                .build();
    }
    
    @Override
    public boolean canProcess(DamageEvent damageEvent) {
        return isActive() && damageEvent.getDamageType() == damageType && remainingAbsorption > 0;
    }
    
    @Override
    public boolean isActive() {
        return active && remainingAbsorption > 0;
    }
    
    @Override
    public LayeredDefenseSystem.DefenseLayerType getLayerType() {
        return LayeredDefenseSystem.DefenseLayerType.ABSORPTION;
    }
    
    @Override
    public int getPriority() {
        return 700; // Средний приоритет
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public float getRemainingAbsorption() {
        return remainingAbsorption;
    }
    
    public float getMaxAbsorption() {
        return maxAbsorption;
    }
    
    public void rechargeAbsorption(float amount) {
        remainingAbsorption = Math.min(maxAbsorption, remainingAbsorption + amount);
        if (remainingAbsorption > 0) {
            active = true;
        }
    }
}