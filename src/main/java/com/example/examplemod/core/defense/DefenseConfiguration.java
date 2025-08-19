package com.example.examplemod.core.defense;

import java.util.*;

/**
 * Конфигурация защитных слоев для сущности
 */
public class DefenseConfiguration {
    private final Map<LayeredDefenseSystem.DefenseLayerType, List<DefenseLayer>> layers = new HashMap<>();
    
    public DefenseConfiguration() {
        // Инициализируем пустые списки для каждого типа слоя
        for (LayeredDefenseSystem.DefenseLayerType type : LayeredDefenseSystem.DefenseLayerType.values()) {
            layers.put(type, new ArrayList<>());
        }
    }
    
    /**
     * Добавляет слой защиты
     */
    public void addLayer(DefenseLayer layer) {
        layers.get(layer.getLayerType()).add(layer);
        // Сортируем по приоритету
        layers.get(layer.getLayerType()).sort(Comparator.comparingInt(DefenseLayer::getPriority).reversed());
    }
    
    /**
     * Удаляет слой защиты
     */
    public boolean removeLayer(DefenseLayer layer) {
        return layers.get(layer.getLayerType()).remove(layer);
    }
    
    /**
     * Получает все слои определенного типа
     */
    public List<DefenseLayer> getLayersOfType(LayeredDefenseSystem.DefenseLayerType type) {
        return new ArrayList<>(layers.get(type));
    }
    
    /**
     * Получает общее количество слоев
     */
    public int getTotalLayerCount() {
        return layers.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Проверяет, есть ли слои определенного типа
     */
    public boolean hasLayersOfType(LayeredDefenseSystem.DefenseLayerType type) {
        return !layers.get(type).isEmpty();
    }
    
    /**
     * Очищает все слои
     */
    public void clearAllLayers() {
        layers.values().forEach(List::clear);
    }
    
    /**
     * Очищает слои определенного типа
     */
    public void clearLayersOfType(LayeredDefenseSystem.DefenseLayerType type) {
        layers.get(type).clear();
    }
    
    /**
     * Получает все активные слои
     */
    public List<DefenseLayer> getActiveLayers() {
        List<DefenseLayer> activeLayers = new ArrayList<>();
        for (List<DefenseLayer> layerList : layers.values()) {
            layerList.stream()
                    .filter(DefenseLayer::isActive)
                    .forEach(activeLayers::add);
        }
        return activeLayers;
    }
    
    /**
     * Debug информация
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalLayers", getTotalLayerCount());
        info.put("activeLayers", getActiveLayers().size());
        
        Map<String, Integer> layerCounts = new HashMap<>();
        for (LayeredDefenseSystem.DefenseLayerType type : LayeredDefenseSystem.DefenseLayerType.values()) {
            layerCounts.put(type.name(), layers.get(type).size());
        }
        info.put("layerCounts", layerCounts);
        
        return info;
    }
}