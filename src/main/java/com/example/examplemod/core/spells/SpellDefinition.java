package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.forms.SpellFormType;
import net.minecraft.nbt.CompoundTag;
import java.util.Map;

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
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString("type", type);
        tag.putString("form_type", formType.name());
        tag.putFloat("base_mana_initiation_cost", baseManaInitiationCost);
        tag.putFloat("base_mana_amplification_cost", baseManaAmplificationCost);
        tag.putInt("base_cast_time", baseCastTime);
        tag.putBoolean("requires_qte", requiresQTE);
        
        CompoundTag paramsTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : baseParameters.getAllParameters().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Float f) {
                paramsTag.putFloat(entry.getKey(), f);
            } else if (value instanceof Integer i) {
                paramsTag.putInt(entry.getKey(), i);
            } else if (value instanceof Double d) {
                paramsTag.putDouble(entry.getKey(), d);
            } else if (value instanceof String s) {
                paramsTag.putString(entry.getKey(), s);
            } else if (value instanceof Boolean b) {
                paramsTag.putBoolean(entry.getKey(), b);
            }
        }
        tag.put("parameters", paramsTag);
        
        return tag;
    }
    
    public static SpellDefinition fromNBT(CompoundTag tag) {
        SpellParameters params = new SpellParameters();
        
        if (tag.contains("parameters")) {
            CompoundTag paramsTag = tag.getCompoundOrEmpty("parameters");
            for (String key : paramsTag.keySet()) {
                if (paramsTag.contains(key)) {
                    if (paramsTag.get(key) instanceof net.minecraft.nbt.FloatTag) {
                        params.setParameter(key, paramsTag.getFloatOr(key, 0f));
                    } else if (paramsTag.get(key) instanceof net.minecraft.nbt.IntTag) {
                        params.setParameter(key, paramsTag.getIntOr(key, 0));
                    } else if (paramsTag.get(key) instanceof net.minecraft.nbt.DoubleTag) {
                        params.setParameter(key, paramsTag.getDoubleOr(key, 0d));
                    } else if (paramsTag.get(key) instanceof net.minecraft.nbt.StringTag) {
                        params.setParameter(key, paramsTag.getStringOr(key, ""));
                    } else if (paramsTag.get(key) instanceof net.minecraft.nbt.ByteTag) {
                        params.setParameter(key, paramsTag.getBooleanOr(key, false));
                    }
                }
            }
        }
        
        return new SpellDefinition(
            tag.getStringOr("id", ""),
            tag.getStringOr("name", ""),
            tag.getStringOr("type", ""),
            SpellFormType.valueOf(tag.getStringOr("form_type", "PROJECTILE")),
            params,
            tag.getFloatOr("base_mana_initiation_cost", 10f),
            tag.getFloatOr("base_mana_amplification_cost", 5f),
            tag.getIntOr("base_cast_time", 20),
            tag.getBooleanOr("requires_qte", false)
        );
    }
}