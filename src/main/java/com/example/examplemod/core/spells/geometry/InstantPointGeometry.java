package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Геометрия мгновенной точки (INSTANT_POINT)
 */
public class InstantPointGeometry extends SpellGeometry {
    
    private final float explosionRadius;
    
    public InstantPointGeometry(SpellFormContext context) {
        super(
            context.getTargetPosition(), // Целевая позиция, не origin
            Vec3.ZERO,
            context.getParameter("radius", 2.0f),
            GeometryType.POINT
        );
        
        this.explosionRadius = size;
    }
    
    @Override
    public AABB getBoundingBox() {
        return new AABB(
            origin.x - explosionRadius, origin.y - explosionRadius, origin.z - explosionRadius,
            origin.x + explosionRadius, origin.y + explosionRadius, origin.z + explosionRadius
        );
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        return List.of(origin);
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        double distance = origin.distanceTo(other.getCenter());
        if (distance <= explosionRadius) {
            Vec3 normal = other.getCenter().subtract(origin).normalize();
            double penetration = explosionRadius - distance;
            return CollisionResult.collision(origin, normal, penetration, CollisionResult.CollisionType.PARTIAL_OVERLAP);
        }
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        return origin.distanceTo(point) <= explosionRadius;
    }
    
    @Override
    public boolean intersects(AABB bounds) {
        return getBoundingBox().intersects(bounds);
    }
    
    @Override
    public void update(Vec3 newPosition, Vec3 newDirection) {
        // Мгновенные точки не перемещаются
    }
    
    @Override
    public double getVolume() {
        return (4.0/3.0) * Math.PI * explosionRadius * explosionRadius * explosionRadius;
    }
    
    @Override
    public double getSurfaceArea() {
        return 4.0 * Math.PI * explosionRadius * explosionRadius;
    }
}