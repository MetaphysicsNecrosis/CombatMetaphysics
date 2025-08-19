package com.example.examplemod.core.defense;

import java.util.List;
import java.util.ArrayList;

/**
 * Способности защиты сущности
 */
public class DefenseCapabilities {
    private final List<DamageType> immunities = new ArrayList<>();
    private final List<ResistanceData> resistances = new ArrayList<>();
    private final List<AbsorptionData> absorptions = new ArrayList<>();
    private final List<ReflectionData> reflections = new ArrayList<>();
    private final List<ConversionData> conversions = new ArrayList<>();
    
    // Getters
    public List<DamageType> getImmunities() { return new ArrayList<>(immunities); }
    public List<ResistanceData> getResistances() { return new ArrayList<>(resistances); }
    public List<AbsorptionData> getAbsorptions() { return new ArrayList<>(absorptions); }
    public List<ReflectionData> getReflections() { return new ArrayList<>(reflections); }
    public List<ConversionData> getConversions() { return new ArrayList<>(conversions); }
    
    // Builders
    public DefenseCapabilities addImmunity(DamageType damageType) {
        immunities.add(damageType);
        return this;
    }
    
    public DefenseCapabilities addResistance(DamageType damageType, float percentage) {
        resistances.add(new ResistanceData(damageType, percentage));
        return this;
    }
    
    public DefenseCapabilities addAbsorption(DamageType damageType, float amount) {
        absorptions.add(new AbsorptionData(damageType, amount));
        return this;
    }
    
    public DefenseCapabilities addReflection(DamageType damageType, float percentage) {
        reflections.add(new ReflectionData(damageType, percentage));
        return this;
    }
    
    public DefenseCapabilities addConversion(DamageType fromType, DamageType toType, float ratio) {
        conversions.add(new ConversionData(fromType, toType, ratio));
        return this;
    }
    
    // Data records
    public record ResistanceData(DamageType damageType, float percentage) {}
    public record AbsorptionData(DamageType damageType, float amount) {}
    public record ReflectionData(DamageType damageType, float percentage) {}
    public record ConversionData(DamageType fromType, DamageType toType, float ratio) {}
    
    // Фабричные методы для частых конфигураций
    public static DefenseCapabilities createBasicArmor() {
        return new DefenseCapabilities()
                .addResistance(DamageType.PHYSICAL, 0.2f)
                .addResistance(DamageType.SLASHING, 0.1f);
    }
    
    public static DefenseCapabilities createMagicalWards() {
        return new DefenseCapabilities()
                .addResistance(DamageType.MAGICAL, 0.3f)
                .addResistance(DamageType.ARCANE, 0.5f);
    }
    
    public static DefenseCapabilities createElementalResistance() {
        return new DefenseCapabilities()
                .addResistance(DamageType.FIRE, 0.4f)
                .addResistance(DamageType.ICE, 0.4f)
                .addResistance(DamageType.LIGHTNING, 0.4f);
    }
}