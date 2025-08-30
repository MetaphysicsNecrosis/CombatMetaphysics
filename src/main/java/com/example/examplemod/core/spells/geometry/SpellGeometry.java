package com.example.examplemod.core.spells.geometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Базовый класс для геометрии заклинаний
 * Определяет коллайдеры и пространственные характеристики заклинания
 */
public abstract class SpellGeometry {
    
    protected Vec3 origin;
    protected Vec3 direction;
    protected float size;
    protected GeometryType geometryType;
    
    public SpellGeometry(Vec3 origin, Vec3 direction, float size, GeometryType geometryType) {
        this.origin = origin;
        this.direction = direction.normalize();
        this.size = size;
        this.geometryType = geometryType;
    }
    
    /**
     * Получить основной ограничивающий прямоугольник (AABB)
     */
    public abstract AABB getBoundingBox();
    
    /**
     * Получить точный коллайдер для проверки пересечений
     */
    public abstract VoxelShape getCollisionShape();
    
    /**
     * Получить список точек для проверки коллизий
     */
    public abstract List<Vec3> getCollisionPoints();
    
    /**
     * Проверить пересечение с другой геометрией
     */
    public abstract CollisionResult checkCollision(SpellGeometry other);
    
    /**
     * Проверить пересечение с точкой
     */
    public abstract boolean intersects(Vec3 point);
    
    /**
     * Проверить пересечение с AABB
     */
    public abstract boolean intersects(AABB bounds);
    
    /**
     * Обновить геометрию (для движущихся заклинаний)
     */
    public abstract void update(Vec3 newPosition, Vec3 newDirection);
    
    /**
     * Получить объем геометрии для расчета маны
     */
    public abstract double getVolume();
    
    /**
     * Получить площадь поверхности для расчета урона
     */
    public abstract double getSurfaceArea();
    
    /**
     * Получить центр геометрии
     */
    public Vec3 getCenter() {
        return origin;
    }
    
    /**
     * Получить тип геометрии
     */
    public GeometryType getGeometryType() {
        return geometryType;
    }
    
    /**
     * Получить размер
     */
    public float getSize() {
        return size;
    }
    
    /**
     * Изменить размер геометрии
     */
    public void setSize(float newSize) {
        this.size = newSize;
    }
    
    /**
     * Типы геометрии согласно концепту
     */
    public enum GeometryType {
        // Для PROJECTILE
        SPHERE,          // Сферический снаряд
        CYLINDER,        // Копье, стрела
        CONE,            // Остроконечный снаряд
        
        // Для BEAM
        RAY,             // Тонкий луч
        THICK_BEAM,      // Толстый луч
        
        // Для BARRIER
        DOME,            // Купол
        WALL,            // Стена
        CIRCLE,          // Круг
        POLYGON,         // N-угольник
        
        // Для AREA
        CIRCLE_2D,       // Круглая зона
        SQUARE_2D,       // Квадратная зона
        POLYGON_2D,      // N-угольная зона
        
        // Для WAVE
        EXPANDING_CIRCLE, // Расширяющийся круг
        CONE_WAVE,       // Волна-конус
        
        // Для остальных форм
        POINT,           // Точка (INSTANT_POINT)
        TOUCH_AREA,      // Область касания (TOUCH)
        WEAPON_BOUNDS,   // Границы оружия (WEAPON_ENCHANT)
        CHAIN_LINE       // Цепочка между целями (CHAIN)
    }
    
    /**
     * Результат проверки коллизии
     */
    public record CollisionResult(
        boolean hasCollision,
        Vec3 collisionPoint,
        Vec3 collisionNormal,
        double penetrationDepth,
        CollisionType type
    ) {
        
        public enum CollisionType {
            SURFACE_CONTACT,    // Касание поверхности
            PARTIAL_OVERLAP,    // Частичное перекрытие
            FULL_CONTAINMENT,   // Полное вложение
            EDGE_INTERSECTION   // Пересечение граней
        }
        
        public static CollisionResult noCollision() {
            return new CollisionResult(false, Vec3.ZERO, Vec3.ZERO, 0.0, CollisionType.SURFACE_CONTACT);
        }
        
        public static CollisionResult collision(Vec3 point, Vec3 normal, double depth, CollisionType type) {
            return new CollisionResult(true, point, normal, depth, type);
        }
    }
}