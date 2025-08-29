package com.example.examplemod.core.spells.parameters.behavioral;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

import java.util.Map;

/**
 * Параметр типа проходимости заклинания
 * Реализует 3 типа из Concept.txt:
 * 
 * PHYSICAL - полная физическая коллизия со всеми объектами
 * PHANTOM - проходит через живые сущности, взаимодействует с неживыми
 * GHOST - проходит через физические препятствия, взаимодействует только с живыми
 * 
 * Критически важно для коллизий и взаимодействий!
 */
public class PersistenceTypeParameter extends AbstractSpellParameter<String> {
    
    // Типы проходимости из Concept.txt
    public static final String PHYSICAL = "PHYSICAL";
    public static final String PHANTOM = "PHANTOM"; 
    public static final String GHOST = "GHOST";
    
    public PersistenceTypeParameter() {
        super("persistence_type", String.class);
    }
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        String requestedType = inputValue != null ? inputValue.toString() : PHYSICAL;
        
        // === ОПРЕДЕЛЕНИЕ ПОДХОДЯЩЕГО ТИПА ПРОХОДИМОСТИ ===
        String finalType = determinePersistenceType(context, requestedType);
        
        // === ПРИМЕНЕНИЕ ТИПА ПРОХОДИМОСТИ ===
        return switch (finalType) {
            case PHYSICAL -> computePhysicalType(context);
            case PHANTOM -> computePhantomType(context);
            case GHOST -> computeGhostType(context);
            default -> computePhysicalType(context); // Fallback
        };
    }
    
    /**
     * Определение подходящего типа на основе формы и элементов
     */
    private String determinePersistenceType(SpellComputationContext context, String requestedType) {
        
        String formType = context.getFormType();
        
        // === АВТОМАТИЧЕСКОЕ ОПРЕДЕЛЕНИЕ ПО ЭЛЕМЕНТАМ ===
        
        // Духовные элементы тяготеют к GHOST
        if (context.hasElementalIntensity("spirit") || 
            context.hasElementalIntensity("light") ||
            context.hasElementalIntensity("shadow")) {
            
            float spiritualIntensity = 
                context.getElementalIntensity("spirit") +
                context.getElementalIntensity("light") * 0.7f +
                context.getElementalIntensity("shadow") * 0.8f;
            
            if (spiritualIntensity >= 0.6f) {
                return GHOST; // Высокая духовная сила = проходит через материю
            }
        }
        
        // Энергетические элементы могут быть PHANTOM
        if (context.hasElementalIntensity("lightning") ||
            context.hasElementalIntensity("fire")) {
            
            float energyIntensity = 
                context.getElementalIntensity("lightning") * 1.2f +
                context.getElementalIntensity("fire") * 0.8f;
            
            if (energyIntensity >= 0.8f && !isPhysicalForm(formType)) {
                return PHANTOM; // Энергия проходит через живое, блокируется материей
            }
        }
        
        // === ОГРАНИЧЕНИЯ ПО ФОРМЕ ===
        
        // Некоторые формы всегда физические
        if (isAlwaysPhysical(formType)) {
            return PHYSICAL;
        }
        
        // Некоторые формы предпочитают нематериальность
        if (prefersIntangible(formType) && !PHYSICAL.equals(requestedType)) {
            return requestedType.equals(GHOST) ? GHOST : PHANTOM;
        }
        
        return requestedType; // Используем выбор игрока
    }
    
    /**
     * PHYSICAL - из Concept.txt: "полная физическая коллизия со всеми объектами"
     */
    private SpellComputationResult computePhysicalType(SpellComputationContext context) {
        
        return buildResult()
            .putValue("persistence_type", PHYSICAL)
            // === КОЛЛИЗИИ ===
            .putValue("collides_with_blocks", 1.0f)
            .putValue("collides_with_entities", 1.0f)
            .putValue("collides_with_liquids", 1.0f)
            .putValue("affected_by_gravity", 1.0f)
            
            // === ВЗАИМОДЕЙСТВИЯ ===
            .putValue("can_be_blocked", 1.0f) // Может быть заблокировано щитами
            .putValue("can_trigger_pressure_plates", 1.0f)
            .putValue("can_break_blocks", 1.0f) // При достаточной силе
            .putValue("can_push_entities", 1.0f)
            
            // === ПРОХОЖДЕНИЕ ЧЕРЕЗ МАГИЧЕСКИЕ БАРЬЕРЫ ===
            // "При прохождении через магический барьер использует Ghost-параметры"
            .putValue("uses_ghost_vs_magic", 1.0f)
            .putValue("ghost_penetration_chance", calculateGhostPenetration(context))
            .putValue("loses_ghost_after_barrier", 1.0f) // Теряет Ghost свойства после прохода
            
            // === ХАРАКТЕРИСТИКИ ===
            .putValue("collision_damage_multiplier", 1.2f) // Физические заклинания наносят больше урона при столкновении
            .putValue("durability_vs_physical", 1.0f)
            .putValue("durability_vs_magic", 0.8f) // Уязвимее к магическим атакам
            .build();
    }
    
    /**
     * PHANTOM - из Concept.txt: "проходит через живые сущности, взаимодействует с неживыми"
     */
    private SpellComputationResult computePhantomType(SpellComputationContext context) {
        
        return buildResult()
            .putValue("persistence_type", PHANTOM)
            // === КОЛЛИЗИИ ===
            .putValue("collides_with_blocks", 1.0f) // Взаимодействует с неживыми
            .putValue("collides_with_entities", 0.0f) // Проходит через живые сущности
            .putValue("collides_with_liquids", 1.0f) // Жидкости - неживые
            .putValue("affected_by_gravity", 0.5f) // Частично подвержен гравитации
            
            // === РАЗРУШЕНИЕ ПРЕПЯТСТВИЙ ===
            // "При достаточном уроне может разрушать препятствия и продолжать движение"
            .putValue("can_break_blocks", 1.0f)
            .putValue("break_threshold_multiplier", 0.8f) // Легче ломает блоки
            .putValue("continues_after_breaking", 1.0f) // Продолжает движение после разрушения
            
            // === ОСОБЫЕ ВЗАИМОДЕЙСТВИЯ ===
            .putValue("ignores_living_entities", 1.0f) // Основная особенность
            .putValue("can_affect_undead", 1.0f) // Нежить может считаться неживой
            .putValue("can_trigger_redstone", 1.0f) // Редстоун - неживое
            .putValue("can_activate_mechanisms", 1.0f)
            
            // === ХАРАКТЕРИСТИКИ ===
            .putValue("persistence_type", PHANTOM)
            .putValue("block_breaking_bonus", 0.3f)
            .putValue("magic_resistance", 0.2f) // Немного устойчив к магии
            .putValue("physical_vulnerability", 0.8f) // Уязвим к физическим атакам
            .build();
    }
    
    /**
     * GHOST - из Concept.txt: "проходит через физические препятствия, взаимодействует только с живыми"
     */
    private SpellComputationResult computeGhostType(SpellComputationContext context) {
        
        return buildResult()
            .putValue("persistence_type", GHOST)
            // === КОЛЛИЗИИ ===
            .putValue("collides_with_blocks", 0.0f) // Проходит через физические препятствия
            .putValue("collides_with_entities", 1.0f) // Взаимодействует только с живыми
            .putValue("collides_with_liquids", 0.0f) // Проходит через жидкости
            .putValue("affected_by_gravity", 0.0f) // Не подвержен гравитации
            
            // === МАГИЧЕСКИЕ ВЗАИМОДЕЙСТВИЯ ===
            // "Может быть остановлен высоким магическим сопротивлением или магическими барьерами"
            .putValue("blocked_by_magic_resistance", 1.0f)
            .putValue("magic_resistance_threshold", 0.7f) // Высокое сопротивление останавливает
            .putValue("blocked_by_magic_barriers", 1.0f)
            .putValue("barrier_interaction_roll", 1.0f) // Проверка взаимодействия с барьерами
            
            // === ЖИВЫЕ ЦЕЛИ ===
            .putValue("affects_only_living", 1.0f) // Основная особенность
            .putValue("enhanced_vs_living", 1.0f) // Усиленное воздействие на живое
            .putValue("ignores_armor", 0.5f) // Частично игнорирует физическую броню
            .putValue("spiritual_damage_bonus", 0.3f) // Бонус духовного урона
            
            // === ОГРАНИЧЕНИЯ ===
            .putValue("no_block_interaction", 1.0f) // Не может активировать механизмы
            .putValue("no_item_pickup", 1.0f) // Не может поднимать предметы
            .putValue("no_redstone_activation", 1.0f) // Не активирует редстоун
            
            // === ХАРАКТЕРИСТИКИ ===
            .putValue("persistence_type", GHOST)
            .putValue("magic_damage_multiplier", 1.3f) // Больше магического урона
            .putValue("physical_immunity", 0.4f) // Частичный иммунитет к физическому урону
            .putValue("dispel_vulnerability", 1.5f) // Уязвим к развеиванию
            .build();
    }
    
    /**
     * Расчет шанса прохождения через магические барьеры для физических заклинаний
     */
    private float calculateGhostPenetration(SpellComputationContext context) {
        
        float basePenetration = 0.1f; // 10% базовый шанс
        
        // Духовные элементы увеличивают шанс
        if (context.hasElementalIntensity("spirit")) {
            basePenetration += context.getElementalIntensity("spirit") * 0.4f;
        }
        
        if (context.hasElementalIntensity("light")) {
            basePenetration += context.getElementalIntensity("light") * 0.2f;
        }
        
        // Высокий урон может "пробить" барьер
        float damage = context.getFloat("damage", 0);
        if (damage > 50) {
            basePenetration += (damage - 50) * 0.005f;
        }
        
        return Math.min(0.8f, basePenetration); // Максимум 80%
    }
    
    /**
     * Формы, которые всегда физические
     */
    private boolean isAlwaysPhysical(String formType) {
        return switch (formType) {
            case "BARRIER" -> true; // Барьеры должны блокировать
            case "AREA" -> false; // Зоны могут быть любыми
            default -> false;
        };
    }
    
    /**
     * Формы, которые тяготеют к физичности
     */
    private boolean isPhysicalForm(String formType) {
        return switch (formType) {
            case "PROJECTILE", "BARRIER" -> true;
            case "BEAM", "CHAIN", "INSTANT_POINT" -> false;
            default -> false;
        };
    }
    
    /**
     * Формы, которые предпочитают нематериальность
     */
    private boolean prefersIntangible(String formType) {
        return switch (formType) {
            case "BEAM", "CHAIN" -> true; // Лучи и цепи часто духовные
            case "TOUCH", "WEAPON_ENCHANT" -> true; // Энчанты нематериальны
            default -> false;
        };
    }
}