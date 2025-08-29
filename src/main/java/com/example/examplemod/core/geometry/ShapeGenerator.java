package com.example.examplemod.core.geometry;

import com.example.examplemod.core.spells.SpellDefinition;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.forms.SpellFormType;
import com.example.examplemod.core.geometry.shapes.*;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Генератор геометрических форм для заклинаний
 * Создаёт SpellShape на основе определения заклинания
 */
public class ShapeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeGenerator.class);

    /**
     * Создать форму заклинания на основе его определения
     */
    public static SpellShape generateShape(SpellDefinition definition, Vec3 origin) {
        SpellFormType formType = definition.form();
        SpellParameters params = definition.parameters();
        
        try {
            return switch (formType) {
                case PROJECTILE -> generateProjectileShape(params, origin);
                case BEAM -> generateBeamShape(params, origin);
                case BARRIER -> generateBarrierShape(params, origin);
                case AREA -> generateAreaShape(params, origin);
                case WAVE -> generateWaveShape(params, origin);
                case TOUCH -> generateTouchShape(params, origin);
                case WEAPON_ENCHANT -> generateWeaponEnchantShape(params, origin);
                case INSTANT_POINT -> generateInstantPointShape(params, origin);
                case CHAIN -> generateChainShape(params, origin);
            };
        } catch (Exception e) {
            LOGGER.error("Failed to generate shape for spell form {}", formType, e);
            // Возвращаем базовую сферу как fallback
            return new SphereShape(origin, 1.0);
        }
    }

    private static SpellShape generateProjectileShape(SpellParameters params, Vec3 origin) {
        // Для снарядов используем сферу с радиусом, зависящим от размера
        double radius = params.getFloat(SpellParameters.GEOMETRY_SIZE, 0.5f);
        return new SphereShape(origin, radius);
    }

    private static SpellShape generateBeamShape(SpellParameters params, Vec3 origin) {
        // Луч представляем как цилиндр
        double range = params.getFloat(SpellParameters.RANGE, 10f);
        double thickness = params.getFloat(SpellParameters.RADIUS, 0.5f);
        
        // TODO: Implement CylinderShape
        // Пока используем сферу
        return new SphereShape(origin, thickness);
    }

    private static SpellShape generateBarrierShape(SpellParameters params, Vec3 origin) {
        // Барьеры могут быть различных форм
        double radius = params.getFloat(SpellParameters.RADIUS, 3f);
        String barrierType = params.getString("barrier_type", "dome");
        
        return switch (barrierType.toLowerCase()) {
            case "sphere" -> new SphereShape(origin, radius);
            case "dome" -> {
                // TODO: Implement DomeShape
                yield new SphereShape(origin, radius);
            }
            default -> new SphereShape(origin, radius);
        };
    }

    private static SpellShape generateAreaShape(SpellParameters params, Vec3 origin) {
        // Области воздействия - обычно круги на поверхности
        double radius = params.getFloat(SpellParameters.RADIUS, 2f);
        double height = params.getFloat("area_height", 0.5f); // Небольшая высота для детекции
        
        // TODO: Implement CylinderShape for flat areas
        return new SphereShape(origin, radius);
    }

    private static SpellShape generateWaveShape(SpellParameters params, Vec3 origin) {
        // Волны - расширяющиеся кольца
        double currentRadius = params.getFloat("current_wave_radius", 1f);
        double thickness = params.getFloat("wave_thickness", 0.5f);
        
        // TODO: Implement WaveRingShape
        return new SphereShape(origin, currentRadius);
    }

    private static SpellShape generateTouchShape(SpellParameters params, Vec3 origin) {
        // Контактные заклинания - небольшая сфера вокруг руки
        double radius = params.getFloat(SpellParameters.RADIUS, 0.8f);
        return new SphereShape(origin, radius);
    }

    private static SpellShape generateWeaponEnchantShape(SpellParameters params, Vec3 origin) {
        // Зачарования оружия - форма зависит от оружия
        double size = params.getFloat(SpellParameters.GEOMETRY_SIZE, 1.2f);
        return new SphereShape(origin, size);
    }

    private static SpellShape generateInstantPointShape(SpellParameters params, Vec3 origin) {
        // Мгновенные заклинания - сфера взрыва
        double radius = params.getFloat(SpellParameters.RADIUS, 2f);
        return new SphereShape(origin, radius);
    }

    private static SpellShape generateChainShape(SpellParameters params, Vec3 origin) {
        // Цепные заклинания - линия между точками
        double thickness = params.getFloat("chain_thickness", 0.3f);
        
        // TODO: Implement ChainLinkShape
        return new SphereShape(origin, thickness);
    }

    /**
     * Обновить форму заклинания (для анимированных форм)
     */
    public static SpellShape updateShape(SpellShape currentShape, SpellDefinition definition, long age) {
        SpellFormType formType = definition.form();
        SpellParameters params = definition.parameters();
        
        return switch (formType) {
            case WAVE -> updateWaveShape(currentShape, params, age);
            case BEAM -> updateBeamShape(currentShape, params, age);
            case AREA -> updateAreaShape(currentShape, params, age);
            default -> currentShape; // Статические формы не изменяются
        };
    }

    private static SpellShape updateWaveShape(SpellShape currentShape, SpellParameters params, long age) {
        if (!(currentShape instanceof SphereShape sphere)) return currentShape;
        
        double growthRate = params.getFloat(SpellParameters.GROWTH_RATE, 1f);
        double ageInSeconds = age / 1000.0;
        double newRadius = sphere.getRadius() + (growthRate * ageInSeconds);
        
        double maxRadius = params.getFloat("max_wave_radius", 20f);
        newRadius = Math.min(newRadius, maxRadius);
        
        return new SphereShape(sphere.getCenter(), newRadius);
    }

    private static SpellShape updateBeamShape(SpellShape currentShape, SpellParameters params, long age) {
        // Лучи могут изменять длину или толщину
        return currentShape;
    }

    private static SpellShape updateAreaShape(SpellShape currentShape, SpellParameters params, long age) {
        // Области могут расширяться или сжиматься
        return currentShape;
    }
}