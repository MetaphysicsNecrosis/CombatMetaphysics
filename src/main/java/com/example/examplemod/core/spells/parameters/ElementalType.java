package com.example.examplemod.core.spells.parameters;

/**
 * Элементальные типы магии с системой антагонизмов
 * Каждый элемент имеет коэффициенты взаимодействия с другими элементами
 */
public enum ElementalType {
    FIRE("fire"),
    ICE("ice"), 
    LIGHTNING("lightning"),
    WATER("water"),
    EARTH("earth"),
    NONE("none"); // Нейтральная магия
    
    private final String id;
    
    ElementalType(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    /**
     * Получить коэффициент влияния этого элемента на другой
     * Базовые антагонистические отношения из концепта
     */
    public float getInfluenceOn(ElementalType other) {
        return switch (this) {
            case FIRE -> switch (other) {
                case ICE -> 10.0f;      // Огонь сильно влияет на лёд
                case WATER -> 2.0f;     // Огонь слабо влияет на воду
                case EARTH -> 3.0f;     // Огонь умеренно влияет на землю
                case LIGHTNING -> 1.0f; // Нейтрально
                case FIRE -> 1.0f;      // На себя нейтрально
                case NONE -> 1.0f;
            };
            
            case ICE -> switch (other) {
                case FIRE -> 2.0f;      // Лёд слабо влияет на огонь
                case WATER -> 8.0f;     // Лёд сильно влияет на воду
                case EARTH -> 5.0f;     // Лёд умеренно влияет на землю
                case LIGHTNING -> 1.0f;
                case ICE -> 1.0f;
                case NONE -> 1.0f;
            };
            
            case WATER -> switch (other) {
                case FIRE -> 10.0f;     // Вода сильно влияет на огонь
                case ICE -> 2.0f;       // Вода слабо влияет на лёд
                case EARTH -> 6.0f;     // Вода размывает землю
                case LIGHTNING -> 8.0f; // Вода проводит электричество
                case WATER -> 1.0f;
                case NONE -> 1.0f;
            };
            
            case LIGHTNING -> switch (other) {
                case FIRE -> 1.0f;
                case ICE -> 4.0f;       // Молния трескает лёд
                case WATER -> 12.0f;    // Молния очень сильна против воды
                case EARTH -> 7.0f;     // Молния бьёт в землю
                case LIGHTNING -> 1.0f;
                case NONE -> 1.0f;
            };
            
            case EARTH -> switch (other) {
                case FIRE -> 1.5f;      // Земля слабо влияет на огонь
                case ICE -> 3.0f;       // Земля ломает лёд
                case WATER -> 2.0f;     // Земля поглощает воду
                case LIGHTNING -> 5.0f; // Земля заземляет молнии
                case EARTH -> 1.0f;
                case NONE -> 1.0f;
            };
            
            case NONE -> 1.0f; // Нейтральная магия не имеет бонусов
        };
    }
    
    /**
     * Проверить, являются ли элементы антагонистами (взаимно усиливающими эффект)
     */
    public boolean isAntagonistWith(ElementalType other) {
        float thisInfluence = this.getInfluenceOn(other);
        float otherInfluence = other.getInfluenceOn(this);
        
        // Антагонизм когда оба элемента сильно влияют друг на друга
        return thisInfluence > 5.0f && otherInfluence > 5.0f;
    }
    
    /**
     * Проверить, являются ли элементы синергичными (взаимно дополняющими)
     */
    public boolean isSynergicWith(ElementalType other) {
        float thisInfluence = this.getInfluenceOn(other);
        float otherInfluence = other.getInfluenceOn(this);
        
        // Синергия когда оба элемента умеренно влияют друг на друга
        return thisInfluence >= 3.0f && thisInfluence <= 5.0f && 
               otherInfluence >= 3.0f && otherInfluence <= 5.0f;
    }
    
    /**
     * Получить доминирующий элемент в паре
     */
    public ElementalType getDominantOver(ElementalType other) {
        float thisInfluence = this.getInfluenceOn(other);
        float otherInfluence = other.getInfluenceOn(this);
        
        if (thisInfluence > otherInfluence + 2.0f) {
            return this;
        } else if (otherInfluence > thisInfluence + 2.0f) {
            return other;
        } else {
            return NONE; // Равные или близкие к равным
        }
    }
}