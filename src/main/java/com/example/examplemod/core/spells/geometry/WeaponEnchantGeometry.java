package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Геометрия зачарования оружия (WEAPON_ENCHANT)
 */
public class WeaponEnchantGeometry extends SpellGeometry {
    
    public WeaponEnchantGeometry(SpellFormContext context) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("geometry_size", 1.5f),
            GeometryType.WEAPON_BOUNDS
        );
    }
    
    @Override
    public AABB getBoundingBox() {
        return new AABB(
            origin.x - size, origin.y - size, origin.z - size,
            origin.x + size, origin.y + size, origin.z + size
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
        if (distance <= size + other.getSize()) {
            Vec3 normal = other.getCenter().subtract(origin).normalize();
            return CollisionResult.collision(origin, normal, size, CollisionResult.CollisionType.SURFACE_CONTACT);
        }
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        return origin.distanceTo(point) <= size;
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
        return size * size * size;
    }
    
    @Override
    public double getSurfaceArea() {
        return 6 * size * size;
    }
}