package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.forms.SpellFormType;

/**
 * Определение заклинания - неизменяемый объект с базовой информацией
 * Thread-safe для использования в многопоточной архитектуре
 */
public record SpellDefinition(
    String id,
    String name,
    String type,
    SpellFormType formType,
    SpellParameters baseParameters,
    float baseManaInitiationCost,
    float baseManaAmplificationCost,
    int baseCastTime,
    boolean requiresQTE
) {
    
    public SpellFormType form() {
        return formType;
    }
    
    public SpellParameters parameters() {
        return baseParameters;
    }
    
    public record Metadata(boolean enabled) {}
    
    public Metadata metadata() {
        return new Metadata(true);
    }
    
    public static Builder builder(net.minecraft.resources.ResourceLocation id) {
        return new Builder(id.toString());
    }
    
    public static class Builder {
        private final String id;
        private String name = "";
        private SpellFormType formType = SpellFormType.PROJECTILE;
        private SpellParameters parameters = new SpellParameters();
        
        private Builder(String id) {
            this.id = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder form(SpellFormType formType) {
            this.formType = formType;
            return this;
        }
        
        public Builder parameters(SpellParameters parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder metadata(SpellMetadata metadata) {
            return this;
        }
        
        public SpellDefinition build() {
            return new SpellDefinition(
                id, name, formType.name(), formType,
                parameters, 10.0f, 5.0f, 20, false
            );
        }
    }
}