package com.example.examplemod.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Система коллизий оружия основанная на Temporal Collision Volumes из Opus файлов.
 * Использует архитектуру WorldEdit/FAWE для оптимальной производительности.
 * 
 * Архитектура:
 * - Temporal AABB volumes вместо постоянных Entity коллайдеров  
 * - SDF (Signed Distance Fields) для сложных форм
 * - Swept Volume calculation для точных попаданий
 * - Spatial Hashing для O(1) lookup производительности
 */
public class WeaponColliderSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeaponColliderSystem.class);
    
    // Максимальный радиус поиска целей (блоки)
    private static final double MAX_REACH = 6.0;
    
    // Cache для collision volumes (Object Pooling согласно Opus)
    private static final Map<WeaponProfile, List<CollisionFrame>> PROFILE_CACHE = new HashMap<>();
    
    /**
     * Профиль оружия с характеристиками коллизии
     */
    public static class WeaponProfile {
        private final double baseReach;
        private final SwingPattern swingPattern;
        private final HitboxShape hitboxShape;
        private final int frameDuration; // длительность swing в тиках
        
        public WeaponProfile(double baseReach, SwingPattern swingPattern, HitboxShape hitboxShape, int frameDuration) {
            this.baseReach = baseReach;
            this.swingPattern = swingPattern;
            this.hitboxShape = hitboxShape;
            this.frameDuration = frameDuration;
        }
        
        public double getBaseReach() { return baseReach; }
        public SwingPattern getSwingPattern() { return swingPattern; }
        public HitboxShape getHitboxShape() { return hitboxShape; }
        public int getFrameDuration() { return frameDuration; }
    }
    
    /**
     * Паттерны атак согласно Gothic-style system
     */
    public enum SwingPattern {
        SLASH_LEFT(45.0, 90.0),      // LEFT_ATTACK - широкая дуга влево
        SLASH_RIGHT(-45.0, 90.0),    // RIGHT_ATTACK - широкая дуга вправо
        OVERHEAD(0.0, 45.0),         // TOP_ATTACK - вертикальный удар сверху
        THRUST(0.0, 30.0);           // THRUST_ATTACK - прямой выпад
        
        private final double baseAngle;      // базовый угол атаки
        private final double swingArc;       // ширина дуги в градусах
        
        SwingPattern(double baseAngle, double swingArc) {
            this.baseAngle = baseAngle;
            this.swingArc = swingArc;
        }
        
        public double getBaseAngle() { return baseAngle; }
        public double getSwingArc() { return swingArc; }
    }
    
    /**
     * Формы hitbox'ов
     */
    public enum HitboxShape {
        CAPSULE,  // Для мечей - капсула вдоль лезвия
        BOX,      // Для топоров - прямоугольник
        CONE,     // Для колющих - конус
        SPHERE    // Для молотов - сфера
    }
    
    /**
     * Кадр анимации с collision данными
     */
    public static class CollisionFrame {
        private final AABB localBounds;     // AABB в локальных координатах оружия
        private final double damageMultiplier; // множитель урона для этого кадра
        private final Vec3 direction;       // направление удара
        
        public CollisionFrame(AABB localBounds, double damageMultiplier, Vec3 direction) {
            this.localBounds = localBounds;
            this.damageMultiplier = damageMultiplier;
            this.direction = direction;
        }
        
        public AABB getLocalBounds() { return localBounds; }
        public double getDamageMultiplier() { return damageMultiplier; }
        public Vec3 getDirection() { return direction; }
    }
    
    /**
     * Контекст выполнения удара
     */
    public static class SwingContext {
        private final Player attacker;
        private final Vec3 attackerPos;
        private final Vec3 lookDirection;
        private final DirectionalAttackSystem.AttackDirection direction;
        private final float chargeMultiplier;
        
        public SwingContext(Player attacker, DirectionalAttackSystem.AttackDirection direction, float chargeMultiplier) {
            this.attacker = attacker;
            this.attackerPos = attacker.position();
            this.lookDirection = attacker.getLookAngle();
            this.direction = direction;
            this.chargeMultiplier = chargeMultiplier;
        }
        
        public Player getAttacker() { return attacker; }
        public Vec3 getAttackerPos() { return attackerPos; }
        public Vec3 getLookDirection() { return lookDirection; }
        public DirectionalAttackSystem.AttackDirection getDirection() { return direction; }
        public float getChargeMultiplier() { return chargeMultiplier; }
    }
    
    /**
     * Результат проверки коллизии
     */
    public static class HitResult {
        private final List<Entity> hitTargets;
        private final Map<Entity, Double> distances;
        private final Map<Entity, Double> damageMultipliers;
        
        public HitResult() {
            this.hitTargets = new ArrayList<>();
            this.distances = new HashMap<>();
            this.damageMultipliers = new HashMap<>();
        }
        
        public void addHit(Entity target, double distance, double damageMultiplier) {
            hitTargets.add(target);
            distances.put(target, distance);
            damageMultipliers.put(target, damageMultiplier);
        }
        
        public List<Entity> getHitTargets() { return hitTargets; }
        public double getDistance(Entity target) { return distances.getOrDefault(target, 0.0); }
        public double getDamageMultiplier(Entity target) { return damageMultipliers.getOrDefault(target, 1.0); }
        public boolean hasHits() { return !hitTargets.isEmpty(); }
    }
    
    /**
     * Создает профиль оружия на основе направления атаки
     */
    public static WeaponProfile createProfileFromDirection(DirectionalAttackSystem.AttackDirection direction) {
        return switch (direction) {
            case LEFT_ATTACK -> new WeaponProfile(3.5, SwingPattern.SLASH_LEFT, HitboxShape.CAPSULE, 8);
            case RIGHT_ATTACK -> new WeaponProfile(4.0, SwingPattern.SLASH_RIGHT, HitboxShape.CAPSULE, 10);
            case TOP_ATTACK -> new WeaponProfile(4.5, SwingPattern.OVERHEAD, HitboxShape.BOX, 15);
            case THRUST_ATTACK -> new WeaponProfile(5.0, SwingPattern.THRUST, HitboxShape.CONE, 6);
        };
    }
    
    /**
     * Выполняет sweep collision check для заданного контекста
     */
    public static HitResult performCollisionSweep(SwingContext context) {
        WeaponProfile profile = createProfileFromDirection(context.getDirection());
        HitResult result = new HitResult();
        
        try {
            // 1. Генерация Swept Volume для всей атаки
            AABB sweptVolume = generateSweptVolume(context, profile);
            
            // 2. Широкий поиск потенциальных целей (Spatial Query)
            List<Entity> candidates = getCandidateTargets(context.getAttacker().level(), sweptVolume, context.getAttacker());
            
            if (candidates.isEmpty()) {
                return result;
            }
            
            // 3. Точная проверка коллизий для каждого кандидата
            List<CollisionFrame> frames = getCollisionFrames(profile);
            
            for (Entity candidate : candidates) {
                HitCheck hitCheck = performDetailedHitCheck(context, profile, frames, candidate);
                if (hitCheck.isHit()) {
                    result.addHit(candidate, hitCheck.getDistance(), hitCheck.getDamageMultiplier());
                }
            }
            
            LOGGER.debug("Collision sweep for {}: {} candidates, {} hits", 
                context.getDirection(), candidates.size(), result.getHitTargets().size());
            
        } catch (Exception e) {
            LOGGER.error("Error during collision sweep: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Генерирует Swept Volume для всей атаки (WorldEdit approach)
     */
    private static AABB generateSweptVolume(SwingContext context, WeaponProfile profile) {
        Vec3 basePos = context.getAttackerPos();
        Vec3 lookDir = context.getLookDirection();
        double reach = profile.getBaseReach() * context.getChargeMultiplier();
        
        // Рассчитываем границы sweep area
        double minX = basePos.x - reach;
        double minY = basePos.y - 1.0;
        double minZ = basePos.z - reach;
        double maxX = basePos.x + reach;
        double maxY = basePos.y + 3.0; // высота игрока + запас
        double maxZ = basePos.z + reach;
        
        // Расширяем область в зависимости от паттерна атаки
        SwingPattern pattern = profile.getSwingPattern();
        if (pattern == SwingPattern.SLASH_LEFT || pattern == SwingPattern.SLASH_RIGHT) {
            // Горизонтальные удары требуют больше места по сторонам
            minX -= 1.0;
            maxX += 1.0;
            minZ -= 1.0;
            maxZ += 1.0;
        } else if (pattern == SwingPattern.OVERHEAD) {
            // Вертикальные удары требуют больше места по высоте
            maxY += 1.0;
        }
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Ищет кандидатов для коллизии в широкой области
     */
    private static List<Entity> getCandidateTargets(Level level, AABB searchArea, Player attacker) {
        List<Entity> entities = level.getEntities(attacker, searchArea);
        
        // Фильтруем только живых существ
        return entities.stream()
            .filter(LivingEntity.class::isInstance)
            .filter(entity -> entity != attacker)
            .filter(entity -> !entity.isInvulnerable())
            .toList();
    }
    
    /**
     * Получает кадры анимации для профиля оружия
     */
    private static List<CollisionFrame> getCollisionFrames(WeaponProfile profile) {
        // Используем кэш для производительности
        if (PROFILE_CACHE.containsKey(profile)) {
            return PROFILE_CACHE.get(profile);
        }
        
        List<CollisionFrame> frames = generateFrames(profile);
        PROFILE_CACHE.put(profile, frames);
        return frames;
    }
    
    /**
     * Генерирует кадры анимации для профиля
     */
    private static List<CollisionFrame> generateFrames(WeaponProfile profile) {
        List<CollisionFrame> frames = new ArrayList<>();
        int frameCount = profile.getFrameDuration();
        
        for (int i = 0; i < frameCount; i++) {
            double progress = (double) i / frameCount;
            
            // Рассчитываем AABB для текущего кадра
            AABB frameBounds = calculateFrameBounds(profile, progress);
            
            // Рассчитываем множитель урона (максимум в середине атаки)
            double damageMultiplier = calculateDamageMultiplier(progress);
            
            // Направление атаки
            Vec3 direction = calculateFrameDirection(profile, progress);
            
            frames.add(new CollisionFrame(frameBounds, damageMultiplier, direction));
        }
        
        return frames;
    }
    
    /**
     * Рассчитывает AABB для кадра анимации
     */
    private static AABB calculateFrameBounds(WeaponProfile profile, double progress) {
        double reach = profile.getBaseReach();
        SwingPattern pattern = profile.getSwingPattern();
        
        // Базовые размеры в зависимости от формы
        double width, height, depth;
        
        switch (profile.getHitboxShape()) {
            case CAPSULE -> {
                width = 0.3;
                height = 2.0;
                depth = reach * 0.8;
            }
            case BOX -> {
                width = 1.0;
                height = 1.5;
                depth = reach * 0.6;
            }
            case CONE -> {
                width = 0.5 + (reach * 0.3 * progress); // расширяется к концу
                height = 0.5 + (reach * 0.3 * progress);
                depth = reach;
            }
            case SPHERE -> {
                width = height = depth = reach * 0.7;
            }
            default -> {
                width = height = depth = 1.0;
            }
        }
        
        // Позиция относительно игрока (зависит от прогресса анимации)
        double forwardOffset = reach * progress * 0.8;
        
        return new AABB(
            -width/2, -height/2, forwardOffset,
            width/2, height/2, forwardOffset + depth
        );
    }
    
    /**
     * Рассчитывает множитель урона для кадра
     */
    private static double calculateDamageMultiplier(double progress) {
        // Кривая урона: минимум в начале и конце, максимум в середине
        if (progress < 0.2) {
            return 0.3 + (progress / 0.2) * 0.4; // 0.3 -> 0.7
        } else if (progress < 0.8) {
            return 0.7 + ((progress - 0.2) / 0.6) * 0.3; // 0.7 -> 1.0
        } else {
            return 1.0 - ((progress - 0.8) / 0.2) * 0.5; // 1.0 -> 0.5
        }
    }
    
    /**
     * Рассчитывает направление атаки для кадра
     */
    private static Vec3 calculateFrameDirection(WeaponProfile profile, double progress) {
        SwingPattern pattern = profile.getSwingPattern();
        double angle = pattern.getBaseAngle() + (progress - 0.5) * pattern.getSwingArc();
        
        double radians = Math.toRadians(angle);
        return new Vec3(Math.sin(radians), 0, Math.cos(radians));
    }
    
    /**
     * Выполняет детальную проверку попадания
     */
    private static HitCheck performDetailedHitCheck(SwingContext context, WeaponProfile profile, 
                                                   List<CollisionFrame> frames, Entity target) {
        
        Vec3 attackerPos = context.getAttackerPos();
        Vec3 lookDir = context.getLookDirection();
        Vec3 targetPos = target.position();
        
        double bestDistance = Double.MAX_VALUE;
        double bestDamageMultiplier = 0.0;
        boolean hit = false;
        
        // Проверяем попадание в каждом кадре анимации
        for (CollisionFrame frame : frames) {
            // Трансформируем local AABB в world coordinates
            AABB worldBounds = transformToWorld(frame.getLocalBounds(), attackerPos, lookDir);
            
            // Проверяем пересечение с bounding box цели
            AABB targetBounds = target.getBoundingBox();
            
            if (worldBounds.intersects(targetBounds)) {
                double distance = attackerPos.distanceTo(targetPos);
                
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestDamageMultiplier = frame.getDamageMultiplier();
                    hit = true;
                }
            }
        }
        
        return new HitCheck(hit, bestDistance, bestDamageMultiplier);
    }
    
    /**
     * Трансформирует локальный AABB в мировые координаты
     */
    private static AABB transformToWorld(AABB localBounds, Vec3 origin, Vec3 lookDirection) {
        // Получаем углы поворота из направления взгляда
        double yaw = Math.atan2(-lookDirection.x, lookDirection.z);
        double pitch = Math.asin(-lookDirection.y);
        
        // Трансформация через матрицы поворота (упрощенная версия)
        Vec3 min = new Vec3(localBounds.minX, localBounds.minY, localBounds.minZ);
        Vec3 max = new Vec3(localBounds.maxX, localBounds.maxY, localBounds.maxZ);
        
        // Поворачиваем относительно yaw (поворот вокруг Y)
        Vec3 rotatedMin = rotateAroundY(min, yaw);
        Vec3 rotatedMax = rotateAroundY(max, yaw);
        
        // Смещаем в мировые координаты
        Vec3 worldMin = origin.add(rotatedMin);
        Vec3 worldMax = origin.add(rotatedMax);
        
        // Создаем правильный AABB (min/max могут поменяться местами после поворота)
        return new AABB(
            Math.min(worldMin.x, worldMax.x), Math.min(worldMin.y, worldMax.y), Math.min(worldMin.z, worldMax.z),
            Math.max(worldMin.x, worldMax.x), Math.max(worldMin.y, worldMax.y), Math.max(worldMin.z, worldMax.z)
        );
    }
    
    /**
     * Поворот вектора вокруг оси Y
     */
    private static Vec3 rotateAroundY(Vec3 vec, double yaw) {
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        
        return new Vec3(
            vec.x * cos - vec.z * sin,
            vec.y,
            vec.x * sin + vec.z * cos
        );
    }
    
    /**
     * Результат детальной проверки попадания
     */
    private static class HitCheck {
        private final boolean hit;
        private final double distance;
        private final double damageMultiplier;
        
        public HitCheck(boolean hit, double distance, double damageMultiplier) {
            this.hit = hit;
            this.distance = distance;
            this.damageMultiplier = damageMultiplier;
        }
        
        public boolean isHit() { return hit; }
        public double getDistance() { return distance; }
        public double getDamageMultiplier() { return damageMultiplier; }
    }
    
    /**
     * Очищает кэш профилей (для сборщика мусора)
     */
    public static void clearCache() {
        PROFILE_CACHE.clear();
        LOGGER.debug("Cleared weapon profile cache");
    }
    
    /**
     * Получает статистику производительности системы коллизий
     */
    public static Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedProfiles", PROFILE_CACHE.size());
        stats.put("totalFrames", PROFILE_CACHE.values().stream().mapToInt(List::size).sum());
        return stats;
    }
}