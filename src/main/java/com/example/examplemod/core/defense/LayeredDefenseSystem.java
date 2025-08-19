package com.example.examplemod.core.defense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Layered Defense Model по архитектуре Opus
 * Цепочка защитных слоев: Immunity → Resistance → Absorption → Reflection → Conversion
 */
public class LayeredDefenseSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayeredDefenseSystem.class);
    
    // Порядок обработки слоев защиты (по приоритету)
    private static final List<DefenseLayerType> LAYER_ORDER = List.of(
        DefenseLayerType.IMMUNITY,      // 1. Полный иммунитет
        DefenseLayerType.RESISTANCE,    // 2. Процентное снижение
        DefenseLayerType.ABSORPTION,    // 3. Поглощение первых N единиц
        DefenseLayerType.REFLECTION,    // 4. Отражение обратно
        DefenseLayerType.CONVERSION     // 5. Превращение урона в другой тип
    );
    
    /**
     * Обрабатывает урон через все слои защиты
     */
    public DamageProcessingResult processDamage(DamageEvent damageEvent, 
                                               DefenseConfiguration defenseConfig) {
        DamageProcessingResult result = new DamageProcessingResult(damageEvent);
        
        // Обрабатываем каждый слой последовательно
        for (DefenseLayerType layerType : LAYER_ORDER) {
            List<DefenseLayer> layers = defenseConfig.getLayersOfType(layerType);
            
            for (DefenseLayer layer : layers) {
                if (layer.isActive() && layer.canProcess(result.getCurrentDamageEvent())) {
                    DefenseLayerResult layerResult = layer.process(result.getCurrentDamageEvent());
                    result.addLayerResult(layerType, layer, layerResult);
                    
                    // Если урон был полностью заблокирован - прерываем обработку
                    if (layerResult.isDamageCompletelyBlocked()) {
                        LOGGER.debug("Damage completely blocked by {} layer: {}", 
                                layerType, layer.getClass().getSimpleName());
                        return result;
                    }
                    
                    // Обновляем событие урона для следующего слоя
                    if (layerResult.getModifiedDamageEvent() != null) {
                        result.setCurrentDamageEvent(layerResult.getModifiedDamageEvent());
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Создает конфигурацию защиты на основе способностей сущности
     */
    public DefenseConfiguration createDefenseConfiguration(DefenseCapabilities capabilities) {
        DefenseConfiguration config = new DefenseConfiguration();
        
        // Добавляем слои на основе способностей
        capabilities.getImmunities().forEach(immunity -> 
                config.addLayer(new ImmunityLayer(immunity)));
        
        capabilities.getResistances().forEach(resistance ->
                config.addLayer(new ResistanceLayer(resistance.damageType(), resistance.percentage())));
        
        capabilities.getAbsorptions().forEach(absorption ->
                config.addLayer(new AbsorptionLayer(absorption.damageType(), absorption.amount())));
        
        capabilities.getReflections().forEach(reflection ->
                config.addLayer(new ReflectionLayer(reflection.damageType(), reflection.percentage())));
        
        capabilities.getConversions().forEach(conversion ->
                config.addLayer(new ConversionLayer(conversion.fromType(), conversion.toType(), conversion.ratio())));
        
        LOGGER.debug("Created defense configuration with {} layers", config.getTotalLayerCount());
        return config;
    }
    
    /**
     * Результат обработки урона через все слои
     */
    public static class DamageProcessingResult {
        private final DamageEvent originalDamageEvent;
        private DamageEvent currentDamageEvent;
        private final Map<DefenseLayerType, List<LayerProcessingResult>> layerResults = new HashMap<>();
        private final long processingStartTime;
        
        public DamageProcessingResult(DamageEvent originalDamageEvent) {
            this.originalDamageEvent = originalDamageEvent;
            this.currentDamageEvent = originalDamageEvent;
            this.processingStartTime = System.currentTimeMillis();
        }
        
        public void addLayerResult(DefenseLayerType layerType, DefenseLayer layer, 
                                 DefenseLayerResult layerResult) {
            layerResults.computeIfAbsent(layerType, k -> new ArrayList<>())
                       .add(new LayerProcessingResult(layer, layerResult));
        }
        
        public DamageEvent getOriginalDamageEvent() { return originalDamageEvent; }
        public DamageEvent getCurrentDamageEvent() { return currentDamageEvent; }
        public void setCurrentDamageEvent(DamageEvent event) { this.currentDamageEvent = event; }
        
        public boolean wasDamageModified() {
            return !originalDamageEvent.equals(currentDamageEvent);
        }
        
        public boolean wasDamageBlocked() {
            return currentDamageEvent.getDamage() <= 0;
        }
        
        public float getDamageReduction() {
            return originalDamageEvent.getDamage() - currentDamageEvent.getDamage();
        }
        
        public float getDamageReductionPercentage() {
            if (originalDamageEvent.getDamage() == 0) return 0;
            return (getDamageReduction() / originalDamageEvent.getDamage()) * 100;
        }
        
        public long getProcessingTime() {
            return System.currentTimeMillis() - processingStartTime;
        }
        
        public Map<DefenseLayerType, List<LayerProcessingResult>> getLayerResults() {
            return new HashMap<>(layerResults);
        }
        
        public record LayerProcessingResult(DefenseLayer layer, DefenseLayerResult result) {}
    }
    
    /**
     * Типы слоев защиты
     */
    public enum DefenseLayerType {
        IMMUNITY("Immunity", "Полный иммунитет к типу урона"),
        RESISTANCE("Resistance", "Процентное снижение урона"),
        ABSORPTION("Absorption", "Поглощение первых N единиц урона"),
        REFLECTION("Reflection", "Отражение урона обратно к атакующему"),
        CONVERSION("Conversion", "Превращение урона в другой тип");
        
        private final String displayName;
        private final String description;
        
        DefenseLayerType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}