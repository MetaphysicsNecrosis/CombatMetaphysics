package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Геометрия снарядов (PROJECTILE)
 * Может быть сферой, цилиндром или конусом
 */
public class ProjectileGeometry extends SpellGeometry {
    
    private final float length;
    private final ProjectileShape projectileShape;
    
    public ProjectileGeometry(SpellFormContext context) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("geometry_size", 0.5f),
            determineGeometryType(context)
        );
        
        this.length = context.getParameter("length", size * 2.0f);
        this.projectileShape = determineProjectileShape(context);
    }
    
    private static GeometryType determineGeometryType(SpellFormContext context) {
        float speed = context.getParameter("speed", 1.0f);
        float pierce = context.getParameter("pierce_count", 0.0f);
        
        if (pierce > 0 || speed > 3.0f) {
            return GeometryType.CYLINDER; // Копье для пробивающих снарядов
        } else if (speed > 1.5f) {
            return GeometryType.CONE; // Остроконечный для быстрых
        } else {
            return GeometryType.SPHERE; // Сфера для обычных
        }
    }
    
    private static ProjectileShape determineProjectileShape(SpellFormContext context) {
        return switch (context.getParameter("pierce_count", 0.0f) > 0 ? "piercing" : 
                      context.getParameter("speed", 1.0f) > 2.0f ? "fast" : "normal") {
            case "piercing" -> ProjectileShape.SPEAR;
            case "fast" -> ProjectileShape.BULLET;
            default -> ProjectileShape.ORB;
        };
    }
    
    @Override
    public AABB getBoundingBox() {
        return switch (projectileShape) {
            case SPEAR -> createCylinderAABB();
            case BULLET -> createConeAABB();
            case ORB -> createSphereAABB();
        };
    }
    
    private AABB createSphereAABB() {
        return new AABB(
            origin.x - size, origin.y - size, origin.z - size,
            origin.x + size, origin.y + size, origin.z + size
        );
    }
    
    private AABB createCylinderAABB() {
        Vec3 end = origin.add(direction.scale(length));
        return new AABB(
            Math.min(origin.x, end.x) - size, Math.min(origin.y, end.y) - size, Math.min(origin.z, end.z) - size,
            Math.max(origin.x, end.x) + size, Math.max(origin.y, end.y) + size, Math.max(origin.z, end.z) + size
        );
    }
    
    private AABB createConeAABB() {
        Vec3 tip = origin.add(direction.scale(length));
        return new AABB(
            Math.min(origin.x - size, tip.x), Math.min(origin.y - size, tip.y), Math.min(origin.z - size, tip.z),
            Math.max(origin.x + size, tip.x), Math.max(origin.y + size, tip.y), Math.max(origin.z + size, tip.z)
        );
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return switch (projectileShape) {
            case SPEAR -> createCylinderShape();
            case BULLET -> createConeShape();
            case ORB -> createSphereShape();
        };
    }
    
    private VoxelShape createSphereShape() {
        return Shapes.create(getBoundingBox());
    }
    
    private VoxelShape createCylinderShape() {
        return Shapes.create(getBoundingBox());
    }
    
    private VoxelShape createConeShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        List<Vec3> points = new ArrayList<>();
        
        return switch (projectileShape) {
            case SPEAR -> {
                // Точки вдоль оси копья
                int segments = Math.max(3, (int)(length / size));
                for (int i = 0; i <= segments; i++) {
                    Vec3 point = origin.add(direction.scale(length * i / segments));
                    points.add(point);
                }
                yield points;
            }
            case BULLET -> {
                // Острие и основание конуса
                points.add(origin.add(direction.scale(length))); // Острие
                points.add(origin); // Основание
                yield points;
            }
            case ORB -> {
                // Центр сферы
                points.add(origin);
                yield points;
            }
        };
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        AABB ourBounds = getBoundingBox();
        AABB otherBounds = other.getBoundingBox();
        
        if (!ourBounds.intersects(otherBounds)) {
            return CollisionResult.noCollision();
        }
        
        // Простая проверка пересечения центров
        Vec3 ourCenter = getCenter();
        Vec3 otherCenter = other.getCenter();
        double distance = ourCenter.distanceTo(otherCenter);
        double combinedRadius = size + other.getSize();
        
        if (distance < combinedRadius) {
            Vec3 collisionPoint = ourCenter.add(otherCenter).scale(0.5);
            Vec3 normal = otherCenter.subtract(ourCenter).normalize();
            double penetration = combinedRadius - distance;
            
            return CollisionResult.collision(collisionPoint, normal, penetration, 
                penetration > combinedRadius * 0.8 ? CollisionResult.CollisionType.FULL_CONTAINMENT : 
                CollisionResult.CollisionType.PARTIAL_OVERLAP);
        }
        
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        return switch (projectileShape) {
            case ORB -> origin.distanceTo(point) <= size;
            case SPEAR -> distanceToLineSegment(origin, origin.add(direction.scale(length)), point) <= size;
            case BULLET -> distanceToCone(point) <= size;
        };
    }
    
    @Override
    public boolean intersects(AABB bounds) {
        return getBoundingBox().intersects(bounds);
    }
    
    @Override
    public void update(Vec3 newPosition, Vec3 newDirection) {
        this.origin = newPosition;
        this.direction = newDirection.normalize();
    }
    
    @Override
    public double getVolume() {
        return switch (projectileShape) {
            case ORB -> (4.0/3.0) * Math.PI * size * size * size;
            case SPEAR -> Math.PI * size * size * length;
            case BULLET -> (1.0/3.0) * Math.PI * size * size * length;
        };
    }
    
    @Override
    public double getSurfaceArea() {
        return switch (projectileShape) {
            case ORB -> 4.0 * Math.PI * size * size;
            case SPEAR -> 2.0 * Math.PI * size * (size + length);
            case BULLET -> Math.PI * size * (size + Math.sqrt(size * size + length * length));
        };
    }
    
    private double distanceToLineSegment(Vec3 lineStart, Vec3 lineEnd, Vec3 point) {
        Vec3 line = lineEnd.subtract(lineStart);
        Vec3 pointToStart = point.subtract(lineStart);
        
        double lineLengthSq = line.lengthSqr();
        if (lineLengthSq == 0) return pointToStart.length();
        
        double t = Math.max(0, Math.min(1, pointToStart.dot(line) / lineLengthSq));
        Vec3 projection = lineStart.add(line.scale(t));
        
        return point.distanceTo(projection);
    }
    
    private double distanceToCone(Vec3 point) {
        Vec3 tip = origin.add(direction.scale(length));
        Vec3 toPoint = point.subtract(origin);
        
        double projectionLength = toPoint.dot(direction);
        if (projectionLength < 0 || projectionLength > length) {
            return Math.min(point.distanceTo(origin), point.distanceTo(tip));
        }
        
        double radiusAtPoint = size * (1.0 - projectionLength / length);
        Vec3 axisPoint = origin.add(direction.scale(projectionLength));
        double distanceFromAxis = point.distanceTo(axisPoint);
        
        return Math.abs(distanceFromAxis - radiusAtPoint);
    }
    
    private enum ProjectileShape {
        ORB,     // Сферический снаряд
        SPEAR,   // Копье/стрела
        BULLET   // Пуля/конус
    }
}