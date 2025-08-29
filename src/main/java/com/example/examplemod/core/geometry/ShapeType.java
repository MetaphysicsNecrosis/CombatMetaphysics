package com.example.examplemod.core.geometry;

/**
 * Типы геометрических форм для хитбоксов заклинаний
 */
public enum ShapeType {
    
    // Базовые формы
    SPHERE("sphere", "Сферическая форма"),
    BOX("box", "Прямоугольная форма"),
    CYLINDER("cylinder", "Цилиндрическая форма"),
    CONE("cone", "Коническая форма"),
    
    // Специализированные формы для заклинаний
    RAY("ray", "Луч (для BEAM заклинаний)"),
    PROJECTILE_PATH("projectile_path", "Траектория снаряда"),
    WAVE_RING("wave_ring", "Кольцевая волна"),
    DOME("dome", "Купол (полусфера)"),
    PLANE("plane", "Плоскость (для AREA заклинаний)"),
    CHAIN_LINK("chain_link", "Связь в цепочке"),
    
    // Сложные составные формы
    COMPOUND("compound", "Составная форма из нескольких базовых"),
    CUSTOM("custom", "Пользовательская форма");

    private final String id;
    private final String description;

    ShapeType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static ShapeType fromId(String id) {
        for (ShapeType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown shape type: " + id);
    }

    /**
     * Проверить совместимость формы с типом заклинания
     */
    public boolean isCompatibleWithSpellForm(String spellFormType) {
        return switch (spellFormType.toLowerCase()) {
            case "projectile" -> this == SPHERE || this == BOX || this == PROJECTILE_PATH;
            case "beam" -> this == RAY || this == CYLINDER;
            case "barrier" -> this == BOX || this == DOME || this == SPHERE || this == CYLINDER;
            case "area" -> this == PLANE || this == CYLINDER || this == SPHERE;
            case "wave" -> this == WAVE_RING || this == SPHERE || this == CYLINDER;
            case "touch", "weapon_enchant" -> this == SPHERE || this == BOX;
            case "instant_point" -> this == SPHERE || this == BOX;
            case "chain" -> this == CHAIN_LINK || this == RAY;
            default -> this == CUSTOM || this == COMPOUND;
        };
    }
}