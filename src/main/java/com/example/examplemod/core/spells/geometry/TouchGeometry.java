package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Геометрия касания (TOUCH) - область вокруг руки заклинателя
 */
public class TouchGeometry extends SpellGeometry {
    
    private final float touchRadius;
    
    public TouchGeometry(SpellFormContext context) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("geometry_size", 1.0f),
            GeometryType.TOUCH_AREA
        );
        
        this.touchRadius = size;
    }
    
    @Override
    public AABB getBoundingBox() {
        return new AABB(
            origin.x - touchRadius, origin.y - touchRadius, origin.z - touchRadius,
            origin.x + touchRadius, origin.y + touchRadius, origin.z + touchRadius
        );
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        return List.of(origin); // Центр касания
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        double distance = origin.distanceTo(other.getCenter());
        if (distance <= touchRadius + other.getSize()) {
            Vec3 normal = other.getCenter().subtract(origin).normalize();
            Vec3 collisionPoint = origin.add(normal.scale(touchRadius));
            return CollisionResult.collision(collisionPoint, normal, touchRadius, CollisionResult.CollisionType.SURFACE_CONTACT);
        }
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        return origin.distanceTo(point) <= touchRadius;
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
        return (4.0/3.0) * Math.PI * touchRadius * touchRadius * touchRadius;
    }
    
    @Override
    public double getSurfaceArea() {
        return 4.0 * Math.PI * touchRadius * touchRadius;
    }
}