package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * Геометрия цепи (CHAIN) - линия между целями
 */
public class ChainGeometry extends SpellGeometry {
    
    private final List<Vec3> chainPoints;
    private final float chainThickness;
    
    public ChainGeometry(SpellFormContext context) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("chain_thickness", 0.3f),
            GeometryType.CHAIN_LINE
        );
        
        this.chainThickness = size;
        this.chainPoints = new ArrayList<>();
        chainPoints.add(origin);
        
        // Добавляем начальную цель
        if (context.target() != null) {
            chainPoints.add(context.target().position());
        }
    }
    
    @Override
    public AABB getBoundingBox() {
        if (chainPoints.isEmpty()) {
            return new AABB(origin.x, origin.y, origin.z, origin.x, origin.y, origin.z);
        }
        
        double minX = chainPoints.stream().mapToDouble(p -> p.x).min().orElse(origin.x) - chainThickness;
        double minY = chainPoints.stream().mapToDouble(p -> p.y).min().orElse(origin.y) - chainThickness;
        double minZ = chainPoints.stream().mapToDouble(p -> p.z).min().orElse(origin.z) - chainThickness;
        
        double maxX = chainPoints.stream().mapToDouble(p -> p.x).max().orElse(origin.x) + chainThickness;
        double maxY = chainPoints.stream().mapToDouble(p -> p.y).max().orElse(origin.y) + chainThickness;
        double maxZ = chainPoints.stream().mapToDouble(p -> p.z).max().orElse(origin.z) + chainThickness;
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        return new ArrayList<>(chainPoints);
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        Vec3 otherCenter = other.getCenter();
        
        // Проверяем коллизию с каждым сегментом цепи
        for (int i = 0; i < chainPoints.size() - 1; i++) {
            Vec3 start = chainPoints.get(i);
            Vec3 end = chainPoints.get(i + 1);
            
            double distance = distanceToLineSegment(start, end, otherCenter);
            if (distance <= chainThickness + other.getSize()) {
                Vec3 closestPoint = findClosestPointOnLine(start, end, otherCenter);
                Vec3 normal = otherCenter.subtract(closestPoint).normalize();
                double penetration = chainThickness + other.getSize() - distance;
                
                return CollisionResult.collision(closestPoint, normal, penetration, CollisionResult.CollisionType.PARTIAL_OVERLAP);
            }
        }
        
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        for (int i = 0; i < chainPoints.size() - 1; i++) {
            Vec3 start = chainPoints.get(i);
            Vec3 end = chainPoints.get(i + 1);
            
            if (distanceToLineSegment(start, end, point) <= chainThickness) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean intersects(AABB bounds) {
        return getBoundingBox().intersects(bounds);
    }
    
    @Override
    public void update(Vec3 newPosition, Vec3 newDirection) {
        // Цепи могут добавлять новые точки
    }
    
    @Override
    public double getVolume() {
        double totalVolume = 0;
        for (int i = 0; i < chainPoints.size() - 1; i++) {
            Vec3 start = chainPoints.get(i);
            Vec3 end = chainPoints.get(i + 1);
            double length = start.distanceTo(end);
            totalVolume += Math.PI * chainThickness * chainThickness * length;
        }
        return totalVolume;
    }
    
    @Override
    public double getSurfaceArea() {
        double totalArea = 0;
        for (int i = 0; i < chainPoints.size() - 1; i++) {
            Vec3 start = chainPoints.get(i);
            Vec3 end = chainPoints.get(i + 1);
            double length = start.distanceTo(end);
            totalArea += 2 * Math.PI * chainThickness * length;
        }
        return totalArea;
    }
    
    /**
     * Добавить новую точку в цепь
     */
    public void addChainPoint(Vec3 point) {
        chainPoints.add(point);
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
}