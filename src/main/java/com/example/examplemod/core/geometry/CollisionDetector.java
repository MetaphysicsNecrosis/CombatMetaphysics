package com.example.examplemod.core.geometry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.example.examplemod.core.spells.forms.PersistenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Детектор коллизий для заклинаний
 * Обрабатывает пересечения с учётом типов проходимости
 */
public class CollisionDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollisionDetector.class);

    /**
     * Найти все сущности, пересекающиеся с формой заклинания
     */
    public static List<Entity> findIntersectingEntities(Level level, SpellShape shape, PersistenceType persistenceType) {
        AABB searchArea = shape.getBoundingBox().inflate(1.0); // Небольшое расширение для оптимизации
        List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class, searchArea);
        
        List<Entity> intersecting = new ArrayList<>();
        
        for (Entity entity : nearbyEntities) {
            if (shouldInteract(entity, persistenceType) && intersectsWithEntity(shape, entity)) {
                intersecting.add(entity);
            }
        }
        
        return intersecting;
    }

    /**
     * Найти живые сущности в области заклинания
     */
    public static List<LivingEntity> findIntersectingLivingEntities(Level level, SpellShape shape, PersistenceType persistenceType) {
        return findIntersectingEntities(level, shape, persistenceType).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .collect(Collectors.toList());
    }

    /**
     * Проверить пересечение формы с конкретной сущностью
     */
    private static boolean intersectsWithEntity(SpellShape shape, Entity entity) {
        AABB entityBounds = entity.getBoundingBox();
        
        // Быстрая проверка AABB
        if (!shape.getBoundingBox().intersects(entityBounds)) {
            return false;
        }
        
        // Детальная проверка с формой
        return shape.intersects(entityBounds);
    }

    /**
     * Определить должно ли заклинание взаимодействовать с сущностью
     * на основе типа проходимости
     */
    private static boolean shouldInteract(Entity entity, PersistenceType persistenceType) {
        boolean isLiving = entity instanceof LivingEntity;
        
        return switch (persistenceType) {
            case GHOST -> isLiving || isMagicalEntity(entity);
            case PHANTOM -> !isLiving || isMagicalEntity(entity);
            case PHYSICAL -> true;
        };
    }

    /**
     * Проверить является ли сущность магической
     * (для будущего расширения системы)
     */
    private static boolean isMagicalEntity(Entity entity) {
        // TODO: Implement magical entity detection
        // Например, проверка тегов, компонентов или типов сущностей
        return false;
    }

    /**
     * Результат детекции коллизий
     */
    public static class CollisionResult {
        private final List<Entity> entities;
        private final List<Vec3> impactPoints;
        private final boolean hasCollisions;

        public CollisionResult(List<Entity> entities, List<Vec3> impactPoints) {
            this.entities = new ArrayList<>(entities);
            this.impactPoints = new ArrayList<>(impactPoints);
            this.hasCollisions = !entities.isEmpty();
        }

        public List<Entity> getEntities() {
            return new ArrayList<>(entities);
        }

        public List<LivingEntity> getLivingEntities() {
            return entities.stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> (LivingEntity) entity)
                    .collect(Collectors.toList());
        }

        public List<Vec3> getImpactPoints() {
            return new ArrayList<>(impactPoints);
        }

        public boolean hasCollisions() {
            return hasCollisions;
        }

        public int getEntityCount() {
            return entities.size();
        }
    }

    /**
     * Выполнить полную детекцию коллизий с подробным результатом
     */
    public static CollisionResult detectCollisions(Level level, SpellShape shape, PersistenceType persistenceType) {
        List<Entity> intersectingEntities = findIntersectingEntities(level, shape, persistenceType);
        List<Vec3> impactPoints = new ArrayList<>();

        // Для каждой сущности найти точку столкновения
        for (Entity entity : intersectingEntities) {
            Vec3 impactPoint = findImpactPoint(shape, entity);
            if (impactPoint != null) {
                impactPoints.add(impactPoint);
            }
        }

        return new CollisionResult(intersectingEntities, impactPoints);
    }

    /**
     * Найти точку столкновения заклинания с сущностью
     */
    private static Vec3 findImpactPoint(SpellShape shape, Entity entity) {
        Vec3 shapeCenter = shape.getCenter();
        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2, 0);
        
        // Простое приближение - точка на линии между центрами
        // TODO: Implement more precise impact point calculation
        return shapeCenter.add(entityCenter).scale(0.5);
    }

    /**
     * Проверить пересечение двух заклинаний
     */
    public static boolean checkSpellIntersection(SpellShape shape1, SpellShape shape2) {
        return shape1.intersects(shape2);
    }

    /**
     * Найти область пересечения двух форм заклинаний
     */
    public static AABB findIntersectionArea(SpellShape shape1, SpellShape shape2) {
        AABB bounds1 = shape1.getBoundingBox();
        AABB bounds2 = shape2.getBoundingBox();
        
        if (!bounds1.intersects(bounds2)) {
            return null;
        }
        
        // Найти пересекающийся AABB
        double minX = Math.max(bounds1.minX, bounds2.minX);
        double minY = Math.max(bounds1.minY, bounds2.minY);
        double minZ = Math.max(bounds1.minZ, bounds2.minZ);
        double maxX = Math.min(bounds1.maxX, bounds2.maxX);
        double maxY = Math.min(bounds1.maxY, bounds2.maxY);
        double maxZ = Math.min(bounds1.maxZ, bounds2.maxZ);
        
        if (minX < maxX && minY < maxY && minZ < maxZ) {
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        
        return null;
    }
}