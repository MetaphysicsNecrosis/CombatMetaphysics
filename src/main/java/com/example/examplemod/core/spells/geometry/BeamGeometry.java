package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * Геометрия луча (BEAM)
 * Поддерживаемый луч энергии от заклинателя к цели
 */
public class BeamGeometry extends SpellGeometry {
    
    private final float width;
    private final float range;
    private final Vec3 endPoint;
    private final BeamType beamType;
    
    public BeamGeometry(SpellFormContext context) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("geometry_size", 0.3f),
            context.getParameter("geometry_size", 0.3f) > 0.5f ? GeometryType.THICK_BEAM : GeometryType.RAY
        );
        
        this.width = size;
        this.range = context.getParameter("range", 20.0f);
        this.endPoint = origin.add(direction.scale(range));
        this.beamType = determineBeamType(context);
    }
    
    private BeamType determineBeamType(SpellFormContext context) {
        if (context.hasElement("lightning")) return BeamType.ENERGY_BEAM;
        if (context.hasElement("fire")) return BeamType.FLAME_BEAM;
        if (context.hasElement("water")) return BeamType.FLUID_STREAM;
        return BeamType.BASIC_BEAM;
    }
    
    @Override
    public AABB getBoundingBox() {
        double minX = Math.min(origin.x, endPoint.x) - width;
        double minY = Math.min(origin.y, endPoint.y) - width;
        double minZ = Math.min(origin.z, endPoint.z) - width;
        
        double maxX = Math.max(origin.x, endPoint.x) + width;
        double maxY = Math.max(origin.y, endPoint.y) + width;
        double maxZ = Math.max(origin.z, endPoint.z) + width;
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        List<Vec3> points = new ArrayList<>();
        
        // Точки вдоль луча
        int segments = Math.max(5, (int)(range / width));
        for (int i = 0; i <= segments; i++) {
            Vec3 point = origin.add(direction.scale(range * i / segments));
            points.add(point);
        }
        
        return points;
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        // Проверяем пересечение с цилиндром луча
        Vec3 otherCenter = other.getCenter();
        double distanceToLine = distanceToLineSegment(origin, endPoint, otherCenter);
        
        if (distanceToLine <= width + other.getSize()) {
            Vec3 closestPoint = findClosestPointOnLine(origin, endPoint, otherCenter);
            Vec3 normal = otherCenter.subtract(closestPoint).normalize();
            double penetration = width + other.getSize() - distanceToLine;
            
            return CollisionResult.collision(closestPoint, normal, penetration, 
                CollisionResult.CollisionType.PARTIAL_OVERLAP);
        }
        
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        return distanceToLineSegment(origin, endPoint, point) <= width;
    }
    
    @Override
    public boolean intersects(AABB bounds) {
        return getBoundingBox().intersects(bounds);
    }
    
    @Override
    public void update(Vec3 newPosition, Vec3 newDirection) {
        this.origin = newPosition;
        this.direction = newDirection.normalize();
        // Пересчитываем конечную точку
        Vec3 newEndPoint = origin.add(direction.scale(range));
    }
    
    @Override
    public double getVolume() {
        return Math.PI * width * width * range; // Цилиндр
    }
    
    @Override
    public double getSurfaceArea() {
        return 2 * Math.PI * width * range; // Боковая поверхность цилиндра
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
    
    private Vec3 findClosestPointOnLine(Vec3 lineStart, Vec3 lineEnd, Vec3 point) {
        Vec3 line = lineEnd.subtract(lineStart);
        Vec3 pointToStart = point.subtract(lineStart);
        
        double lineLengthSq = line.lengthSqr();
        if (lineLengthSq == 0) return lineStart;
        
        double t = Math.max(0, Math.min(1, pointToStart.dot(line) / lineLengthSq));
        return lineStart.add(line.scale(t));
    }
    
    public float getWidth() { return width; }
    public float getRange() { return range; }
    public Vec3 getEndPoint() { return endPoint; }
    public BeamType getBeamType() { return beamType; }
    
    public enum BeamType {
        BASIC_BEAM,
        ENERGY_BEAM,
        FLAME_BEAM,  
        FLUID_STREAM
    }
}