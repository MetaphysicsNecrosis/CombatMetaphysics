package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * Геометрия на основе N-угольников согласно концепту
 * Используется для BARRIER и AREA заклинаний
 * 
 * Особые параметры:
 * - GeometryType — хитбокс заклинания (фиксированный, балансируемый глобально)
 * - ReflectChanceModifier — модификатор отражения для формы
 * - CritChanceModifier — модификатор критического урона
 */
public class PolygonalGeometry extends SpellGeometry {
    
    private final int sides;              // Количество сторон N-угольника
    private final List<Vec3> vertices;    // Вершины полигона
    private final float height;           // Высота для 3D структур
    private final boolean is3D;           // 3D структура или 2D зона
    private final PolygonType polygonType;
    
    // Фиксированные параметры (балансируются глобально)
    private final float reflectChanceModifier;
    private final float critChanceModifier;
    
    public PolygonalGeometry(SpellFormContext context, boolean is3D) {
        super(
            context.origin(),
            context.direction(),
            context.getParameter("radius", 3.0f),
            is3D ? GeometryType.POLYGON : GeometryType.POLYGON_2D
        );
        
        this.is3D = is3D;
        this.sides = determineSides(context);
        this.height = is3D ? context.getParameter("geometry_size", size) : 0.1f;
        this.polygonType = determinePolygonType(context);
        
        // Создаем вершины N-угольника
        this.vertices = generatePolygonVertices();
        
        // Фиксированные параметры по типу геометрии
        this.reflectChanceModifier = calculateReflectModifier();
        this.critChanceModifier = calculateCritModifier();
    }
    
    /**
     * Определить количество сторон на основе параметров
     */
    private int determineSides(SpellFormContext context) {
        float complexity = context.getParameter("geometry_complexity", 0.0f);
        float elements = (float) context.elements().values().stream().mapToDouble(f -> f).sum();
        
        if (complexity > 0.8f || elements > 3.0f) return 8;  // Восьмиугольник для сложных
        if (complexity > 0.5f || elements > 2.0f) return 6;  // Шестиугольник
        if (complexity > 0.2f || elements > 1.0f) return 5;  // Пятиугольник
        return 4; // Квадрат по умолчанию
    }
    
    /**
     * Определить тип полигона
     */
    private PolygonType determinePolygonType(SpellFormContext context) {
        if (context.hasElement("earth")) return PolygonType.CRYSTAL_STRUCTURE;
        if (context.hasElement("fire")) return PolygonType.FLAME_PATTERN;
        if (context.hasElement("water")) return PolygonType.FLUID_BOUNDARY;
        if (context.hasElement("lightning")) return PolygonType.ENERGY_GRID;
        return PolygonType.BASIC_POLYGON;
    }
    
