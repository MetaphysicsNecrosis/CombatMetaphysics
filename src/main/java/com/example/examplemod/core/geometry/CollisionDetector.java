package com.example.examplemod.core.geometry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.example.examplemod.core.spells.forms.PersistenceType;
import com.example.examplemod.core.spells.collision.CollisionSnapshot;
import com.example.examplemod.core.spells.collision.CollisionSnapshot.EntitySnapshot;
import com.example.examplemod.core.spells.collision.CollisionSnapshot.BlockSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collection;

/**
 * Детектор коллизий для заклинаний
 * Обрабатывает пересечения с учётом типов проходимости
 * 
 * THREAD-SAFE: использует снепшоты вместо прямых ссылок на Minecraft объекты
 */
public class CollisionDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollisionDetector.class);

    // === THREAD-SAFE МЕТОДЫ (работают со снепшотами) ===
    
    /**
     * Найти все сущности из снепшота, пересекающиеся с формой заклинания
     * THREAD-SAFE: может вызываться из Collision Thread
     */
    public static List<EntitySnapshot> findIntersectingEntities(CollisionSnapshot snapshot, SpellShape shape, PersistenceType persistenceType) {
        AABB spellBounds = shape.getBoundingBox();
        Collection<EntitySnapshot> nearbyEntities = snapshot.getAllEntities();
        
        List<EntitySnapshot> intersecting = new ArrayList<>();
        
        for (EntitySnapshot entitySnapshot : nearbyEntities) {
            if (shouldInteract(entitySnapshot, persistenceType) && intersectsWithEntity(shape, entitySnapshot)) {
                intersecting.add(entitySnapshot);
            }
        }
        
        return intersecting;
    }

    // === LEGACY МЕТОДЫ (только для Main Thread) ===
    
    /**
     * Найти все сущности, пересекающиеся с формой заклинания
     * LEGACY: используй findIntersectingEntities(CollisionSnapshot, ...) вместо этого
     * @deprecated Используй thread-safe версию со снепшотами
     */
    @Deprecated
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
     * Найти живые сущности из снепшота в области заклинания
     * THREAD-SAFE: может вызываться из Collision Thread
     */
    public static List<EntitySnapshot> findIntersectingLivingEntities(CollisionSnapshot snapshot, SpellShape shape, PersistenceType persistenceType) {
        return findIntersectingEntities(snapshot, shape, persistenceType).stream()
                .filter(EntitySnapshot::isLiving)
                .collect(Collectors.toList());
    }

    /**
     * Найти живые сущности в области заклинания
     * @deprecated Используй thread-safe версию со снепшотами
     */
    @Deprecated
    public static List<LivingEntity> findIntersectingLivingEntities(Level level, SpellShape shape, PersistenceType persistenceType) {
        return findIntersectingEntities(level, shape, persistenceType).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .collect(Collectors.toList());
    }

    // === THREAD-SAFE ПРОВЕРКИ ПЕРЕСЕЧЕНИЙ ===
    
    /**
     * Проверить пересечение формы с EntitySnapshot
     * THREAD-SAFE: работает только с данными снепшота
     */
    private static boolean intersectsWithEntity(SpellShape shape, EntitySnapshot entitySnapshot) {
        AABB entityBounds = entitySnapshot.getBoundingBox();
        
        // Быстрая проверка AABB
        if (!shape.getBoundingBox().intersects(entityBounds)) {
            return false;
        }
        
        // Детальная проверка с формой
        return shape.intersects(entityBounds);
    }

    /**
     * Проверить пересечение формы с конкретной сущностью
     * LEGACY: для Main Thread только
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

    // === ЛОГИКА ВЗАИМОДЕЙСТВИЙ ===
    
    /**
     * Определить должно ли заклинание взаимодействовать с EntitySnapshot
     * на основе типа проходимости - THREAD-SAFE
     */
    private static boolean shouldInteract(EntitySnapshot entitySnapshot, PersistenceType persistenceType) {
        boolean isLiving = entitySnapshot.isLiving();
        
        return switch (persistenceType) {
            case GHOST -> isLiving || isMagicalEntity(entitySnapshot);
            case PHANTOM -> !isLiving || isMagicalEntity(entitySnapshot);
            case PHYSICAL -> true;
        };
    }

    /**
     * Определить должно ли заклинание взаимодействовать с сущностью
     * на основе типа проходимости - LEGACY
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
     * Проверить является ли EntitySnapshot магической
     * THREAD-SAFE: использует только данные снепшота
     */
    private static boolean isMagicalEntity(EntitySnapshot entitySnapshot) {
        String entityType = entitySnapshot.getEntityType();
        // TODO: Implement magical entity detection based on entity type string
        // Например, проверка типов заклинаний, магических существ и т.д.
        return entityType.contains("spell") || 
               entityType.contains("magic") || 
               entityType.contains("elemental");
    }

    /**
     * Проверить является ли сущность магической
     * (для будущего расширения системы) - LEGACY
     */
    private static boolean isMagicalEntity(Entity entity) {
        // TODO: Implement magical entity detection
        // Например, проверка тагов, компонентов или типов сущностей
        return false;
    }

    /**
     * Результат детекции коллизий - поддерживает как Entity, так и EntitySnapshot
     */
    public static class CollisionResult {
        // Thread-safe данные со снепшотами
        private final List<EntitySnapshot> entitySnapshots;
        
        // Legacy данные для совместимости
        private final List<Entity> entities;
        private final List<Vec3> impactPoints;
        private final boolean hasCollisions;

        // Конструктор для thread-safe результата (EntitySnapshot)
        public CollisionResult(List<EntitySnapshot> entitySnapshots, List<Vec3> impactPoints) {
            this.entitySnapshots = new ArrayList<>(entitySnapshots);
            this.entities = new ArrayList<>(); // Пустой для thread-safe версии
            this.impactPoints = new ArrayList<>(impactPoints);
            this.hasCollisions = !entitySnapshots.isEmpty();
        }
        
        // Legacy конструктор для совместимости (Entity)
        @Deprecated
        public CollisionResult(List<Entity> entities, List<Vec3> impactPoints, boolean legacy) {
            this.entitySnapshots = new ArrayList<>(); // Пустой для legacy версии
            this.entities = new ArrayList<>(entities);
            this.impactPoints = new ArrayList<>(impactPoints);
            this.hasCollisions = !entities.isEmpty();
        }

        // Thread-safe геттеры
        public List<EntitySnapshot> getEntitySnapshots() {
            return new ArrayList<>(entitySnapshots);
        }
        
        public List<EntitySnapshot> getLivingEntitySnapshots() {
            return entitySnapshots.stream()
                    .filter(EntitySnapshot::isLiving)
                    .collect(Collectors.toList());
        }

        // Legacy геттеры для совместимости
        @Deprecated
        public List<Entity> getEntities() {
            return new ArrayList<>(entities);
        }

        @Deprecated
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
            return !entitySnapshots.isEmpty() ? entitySnapshots.size() : entities.size();
        }
        
        public boolean isThreadSafeResult() {
            return !entitySnapshots.isEmpty();
        }
    }

    /**
     * Выполнить полную детекцию коллизий с подробным результатом - THREAD-SAFE
     * Работает со снепшотом вместо Level
     */
    public static CollisionResult detectCollisions(CollisionSnapshot snapshot, SpellShape shape, PersistenceType persistenceType) {
        List<EntitySnapshot> intersectingEntities = findIntersectingEntities(snapshot, shape, persistenceType);
        List<Vec3> impactPoints = new ArrayList<>();

        // Для каждой сущности найти точку столкновения
        for (EntitySnapshot entitySnapshot : intersectingEntities) {
            Vec3 impactPoint = findImpactPoint(shape, entitySnapshot);
            if (impactPoint != null) {
                impactPoints.add(impactPoint);
            }
        }

        return new CollisionResult(intersectingEntities, impactPoints);
    }

    /**
     * Выполнить полную детекцию коллизий с подробным результатом - LEGACY
     * @deprecated Используй thread-safe версию со снепшотами
     */
    @Deprecated
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

        return new CollisionResult(intersectingEntities, impactPoints, true);
    }

    /**
     * Найти точку столкновения заклинания с EntitySnapshot - THREAD-SAFE
     */
    private static Vec3 findImpactPoint(SpellShape shape, EntitySnapshot entitySnapshot) {
        Vec3 shapeCenter = shape.getCenter();
        Vec3 entityCenter = entitySnapshot.getPosition();
        
        // Простое приближение - точка на линии между центрами
        // TODO: Implement more precise impact point calculation based on bounding boxes
        return shapeCenter.add(entityCenter).scale(0.5);
    }

    /**
     * Найти точку столкновения заклинания с сущностью - LEGACY
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