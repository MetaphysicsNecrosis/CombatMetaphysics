package com.example.examplemod.core.spells.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe снепшот состояния мира для детекции коллизий
 * НЕ содержит прямых ссылок на Minecraft объекты
 * 
 * Соответствует архитектуре: Main Thread -> Collision Thread (ВОТ МЫ ЗДЕСЬ)
 */
public class CollisionSnapshot {
    
    // === THREAD-SAFE ДАННЫЕ ===
    private final long snapshotTime;
    private final int dimensionId;
    
    // Снепшоты сущностей - копии данных без ссылок
    private final Map<UUID, EntitySnapshot> nearbyEntities;
    
    // Снепшоты блоков - копии BlockState без ссылок на Level
    private final Map<BlockPos, BlockSnapshot> nearbyBlocks;
    
    // Границы области снепшота
    private final AABB snapshotBounds;
    
    public CollisionSnapshot(Level level, AABB searchArea) {
        this.snapshotTime = System.currentTimeMillis();
        this.dimensionId = level.dimension().hashCode();
        this.snapshotBounds = searchArea.inflate(1.0); // Небольшое расширение
        this.nearbyEntities = new ConcurrentHashMap<>();
        this.nearbyBlocks = new ConcurrentHashMap<>();
        
        // Создаём снепшоты в Main Thread (безопасно)
        captureEntities(level, searchArea);
        captureBlocks(level, searchArea);
    }
    
    /**
     * Захватить снепшоты сущностей (Main Thread only)
     */
    private void captureEntities(Level level, AABB searchArea) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, searchArea);
        
        for (Entity entity : entities) {
            if (entity != null && entity.isAlive()) {
                EntitySnapshot snapshot = new EntitySnapshot(entity);
                nearbyEntities.put(entity.getUUID(), snapshot);
            }
        }
    }
    
    /**
     * Захватить снепшоты блоков (Main Thread only)
     */
    private void captureBlocks(Level level, AABB searchArea) {
        int minX = (int) Math.floor(searchArea.minX);
        int minY = (int) Math.floor(searchArea.minY);
        int minZ = (int) Math.floor(searchArea.minZ);
        int maxX = (int) Math.ceil(searchArea.maxX);
        int maxY = (int) Math.ceil(searchArea.maxY);
        int maxZ = (int) Math.ceil(searchArea.maxZ);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    if (!state.isAir()) {
                        BlockSnapshot snapshot = new BlockSnapshot(pos, state);
                        nearbyBlocks.put(pos, snapshot);
                    }
                }
            }
        }
    }
    
    // === THREAD-SAFE ГЕТТЕРЫ ===
    
    public Collection<EntitySnapshot> getAllEntities() {
        return new ArrayList<>(nearbyEntities.values());
    }
    
    public Collection<EntitySnapshot> getLivingEntities() {
        return nearbyEntities.values().stream()
                .filter(EntitySnapshot::isLiving)
                .toList();
    }
    
    public Collection<BlockSnapshot> getAllBlocks() {
        return new ArrayList<>(nearbyBlocks.values());
    }
    
    public Optional<EntitySnapshot> getEntity(UUID entityId) {
        return Optional.ofNullable(nearbyEntities.get(entityId));
    }
    
    public Optional<BlockSnapshot> getBlock(BlockPos pos) {
        return Optional.ofNullable(nearbyBlocks.get(pos));
    }
    
    public AABB getSnapshotBounds() {
        return snapshotBounds;
    }
    
    public long getSnapshotTime() {
        return snapshotTime;
    }
    
    public int getDimensionId() {
        return dimensionId;
    }
    
    public int getEntityCount() {
        return nearbyEntities.size();
    }
    
    public int getBlockCount() {
        return nearbyBlocks.size();
    }
    
    /**
     * Проверить истекло ли время снепшота (для кэширования)
     */
    public boolean isExpired(long maxAgeMs) {
        return System.currentTimeMillis() - snapshotTime > maxAgeMs;
    }
    
    // === ВНУТРЕННИЕ КЛАССЫ СНЕПШОТОВ ===
    
    /**
     * Thread-safe снепшот сущности без ссылки на Entity
     */
    public static class EntitySnapshot {
        private final UUID entityId;
        private final String entityType;
        private final Vec3 position;
        private final AABB boundingBox;
        private final boolean isLiving;
        private final boolean isAlive;
        private final float health;
        private final float maxHealth;
        private final boolean isOnGround;
        
        private EntitySnapshot(Entity entity) {
            this.entityId = entity.getUUID();
            this.entityType = entity.getType().toString();
            this.position = entity.position();
            this.boundingBox = entity.getBoundingBox();
            this.isLiving = entity instanceof LivingEntity;
            this.isAlive = entity.isAlive();
            this.isOnGround = entity.onGround();
            
            if (entity instanceof LivingEntity living) {
                this.health = living.getHealth();
                this.maxHealth = living.getMaxHealth();
            } else {
                this.health = 1.0f;
                this.maxHealth = 1.0f;
            }
        }
        
        // Thread-safe геттеры
        public UUID getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
        public Vec3 getPosition() { return position; }
        public AABB getBoundingBox() { return boundingBox; }
        public boolean isLiving() { return isLiving; }
        public boolean isAlive() { return isAlive; }
        public float getHealth() { return health; }
        public float getMaxHealth() { return maxHealth; }
        public boolean isOnGround() { return isOnGround; }
        
        /**
         * Проверить пересечение с областью
         */
        public boolean intersects(AABB area) {
            return boundingBox.intersects(area);
        }
        
        /**
         * Вычислить расстояние до точки
         */
        public double distanceTo(Vec3 point) {
            return position.distanceTo(point);
        }
    }
    
    /**
     * Thread-safe снепшот блока без ссылки на BlockState
     */
    public static class BlockSnapshot {
        private final BlockPos position;
        private final String blockType;
        private final boolean isSolid;
        private final boolean isAir;
        private final AABB shape;
        private final float hardness;
        private final boolean canOcclude;
        
        private BlockSnapshot(BlockPos pos, BlockState state) {
            this.position = pos.immutable(); // Defensive copy
            this.blockType = state.getBlock().toString();
            this.isSolid = !state.isAir() && state.canOcclude();
            this.isAir = state.isAir();
            this.hardness = state.getBlock().defaultDestroyTime();
            this.canOcclude = state.canOcclude();
            
            // Создаём AABB блока
            this.shape = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                 pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }
        
        // Thread-safe геттеры
        public BlockPos getPosition() { return position; }
        public String getBlockType() { return blockType; }
        public boolean isSolid() { return isSolid; }
        public boolean isAir() { return isAir; }
        public AABB getShape() { return shape; }
        public float getHardness() { return hardness; }
        public boolean canOcclude() { return canOcclude; }
        
        /**
         * Проверить пересечение с областью
         */
        public boolean intersects(AABB area) {
            return shape.intersects(area);
        }
    }
}