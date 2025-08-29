package com.example.examplemod.core.geometry;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.List;

/**
 * Базовый интерфейс для геометрических форм заклинаний
 * Определяет пространственное представление заклинания
 */
public interface SpellShape {
    
    /**
     * Получить тип геометрической формы
     */
    ShapeType getType();
    
    /**
     * Получить ограничивающий прямоугольник (AABB)
     */
    AABB getBoundingBox();
    
    /**
     * Проверить пересечение с точкой
     */
    boolean intersects(Vec3 point);
    
    /**
     * Проверить пересечение с другой формой
     */
    boolean intersects(SpellShape other);
    
    /**
     * Проверить пересечение с AABB
     */
    boolean intersects(AABB boundingBox);
    
    /**
     * Получить все точки формы для детальной проверки коллизий
     */
    List<Vec3> getCollisionPoints();
    
    /**
     * Получить центр формы
     */
    Vec3 getCenter();
    
    /**
     * Получить объём формы (для расчёта воздействия)
     */
    double getVolume();
    
    /**
     * Трансформировать форму (перемещение, поворот, масштабирование)
     */
    SpellShape transform(Vec3 translation, Vec3 rotation, double scale);
    
    /**
     * Создать копию формы
     */
    SpellShape copy();
    
    /**
     * Проверить валидность формы
     */
    boolean isValid();
}