    /**
     * Сгенерировать вершины N-угольника
     */
    private List<Vec3> generatePolygonVertices() {
        List<Vec3> vertices = new ArrayList<>();
        double angleStep = 2 * Math.PI / sides;
        
        // Вычисляем плоскость перпендикулярную направлению
        Vec3 up = Math.abs(direction.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = direction.cross(up).normalize();
        Vec3 forward = right.cross(direction).normalize();
        
        for (int i = 0; i < sides; i++) {
            double angle = i * angleStep;
            double x = Math.cos(angle) * size;
            double z = Math.sin(angle) * size;
            
            Vec3 vertex = origin
                .add(right.scale(x))
                .add(forward.scale(z));
                
            vertices.add(vertex);
        }
        
        return vertices;
    }
    
    @Override
    public AABB getBoundingBox() {
        if (vertices.isEmpty()) {
            return new AABB(origin.x - size, origin.y - size, origin.z - size,
                           origin.x + size, origin.y + size, origin.z + size);
        }
        
        double minX = vertices.stream().mapToDouble(v -> v.x).min().orElse(origin.x - size);
        double minY = vertices.stream().mapToDouble(v -> v.y).min().orElse(origin.y - height/2);
        double minZ = vertices.stream().mapToDouble(v -> v.z).min().orElse(origin.z - size);
        
        double maxX = vertices.stream().mapToDouble(v -> v.x).max().orElse(origin.x + size);
        double maxY = vertices.stream().mapToDouble(v -> v.y).max().orElse(origin.y + height/2);
        double maxZ = vertices.stream().mapToDouble(v -> v.z).max().orElse(origin.z + size);
        
        if (is3D) {
            minY -= height / 2;
            maxY += height / 2;
        }
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    @Override
    public VoxelShape getCollisionShape() {
        return Shapes.create(getBoundingBox());
    }
    
    @Override
    public List<Vec3> getCollisionPoints() {
        List<Vec3> points = new ArrayList<>(vertices);
        
        if (is3D) {
            // Добавляем верхние вершины для 3D структуры
            for (Vec3 vertex : vertices) {
                points.add(vertex.add(0, height, 0));
            }
        }
        
        // Добавляем центр
        points.add(origin);
        
        return points;
    }
    
    @Override
    public CollisionResult checkCollision(SpellGeometry other) {
        // Базовая проверка через AABB
        AABB ourBounds = getBoundingBox();
        AABB otherBounds = other.getBoundingBox();
        
        if (!ourBounds.intersects(otherBounds)) {
            return CollisionResult.noCollision();
        }
        
        // Для точной проверки используем point-in-polygon
        if (isPointInside(other.getCenter())) {
            Vec3 collisionPoint = findNearestPointOnBoundary(other.getCenter());
            Vec3 normal = other.getCenter().subtract(collisionPoint).normalize();
            double penetration = other.getSize();
            
            return CollisionResult.collision(collisionPoint, normal, penetration, 
                CollisionResult.CollisionType.PARTIAL_OVERLAP);
        }
        
        return CollisionResult.noCollision();
    }
    
    @Override
    public boolean intersects(Vec3 point) {
        return isPointInside(point);
    }
    
    @Override
    public boolean intersects(AABB bounds) {
        return getBoundingBox().intersects(bounds);
    }
    
    /**
     * Проверить, находится ли точка внутри N-угольника
     */
    private boolean isPointInside(Vec3 point) {
        if (vertices.size() < 3) return false;
        
        // Проверяем высоту для 3D структур
        if (is3D) {
            if (point.y < origin.y - height/2 || point.y > origin.y + height/2) {
                return false;
            }
        }
        
        // Ray casting algorithm для 2D проекции
        int intersections = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get((i + 1) % vertices.size());
            
            if (rayIntersectsSegment(point, v1, v2)) {
                intersections++;
            }
        }
        
        return intersections % 2 == 1;
    }
    
    /**
     * Проверить пересечение луча с отрезком (для ray casting)
     */
    private boolean rayIntersectsSegment(Vec3 point, Vec3 v1, Vec3 v2) {
        if (v1.z > point.z == v2.z > point.z) return false;
        
        double slope = (point.z - v1.z) / (v2.z - v1.z);
        double intersectX = v1.x + slope * (v2.x - v1.x);
        
        return intersectX > point.x;
    }
    
    /**
     * Найти ближайшую точку на границе полигона
     */
    private Vec3 findNearestPointOnBoundary(Vec3 point) {
        Vec3 nearest = vertices.get(0);
        double minDistance = point.distanceTo(nearest);
        
        for (int i = 0; i < vertices.size(); i++) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get((i + 1) % vertices.size());
            
            Vec3 edgePoint = findNearestPointOnEdge(point, v1, v2);
            double distance = point.distanceTo(edgePoint);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = edgePoint;
            }
        }
        
