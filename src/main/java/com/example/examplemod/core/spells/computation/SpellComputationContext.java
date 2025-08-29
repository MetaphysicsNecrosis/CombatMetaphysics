package com.example.examplemod.core.spells.computation;

import com.example.examplemod.core.spells.parameters.SpellParameters;
import java.util.UUID;

/**
 * Thread-safe контекст для вычисления параметров заклинания
 * НЕ содержит Minecraft объектов (Level, Player, Entity) для безопасности потоков
 * 
 * Соответствует архитектуре: Main Thread -> Spell Computation Pool (ВОТ МЫ ЗДЕСЬ)
 */
public class SpellComputationContext {
    
    // Безопасные данные - могут использоваться в worker threads
    private final UUID spellInstanceId;
    private final String spellType;
    private final SpellParameters allParameters;
    private final float currentTime; // game time snapshot
    
    // Снепшоты позиции - безопасны для использования в worker threads
    private final double casterX, casterY, casterZ;
    private final float casterYaw, casterPitch;
    
    // Характеристики кастера - копии, не ссылки на Minecraft объекты
    private final float casterManaMax;
    private final float casterManaInitiationAvailable;
    private final float casterManaAmplificationAvailable;
    
    // Состояние мира - снепшоты, не ссылки
    private final boolean isRaining;
    private final float worldTime;
    private final int dimensionId;
    
    public SpellComputationContext(UUID spellInstanceId, String spellType, 
                                  SpellParameters allParameters, float currentTime,
                                  double casterX, double casterY, double casterZ,
                                  float casterYaw, float casterPitch,
                                  float casterManaMax, float casterManaInitiationAvailable,
                                  float casterManaAmplificationAvailable,
                                  boolean isRaining, float worldTime, int dimensionId) {
        this.spellInstanceId = spellInstanceId;
        this.spellType = spellType;
        this.allParameters = allParameters.copy(); // Defensive copy
        this.currentTime = currentTime;
        this.casterX = casterX;
        this.casterY = casterY;
        this.casterZ = casterZ;
        this.casterYaw = casterYaw;
        this.casterPitch = casterPitch;
        this.casterManaMax = casterManaMax;
        this.casterManaInitiationAvailable = casterManaInitiationAvailable;
        this.casterManaAmplificationAvailable = casterManaAmplificationAvailable;
        this.isRaining = isRaining;
        this.worldTime = worldTime;
        this.dimensionId = dimensionId;
    }
    
    // Getters - все thread-safe
    public UUID getSpellInstanceId() { return spellInstanceId; }
    public String getSpellType() { return spellType; }
    public SpellParameters getAllParameters() { return allParameters; }
    public float getCurrentTime() { return currentTime; }
    
    public double getCasterX() { return casterX; }
    public double getCasterY() { return casterY; }
    public double getCasterZ() { return casterZ; }
    public float getCasterYaw() { return casterYaw; }
    public float getCasterPitch() { return casterPitch; }
    
    public float getCasterManaMax() { return casterManaMax; }
    public float getCasterManaInitiationAvailable() { return casterManaInitiationAvailable; }
    public float getCasterManaAmplificationAvailable() { return casterManaAmplificationAvailable; }
    
    public boolean isRaining() { return isRaining; }
    public float getWorldTime() { return worldTime; }
    public int getDimensionId() { return dimensionId; }
    
    /**
     * Получить параметр как число
     */
    public float getParameterFloat(String key, float defaultValue) {
        return allParameters.getFloat(key, defaultValue);
    }
    
    /**
     * Проверить наличие параметра
     */
    public boolean hasParameter(String key) {
        return allParameters.hasParameter(key);
    }
    
    // === ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ДЛЯ СОВМЕСТИМОСТИ ===
    
    public String getFormType() {
        return spellType; // Alias для совместимости
    }
    
    public float getFloat(String key, int defaultValue) {
        return getParameterFloat(key, (float) defaultValue);
    }
    
    public boolean hasElementalIntensity(String element) {
        return hasParameter("elemental_" + element);
    }
    
    public float getElementalIntensity(String element) {
        return getParameterFloat("elemental_" + element, 0.0f);
    }
    
    public java.util.Map<String, Float> getElementalMix() {
        java.util.Map<String, Float> mix = new java.util.HashMap<>();
        String[] elements = {"fire", "water", "earth", "air", "ice", "lightning", "light", "shadow", "spirit", "void"};
        for (String element : elements) {
            float intensity = getElementalIntensity(element);
            if (intensity > 0) {
                mix.put(element, intensity);
            }
        }
        return mix;
    }
}