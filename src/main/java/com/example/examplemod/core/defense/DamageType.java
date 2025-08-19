package com.example.examplemod.core.defense;

/**
 * Типы урона для Layered Defense Model
 */
public enum DamageType {
    // Базовые типы урона
    GENERIC("Generic", "Обычный урон"),
    PHYSICAL("Physical", "Физический урон"),
    MAGICAL("Magical", "Магический урон"),
    
    // Физические подтипы
    SLASHING("Slashing", "Режущий урон"),
    PIERCING("Piercing", "Колющий урон"),
    BLUDGEONING("Bludgeoning", "Дробящий урон"),
    
    // Магические подтипы
    FIRE("Fire", "Огненный урон"),
    ICE("Ice", "Ледяной урон"),
    LIGHTNING("Lightning", "Урон молнией"),
    ARCANE("Arcane", "Тайная магия"),
    DARK("Dark", "Темная магия"),
    HOLY("Holy", "Светлая магия"),
    
    // Особые типы
    POISON("Poison", "Урон ядом"),
    DISEASE("Disease", "Урон болезнью"),
    PSYCHIC("Psychic", "Ментальный урон"),
    FORCE("Force", "Силовое воздействие"),
    
    // Экологические
    FALL("Fall", "Урон от падения"),
    SUFFOCATION("Suffocation", "Урон от удушья"),
    DROWNING("Drowning", "Урон от утопления"),
    LAVA("Lava", "Урон от лавы"),
    
    // Мета-тип для универсальной защиты
    ALL("All", "Все типы урона");
    
    private final String displayName;
    private final String description;
    
    DamageType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Проверяет, является ли данный тип физическим
     */
    public boolean isPhysical() {
        return switch (this) {
            case PHYSICAL, SLASHING, PIERCING, BLUDGEONING -> true;
            default -> false;
        };
    }
    
    /**
     * Проверяет, является ли данный тип магическим
     */
    public boolean isMagical() {
        return switch (this) {
            case MAGICAL, FIRE, ICE, LIGHTNING, ARCANE, DARK, HOLY -> true;
            default -> false;
        };
    }
    
    /**
     * Проверяет, является ли данный тип элементальным
     */
    public boolean isElemental() {
        return switch (this) {
            case FIRE, ICE, LIGHTNING -> true;
            default -> false;
        };
    }
    
    /**
     * Получает родительский тип урона
     */
    public DamageType getParentType() {
        return switch (this) {
            case SLASHING, PIERCING, BLUDGEONING -> PHYSICAL;
            case FIRE, ICE, LIGHTNING, ARCANE, DARK, HOLY -> MAGICAL;
            default -> GENERIC;
        };
    }
    
    /**
     * Проверяет совместимость с другим типом урона
     */
    public boolean isCompatibleWith(DamageType other) {
        if (this == other || other == ALL || this == ALL) {
            return true;
        }
        
        // Проверяем родительские типы
        return this.getParentType() == other || other.getParentType() == this;
    }
}