        return nearest;
    }
    
    /**
     * Найти ближайшую точку на грани
     */
    private Vec3 findNearestPointOnEdge(Vec3 point, Vec3 edgeStart, Vec3 edgeEnd) {
        Vec3 edge = edgeEnd.subtract(edgeStart);
        Vec3 pointToStart = point.subtract(edgeStart);
        
        double edgeLengthSq = edge.lengthSqr();
        if (edgeLengthSq == 0) return edgeStart;
        
        double t = Math.max(0, Math.min(1, pointToStart.dot(edge) / edgeLengthSq));
        return edgeStart.add(edge.scale(t));
    }
    
    @Override
    public void update(Vec3 newPosition, Vec3 newDirection) {
        Vec3 offset = newPosition.subtract(origin);
        this.origin = newPosition;
        this.direction = newDirection.normalize();
        
        // Обновляем вершины
        for (int i = 0; i < vertices.size(); i++) {
            vertices.set(i, vertices.get(i).add(offset));
        }
    }
    
    @Override
    public double getVolume() {
        double baseArea = calculatePolygonArea();
        return is3D ? baseArea * height : baseArea * 0.1; // Толщина для 2D
    }
    
    @Override
    public double getSurfaceArea() {
        double baseArea = calculatePolygonArea();
        if (!is3D) return baseArea;
        
        double sideArea = calculatePerimeter() * height;
        return 2 * baseArea + sideArea; // Верх + низ + боковые стороны
    }
    
    /**
     * Вычислить площадь N-угольника
     */
    private double calculatePolygonArea() {
        if (vertices.size() < 3) return 0;
        
        double area = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get((i + 1) % vertices.size());
            area += v1.x * v2.z - v2.x * v1.z;
        }
        return Math.abs(area) / 2.0;
    }
    
    /**
     * Вычислить периметр полигона
     */
    private double calculatePerimeter() {
        double perimeter = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get((i + 1) % vertices.size());
            perimeter += v1.distanceTo(v2);
        }
        return perimeter;
    }
    
    /**
     * Вычислить модификатор отражения на основе геометрии
     */
    private float calculateReflectModifier() {
        return switch (polygonType) {
            case CRYSTAL_STRUCTURE -> 1.5f;  // Кристаллы отражают лучше
            case ENERGY_GRID -> 1.2f;        // Энергетические структуры
            case FLUID_BOUNDARY -> 0.8f;     // Жидкость поглощает
            case FLAME_PATTERN -> 0.6f;      // Огонь рассеивает
            case BASIC_POLYGON -> 1.0f;      // Базовое отражение
        };
    }
    
    /**
     * Вычислить модификатор критического урона
     */
    private float calculateCritModifier() {
        float baseMod = 1.0f + (sides - 4) * 0.1f; // Больше сторон = выше крит
        
        return switch (polygonType) {
            case CRYSTAL_STRUCTURE -> baseMod * 1.3f;
            case ENERGY_GRID -> baseMod * 1.2f;
            case FLAME_PATTERN -> baseMod * 1.4f;
            case FLUID_BOUNDARY -> baseMod * 0.9f;
            case BASIC_POLYGON -> baseMod;
        };
    }
    
    // Геттеры для особых параметров
    public int getSides() { return sides; }
    public boolean is3D() { return is3D; }
    public float getHeight() { return height; }
    public List<Vec3> getVertices() { return new ArrayList<>(vertices); }
    public float getReflectChanceModifier() { return reflectChanceModifier; }
    public float getCritChanceModifier() { return critChanceModifier; }
    public PolygonType getPolygonType() { return polygonType; }
    
    /**
     * Типы полигональной геометрии
     */
    public enum PolygonType {
        BASIC_POLYGON,      // Базовый N-угольник
        CRYSTAL_STRUCTURE,  // Кристаллическая структура (земля)
        FLAME_PATTERN,      // Огненный узор (огонь)  
        FLUID_BOUNDARY,     // Граница жидкости (вода)
        ENERGY_GRID        // Энергетическая сетка (молния)
    }
}