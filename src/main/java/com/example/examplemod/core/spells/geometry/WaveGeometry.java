package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * Геометрия волны (WAVE) - расширяющаяся структура с изгибом
 */
public class WaveGeometry extends SpellGeometry {
    
    private final float currentRadius;
    private final float maxRadius;
    private final float bendAngle;
    private final float thickness;
    
    public WaveGeometry(SpellFormContext context) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("current_wave_radius", 1.0f),
            GeometryType.EXPANDING_CIRCLE
        );
        
        this.currentRadius = size;
        this.maxRadius = context.getParameter("radius", 10.0f);
        this.bendAngle = context.getParameter("wave_bend", 0.0f);
        this.thickness = context.getParameter("wave_thickness", 0.5f);
    }
    
    @Override
    public AABB getBoundingBox() {
        return new AABB(
            origin.x - currentRadius, origin.y - thickness, origin.z - currentRadius,
            origin.x + currentRadius, origin.y + thickness, origin.z + currentRadius
        );
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        List<Vec3> points = new ArrayList<>();
        
        // Точки по окружности волны
        int segments = Math.max(8, (int)(currentRadius * 2));
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = origin.x + Math.cos(angle) * currentRadius;
            double z = origin.z + Math.sin(angle) * currentRadius;
            points.add(new Vec3(x, origin.y, z));
        }
        
        return points;
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        AABB ourBounds = getBoundingBox();
        AABB otherBounds = other.getBoundingBox();
        
        if (!ourBounds.intersects(otherBounds)) {
            return CollisionResult.noCollision();
        }
        
        Vec3 otherCenter = other.getCenter();
        double distance = origin.distanceTo(otherCenter);
        
        // Проверяем, находится ли точка в кольце волны
        if (distance >= currentRadius - thickness && distance <= currentRadius + thickness) {
            Vec3 normal = otherCenter.subtract(origin).normalize();
            Vec3 collisionPoint = origin.add(normal.scale(currentRadius));
            return CollisionResult.collision(collisionPoint, normal, thickness, CollisionResult.CollisionType.SURFACE_CONTACT);
        }
        
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        double distance = origin.distanceTo(point);
        return distance >= currentRadius - thickness && distance <= currentRadius + thickness &&
               Math.abs(point.y - origin.y) <= thickness;
    }
    
    @Override
    public boolean intersects(AABB bounds) {
        return getBoundingBox().intersects(bounds);
    }
    
    @Override
    public void update(Vec3 newPosition, Vec3 newDirection) {
        // Волна расширяется от исходной позиции
        this.size = Math.min(maxRadius, this.size + 0.5f);
    }
    
    @Override
    public double getVolume() {
        return Math.PI * (Math.pow(currentRadius + thickness, 2) - Math.pow(Math.max(0, currentRadius - thickness), 2)) * thickness * 2;
    }
    
    @Override
    public double getSurfaceArea() {
        return 2 * Math.PI * currentRadius * thickness * 2;
    }
}