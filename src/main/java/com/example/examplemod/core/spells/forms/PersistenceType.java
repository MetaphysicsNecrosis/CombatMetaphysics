package com.example.examplemod.core.spells.forms;

/**
 * Типы проходимости заклинаний согласно Concept.txt
 */
public enum PersistenceType {
    
    /**
     * Проходит через физические препятствия, взаимодействует только с живыми сущностями 
     * и магическими объектами
     */
    GHOST("ghost", "Проходит через физические препятствия"),
    
    /**
     * Проходит через живые сущности, взаимодействует с неживыми объектами
     */
    PHANTOM("phantom", "Проходит через живые сущности"),
    
    /**
     * Полная физическая коллизия со всеми объектами
     */
    PHYSICAL("physical", "Полная физическая коллизия");

    private final String id;
    private final String description;

    PersistenceType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static PersistenceType fromId(String id) {
        for (PersistenceType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown persistence type: " + id);
    }
}