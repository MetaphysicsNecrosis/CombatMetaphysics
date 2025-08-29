package com.example.examplemod.core.geometry.shapes;

import com.example.examplemod.core.geometry.SpellShape;
import com.example.examplemod.core.geometry.ShapeType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.ArrayList;

/**
 * Сферическая форма заклинания
 * Наиболее распространённая форма для PROJECTILE, AREA, INSTANT_POINT
 */
public class SphereShape implements SpellShape {
    private final Vec3 center;
    private final double radius;

    public SphereShape(Vec3 center, double radius) {
        this.center = center;
        this.radius = Math.max(radius, 0.1); // Минимальный радиус
    }

    @Override
    public ShapeType getType() {
        return ShapeType.SPHERE;
    }

    @Override
    public AABB getBoundingBox() {
        return new AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        );
    }

    @Override
    public boolean intersects(Vec3 point) {
        return center.distanceTo(point) <= radius;
    }

    @Override
    public boolean intersects(SpellShape other) {
        if (other instanceof SphereShape otherSphere) {
            double distance = this.center.distanceTo(otherSphere.center);
            return distance <= (this.radius + otherSphere.radius);
        }
        
        // Для других форм используем проверку AABB как приближение
        return this.getBoundingBox().intersects(other.getBoundingBox());
    }

    @Override
    public boolean intersects(AABB boundingBox) {
        // Найти ближайшую точку AABB к центру сферы
        double closestX = Math.max(boundingBox.minX, Math.min(center.x, boundingBox.maxX));
        double closestY = Math.max(boundingBox.minY, Math.min(center.y, boundingBox.maxY));
        double closestZ = Math.max(boundingBox.minZ, Math.min(center.z, boundingBox.maxZ));
        
        Vec3 closestPoint = new Vec3(closestX, closestY, closestZ);
        return center.distanceTo(closestPoint) <= radius;
    }

    @Override
    public List<Vec3> getCollisionPoints() {
        List<Vec3> points = new ArrayList<>();
        
        // Добавить центр
        points.add(center);
        
        // Добавить точки на поверхности сферы (для детальной проверки)
        int samples = Math.max(8, (int)(radius * 4)); // Больше точек для больших сфер
        
        for (int i = 0; i < samples; i++) {
            double theta = 2 * Math.PI * i / samples;
            for (int j = 0; j < samples / 2; j++) {
                double phi = Math.PI * j / (samples / 2 - 1);
                
                double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
                double y = center.y + radius * Math.cos(phi);
                double z = center.z + radius * Math.sin(phi) * Math.sin(theta);
                
                points.add(new Vec3(x, y, z));
            }
        }
        
        return points;
    }

    @Override
    public Vec3 getCenter() {
        return center;
    }

    @Override
    public double getVolume() {
        return (4.0 / 3.0) * Math.PI * radius * radius * radius;
    }

    @Override
    public SpellShape transform(Vec3 translation, Vec3 rotation, double scale) {
        // Для сферы поворот не влияет на форму
        Vec3 newCenter = center.add(translation);
        double newRadius = radius * scale;
        return new SphereShape(newCenter, newRadius);
    }

    @Override
    public SpellShape copy() {
        return new SphereShape(center, radius);
    }

    @Override
    public boolean isValid() {
        return radius > 0 && center != null && 
               !Double.isNaN(center.x) && !Double.isNaN(center.y) && !Double.isNaN(center.z) &&
               !Double.isNaN(radius);
    }

    // Getter для радиуса
    public double getRadius() {
        return radius;
    }

    @Override
    public String toString() {
        return String.format("SphereShape{center=%.2f,%.2f,%.2f, radius=%.2f}", 
                           center.x, center.y, center.z, radius);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SphereShape that = (SphereShape) obj;
        return Double.compare(that.radius, radius) == 0 && center.equals(that.center);
    }

    @Override
    public int hashCode() {
        return center.hashCode() * 31 + Double.hashCode(radius);
    }
}