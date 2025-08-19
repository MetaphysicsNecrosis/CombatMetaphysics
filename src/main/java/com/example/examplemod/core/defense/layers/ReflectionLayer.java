package com.example.examplemod.core.defense.layers;

import com.example.examplemod.core.defense.DefenseLayer;
import com.example.examplemod.core.defense.DefenseLayerResult;
import com.example.examplemod.core.defense.DamageEvent;
import com.example.examplemod.core.defense.DamageType;
import com.example.examplemod.core.defense.LayeredDefenseSystem;

/**
 * Reflection Layer - отражает часть урона обратно в атакующего
 */
public class ReflectionLayer implements DefenseLayer {
    private final DamageType damageType;
    private final float reflectionPercentage;
    private final String description;
    private boolean active = true;
    
    public ReflectionLayer(DamageType damageType, float percentage) {
        this.damageType = damageType;
        this.reflectionPercentage = Math.max(0, Math.min(1, percentage)); // 0-1
        this.description = "Reflection: " + (percentage * 100) + "% " + damageType.name() + " damage";
    }
    
    @Override
    public DefenseLayerResult process(DamageEvent damageEvent) {
        if (!canProcess(damageEvent)) {
            return DefenseLayerResult.noEffect(this);
        }
        
        float incomingDamage = damageEvent.getDamage();
        float reflectedDamage = incomingDamage * reflectionPercentage;
        
        // Создаем событие отраженного урона
        DamageEvent reflectedEvent = DamageEvent.builder()
                .attacker(damageEvent.getDefender()) // Защитник становится атакующим
                .defender(damageEvent.getAttacker()) // Атакующий получает урон
                .damage(reflectedDamage)
                .damageType(damageType)
                .reflected(true)
                .build();
        
        // Оригинальный урон проходит полностью
        return DefenseLayerResult.builder()
                .layer(this)
                .success(true)
                .damageCompletelyBlocked(false)
                .modifiedDamageEvent(damageEvent) // Не изменяем входящий урон
                .additionalDamageEvent(reflectedEvent) // Добавляем отраженный урон
                .description(String.format("Reflected %.1f damage (%.0f%%) back to attacker", 
                           reflectedDamage, reflectionPercentage * 100))
                .build();
    }
    
    @Override
    public boolean canProcess(DamageEvent damageEvent) {
        return isActive() && 
               damageEvent.getDamageType() == damageType && 
               !damageEvent.isReflected() && // Не отражаем уже отраженный урон
               damageEvent.getAttacker() != null; // Есть кого "контратаковать"
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public LayeredDefenseSystem.DefenseLayerType getLayerType() {
        return LayeredDefenseSystem.DefenseLayerType.REFLECTION;
    }
    
    @Override
    public int getPriority() {
        return 400; // Низкий приоритет - отражаем после всех других защит
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public float getReflectionPercentage() {
        return reflectionPercentage;
    }
    
    public DamageType getDamageType() {
        return damageType;
    }
}