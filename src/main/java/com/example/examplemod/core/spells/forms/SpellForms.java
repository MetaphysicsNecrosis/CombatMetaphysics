package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.*;

/**
 * Полная реализация всех 9 форм заклинаний согласно Concept.txt
 * 
 * Каждая форма определяет геометрическо-пространственное проявление заклинания
 * с уникальными характеристиками поведения и взаимодействия
 */
public enum SpellForms {
    
    /**
     * PROJECTILE — снарядный вид
     * Может иметь вид копья, продолговатый, пули
     * Характеристики: линейное движение, коллизия с первой целью, может рикошетить
     */
    PROJECTILE("projectile") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new ProjectileGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new ProjectileBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "speed", "range", "pierce_count", "bounce_count", 
                     "homing_strength", "penetration" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.INSTANT_CAST;
        }
    },
    
    /**
     * BEAM — поддерживаемый луч
     * Непрерывный поток энергии от заклинателя к цели
     * Характеристики: требует поддержание маны, фиксированная длина
     */
    BEAM("beam") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new BeamGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new BeamBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "range", "tick_rate", "penetration", "geometry_size" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.MANA_SUSTAINED;
        }
    },
    
    /**
     * BARRIER — защитная структура (трёхмерная)
     * Может представлять купол, полукруг, круг с пропусками
     * Использует N-угольные фигуры для основания
     */
    BARRIER("barrier") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new BarrierGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new BarrierBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "durability", "radius", "duration", "geometry_size", "regeneration" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.MANA_SUSTAINED;
        }
    },
    
    /**
     * AREA — зона воздействия (двумерная)
     * Накладывается только в двух плоскостях (пол/потолок)
     * N-угольные формы, создает "зону" с эффектом
     */
    AREA("area") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new AreaGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new AreaBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "healing", "radius", "duration", "tick_rate", "growth_rate" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.INSTANT_CAST;
        }
    },
    
    /**
     * WAVE — расширяющаяся волна
     * В отличие от projectile, имеет широкую структуру с градусом изгиба
     * Характеристики: расширение во время движения, угол распространения
     */
    WAVE("wave") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new WaveGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new WaveBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "speed", "range", "spread_angle", "wave_bend", "growth_rate" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.INSTANT_CAST;
        }
    },
    
    /**
     * TOUCH — контактное воздействие
     * Накладывает на руку заклинателя способность, активируется по удару
     * Характеристики: требует прямого контакта, эффект на касание
     */
    TOUCH("touch") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new TouchGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new TouchBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "healing", "enchantment_duration", "amplify_factor" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.INSTANT_CAST;
        }
    },
    
    /**
     * WEAPON_ENCHANT — наложение на оружие/конечности
     * Временно усиливает оружие или конечности заклинателя
     * Характеристики: длительность действия, стакается с другими эффектами
     */
    WEAPON_ENCHANT("weapon_enchant") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new WeaponEnchantGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new WeaponEnchantBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "enchantment_duration", "amplify_factor", "crit_chance" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.INSTANT_CAST;
        }
    },
    
    /**
     * INSTANT_POINT — мгновенное проявление в точке
     * Эффект возникает немедленно в указанной точке без траектории
     * Характеристики: телепортация эффекта, задержка активации
     */
    INSTANT_POINT("instant_point") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new InstantPointGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new InstantPointBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "healing", "range", "radius", "instant_delay" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.INSTANT_CAST;
        }
    },
    
    /**
     * CHAIN — цепная реакция между целями
     * Эффект перескакивает от цели к цели с ослаблением
     * Характеристики: количество прыжков, радиус поиска следующей цели
     */
    CHAIN("chain") {
        @Override
        public SpellGeometry createGeometry(SpellFormContext context) {
            return new ChainGeometry(context);
        }
        
        @Override
        public SpellFormBehavior createBehavior() {
            return new ChainBehavior();
        }
        
        @Override
        public boolean supportsParameter(String parameter) {
            return switch (parameter) {
                case "damage", "healing", "range", "bounce_count", "amplify_factor" -> true;
                default -> false;
            };
        }
        
        @Override
        public CastMode getPreferredCastMode() {
            return CastMode.QTE_SUSTAINED;
        }
    };
    
    private final String registryName;
    
    SpellForms(String registryName) {
        this.registryName = registryName;
    }
    
    public String getRegistryName() {
        return registryName;
    }
    
    /**
     * Создает геометрию для данной формы заклинания
     */
    public abstract SpellGeometry createGeometry(SpellFormContext context);
    
    /**
     * Создает поведение для данной формы заклинания
     */
    public abstract SpellFormBehavior createBehavior();
    
    /**
     * Проверяет, поддерживает ли форма данный параметр
     */
    public abstract boolean supportsParameter(String parameter);
    
    /**
     * Возвращает предпочтительный режим каста для формы
     */
    public abstract CastMode getPreferredCastMode();
    
    /**
     * Найти форму по имени
     */
    public static SpellForms fromString(String name) {
        for (SpellForms form : values()) {
            if (form.registryName.equalsIgnoreCase(name) || 
                form.name().equalsIgnoreCase(name)) {
                return form;
            }
        }
        return PROJECTILE; // Fallback
    }
    
    /**
     * Режимы каста согласно концепту
     */
    public enum CastMode {
        INSTANT_CAST,        // Мгновенный каст
        MANA_SUSTAINED,      // Поддерживаемый маной
        QTE_SUSTAINED        // Поддерживаемый с QTE
    }
}