package com.example.examplemod.core.spells.forms;

/**
 * Типы форм заклинаний согласно Concept.txt
 */
public enum SpellFormType {
    PROJECTILE("projectile", "Снарядный вид заклинания"),
    BEAM("beam", "Поддерживаемый луч"),
    BARRIER("barrier", "Защитная трёхмерная структура"),
    AREA("area", "Зона воздействия (двумерная)"),
    WAVE("wave", "Расширяющаяся волна"),
    TOUCH("touch", "Контактное воздействие"),
    WEAPON_ENCHANT("weapon_enchant", "Наложение на оружие/конечности"),
    INSTANT_POINT("instant_point", "Мгновенное проявление в точке"),
    CHAIN("chain", "Цепная реакция между целями");

    private final String id;
    private final String description;

    SpellFormType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static SpellFormType fromId(String id) {
        for (SpellFormType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown spell form type: " + id);
    }
}