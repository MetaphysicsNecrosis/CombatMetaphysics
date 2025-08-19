package com.example.examplemod.core.defense;

/**
 * Результат обработки урона через слой защиты
 */
public class DefenseLayerResult {
    private final DefenseLayer layer;
    private final boolean success;
    private final boolean damageCompletelyBlocked;
    private final DamageEvent modifiedDamageEvent;
    private final float damageReduced;
    private final float damageReflected;
    private final String description;
    
    private DefenseLayerResult(Builder builder) {
        this.layer = builder.layer;
        this.success = builder.success;
        this.damageCompletelyBlocked = builder.damageCompletelyBlocked;
        this.modifiedDamageEvent = builder.modifiedDamageEvent;
        this.damageReduced = builder.damageReduced;
        this.damageReflected = builder.damageReflected;
        this.description = builder.description;
    }
    
    // Getters
    public DefenseLayer getLayer() { return layer; }
    public boolean isSuccess() { return success; }
    public boolean isDamageCompletelyBlocked() { return damageCompletelyBlocked; }
    public DamageEvent getModifiedDamageEvent() { return modifiedDamageEvent; }
    public float getDamageReduced() { return damageReduced; }
    public float getDamageReflected() { return damageReflected; }
    public String getDescription() { return description; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DefenseLayer layer;
        private boolean success = false;
        private boolean damageCompletelyBlocked = false;
        private DamageEvent modifiedDamageEvent;
        private float damageReduced = 0.0f;
        private float damageReflected = 0.0f;
        private String description = "";
        
        public Builder layer(DefenseLayer layer) {
            this.layer = layer;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder damageCompletelyBlocked(boolean blocked) {
            this.damageCompletelyBlocked = blocked;
            return this;
        }
        
        public Builder modifiedDamageEvent(DamageEvent event) {
            this.modifiedDamageEvent = event;
            return this;
        }
        
        public Builder damageReduced(float reduced) {
            this.damageReduced = reduced;
            return this;
        }
        
        public Builder damageReflected(float reflected) {
            this.damageReflected = reflected;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public DefenseLayerResult build() {
            return new DefenseLayerResult(this);
        }
    }
    
    // Фабричные методы
    public static DefenseLayerResult noEffect(DefenseLayer layer) {
        return builder()
                .layer(layer)
                .success(false)
                .description("No effect from " + layer.getDescription())
                .build();
    }
    
    public static DefenseLayerResult completeBlock(DefenseLayer layer, String reason) {
        return builder()
                .layer(layer)
                .success(true)
                .damageCompletelyBlocked(true)
                .description(reason)
                .build();
    }
}