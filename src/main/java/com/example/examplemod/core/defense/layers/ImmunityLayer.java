package com.example.examplemod.core.defense.layers;

import com.example.examplemod.core.defense.DefenseLayer;
import com.example.examplemod.core.defense.DefenseLayerResult;
import com.example.examplemod.core.defense.DamageEvent;
import com.example.examplemod.core.defense.DamageType;
import com.example.examplemod.core.defense.LayeredDefenseSystem;

/**
 * Immunity Layer - полный иммунитет к определенному типу урона
 * Приоритет: 1 (самый высокий)
 */
public class ImmunityLayer implements DefenseLayer {
    private final DamageType immunityType;
    private final String description;
    private boolean active = true;
    
    public ImmunityLayer(DamageType immunityType) {
        this.immunityType = immunityType;
        this.description = "Immunity to " + immunityType.name();
    }
    
    @Override
    public DefenseLayerResult process(DamageEvent damageEvent) {
        if (!canProcess(damageEvent)) {
            return DefenseLayerResult.noEffect(this);
        }
        
        // Полная блокировка урона данного типа
        DamageEvent blockedEvent = DamageEvent.builder(damageEvent)
                .damage(0.0f)
                .blocked(true)
                .build();
        
        return DefenseLayerResult.builder()
                .layer(this)
                .success(true)
                .damageCompletelyBlocked(true)
                .modifiedDamageEvent(blockedEvent)
                .description("Damage completely blocked by immunity to " + immunityType.name())
                .build();
    }
    
    @Override
    public boolean canProcess(DamageEvent damageEvent) {
        return isActive() && damageEvent.getDamageType() == immunityType;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public LayeredDefenseSystem.DefenseLayerType getLayerType() {
        return LayeredDefenseSystem.DefenseLayerType.IMMUNITY;
    }
    
    @Override
    public int getPriority() {
        return 1000; // Высший приоритет
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public DamageType getImmunityType() {
        return immunityType;
    }
}