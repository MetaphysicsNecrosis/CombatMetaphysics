package com.example.examplemod.core.defense;

import net.minecraft.world.entity.Entity;

/**
 * Событие урона для обработки через Layered Defense Model
 */
public class DamageEvent {
    private final Entity attacker;
    private final Entity target;
    private final float damage;
    private final DamageType damageType;
    private final boolean blocked;
    private final boolean reflected;
    private final boolean converted;
    private final long timestamp;
    
    private DamageEvent(Builder builder) {
        this.attacker = builder.attacker;
        this.target = builder.target;
        this.damage = builder.damage;
        this.damageType = builder.damageType;
        this.blocked = builder.blocked;
        this.reflected = builder.reflected;
        this.converted = builder.converted;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public Entity getAttacker() { return attacker; }
    public Entity getTarget() { return target; }
    public Entity getDefender() { return target; } // Алиас для защитника
    public float getDamage() { return damage; }
    public DamageType getDamageType() { return damageType; }
    public boolean isBlocked() { return blocked; }
    public boolean isReflected() { return reflected; }
    public boolean isConverted() { return converted; }
    public long getTimestamp() { return timestamp; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(DamageEvent original) {
        return new Builder()
                .attacker(original.attacker)
                .target(original.target)
                .damage(original.damage)
                .damageType(original.damageType)
                .blocked(original.blocked)
                .reflected(original.reflected)
                .converted(original.converted);
    }
    
    public static class Builder {
        private Entity attacker;
        private Entity target;
        private float damage;
        private DamageType damageType = DamageType.GENERIC;
        private boolean blocked = false;
        private boolean reflected = false;
        private boolean converted = false;
        
        public Builder attacker(Entity attacker) {
            this.attacker = attacker;
            return this;
        }
        
        public Builder target(Entity target) {
            this.target = target;
            return this;
        }
        
        public Builder damage(float damage) {
            this.damage = damage;
            return this;
        }
        
        public Builder damageType(DamageType damageType) {
            this.damageType = damageType;
            return this;
        }
        
        public Builder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }
        
        public Builder reflected(boolean reflected) {
            this.reflected = reflected;
            return this;
        }
        
        public Builder converted(boolean converted) {
            this.converted = converted;
            return this;
        }
        
        public Builder defender(Entity defender) {
            this.target = defender;
            return this;
        }
        
        public DamageEvent build() {
            return new DamageEvent(this);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DamageEvent other)) return false;
        
        return Float.compare(damage, other.damage) == 0 &&
               blocked == other.blocked &&
               damageType == other.damageType &&
               (attacker == null ? other.attacker == null : attacker.equals(other.attacker)) &&
               (target == null ? other.target == null : target.equals(other.target));
    }
    
    @Override
    public int hashCode() {
        int result = attacker != null ? attacker.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + Float.floatToIntBits(damage);
        result = 31 * result + (damageType != null ? damageType.hashCode() : 0);
        result = 31 * result + (blocked ? 1 : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("DamageEvent{damage=%.1f, type=%s, blocked=%s, attacker=%s, target=%s}",
                damage, damageType, blocked, 
                attacker != null ? attacker.getName().getString() : "null",
                target != null ? target.getName().getString() : "null");
    }
}