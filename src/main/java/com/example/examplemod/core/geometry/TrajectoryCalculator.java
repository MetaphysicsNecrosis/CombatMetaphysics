package com.example.examplemod.core.geometry;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import com.example.examplemod.core.spells.parameters.SpellParameters;

import java.util.List;
import java.util.ArrayList;

/**
 * Калькулятор траекторий для подвижных заклинаний
 * Обрабатывает движение снарядов, лучей, волн и цепных заклинаний
 */
public class TrajectoryCalculator {

    /**
     * Рассчитать простую линейную траекторию
     */
    public static Trajectory calculateLinearTrajectory(Vec3 start, Vec3 direction, double speed, double maxRange) {
        Vec3 normalizedDirection = direction.normalize();
        double distance = Math.min(maxRange, start.distanceTo(start.add(direction)));
        
        List<Vec3> points = new ArrayList<>();
        int steps = Math.max(10, (int) (distance / 0.5)); // Точка каждые 0.5 блока
        
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3 point = start.add(normalizedDirection.scale(distance * progress));
            points.add(point);
        }
        
        return new Trajectory(points, distance, normalizedDirection);
    }

    /**
     * Рассчитать параболическую траекторию (с гравитацией)
     */
    public static Trajectory calculateParabolicTrajectory(Vec3 start, Vec3 velocity, double gravity, double maxTime) {
        List<Vec3> points = new ArrayList<>();
        
        double timeStep = 0.05; // 50ms шаги
        int steps = (int) (maxTime / timeStep);
        
        Vec3 currentPos = start;
        Vec3 currentVelocity = velocity;
        
        for (int i = 0; i <= steps; i++) {
            points.add(currentPos);
            
            // Обновить позицию
            currentPos = currentPos.add(currentVelocity.scale(timeStep));
            
            // Применить гравитацию
            currentVelocity = currentVelocity.add(0, -gravity * timeStep, 0);
            
            // Проверить на столкновение с землёй
            if (currentPos.y <= start.y - 50) { // Не падать слишком низко
                break;
            }
        }
        
        double totalDistance = start.distanceTo(points.get(points.size() - 1));
        Vec3 finalDirection = points.size() > 1 ? 
            points.get(points.size() - 1).subtract(points.get(points.size() - 2)).normalize() :
            Vec3.ZERO;
        
        return new Trajectory(points, totalDistance, finalDirection);
    }

    /**
     * Рассчитать траекторию с самонаведением
     */
    public static Trajectory calculateHomingTrajectory(Vec3 start, Entity target, SpellParameters params) {
        double speed = params.getFloat(SpellParameters.SPEED, 10f);
        double homingStrength = params.getFloat(SpellParameters.HOMING_STRENGTH, 0.5f);
        double maxRange = params.getFloat(SpellParameters.RANGE, 20f);
        
        List<Vec3> points = new ArrayList<>();
        Vec3 currentPos = start;
        Vec3 currentDirection = target.position().subtract(start).normalize();
        
        double remainingRange = maxRange;
        double stepSize = 0.25;
        
        while (remainingRange > 0 && currentPos.distanceTo(target.position()) > 1.0) {
            points.add(currentPos);
            
            // Обновить направление с учётом самонаведения
            Vec3 toTarget = target.position().subtract(currentPos).normalize();
            currentDirection = currentDirection.scale(1 - homingStrength)
                                             .add(toTarget.scale(homingStrength))
                                             .normalize();
            
            // Двигаться по текущему направлению
            Vec3 step = currentDirection.scale(stepSize);
            currentPos = currentPos.add(step);
            remainingRange -= stepSize;
        }
        
        points.add(currentPos); // Добавить финальную позицию
        
        double totalDistance = maxRange - remainingRange;
        return new Trajectory(points, totalDistance, currentDirection);
    }

    /**
     * Рассчитать волновую траекторию (расширяющееся кольцо)
     */
    public static WaveTrajectory calculateWaveTrajectory(Vec3 center, double initialRadius, double expansionSpeed, double maxRadius) {
        List<Double> radiusOverTime = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        double currentRadius = initialRadius;
        
        while (currentRadius <= maxRadius) {
            radiusOverTime.add(currentRadius);
            timestamps.add(System.currentTimeMillis() - startTime);
            
            currentRadius += expansionSpeed * 0.05; // 50ms шаги
        }
        
        return new WaveTrajectory(center, radiusOverTime, timestamps, maxRadius);
    }

    /**
     * Рассчитать цепную траекторию между целями
     */
    public static ChainTrajectory calculateChainTrajectory(Vec3 start, List<Entity> targets, double maxChainRange) {
        List<Vec3> chainPoints = new ArrayList<>();
        chainPoints.add(start);
        
        Vec3 currentPos = start;
        
        for (Entity target : targets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            
            // Проверить дистанцию
            if (currentPos.distanceTo(targetPos) <= maxChainRange) {
                chainPoints.add(targetPos);
                currentPos = targetPos;
            } else {
                break; // Цепь прерывается если цель слишком далеко
            }
        }
        
        return new ChainTrajectory(chainPoints, targets.subList(0, chainPoints.size() - 1));
    }

    /**
     * Базовый класс траектории
     */
    public static class Trajectory {
        private final List<Vec3> points;
        private final double totalDistance;
        private final Vec3 finalDirection;

        public Trajectory(List<Vec3> points, double totalDistance, Vec3 finalDirection) {
            this.points = new ArrayList<>(points);
            this.totalDistance = totalDistance;
            this.finalDirection = finalDirection;
        }

        public List<Vec3> getPoints() { return new ArrayList<>(points); }
        public double getTotalDistance() { return totalDistance; }
        public Vec3 getFinalDirection() { return finalDirection; }
        public Vec3 getStartPoint() { return points.isEmpty() ? Vec3.ZERO : points.get(0); }
        public Vec3 getEndPoint() { return points.isEmpty() ? Vec3.ZERO : points.get(points.size() - 1); }
        
        public Vec3 getPointAt(double progress) {
            if (points.isEmpty()) return Vec3.ZERO;
            
            progress = Math.max(0, Math.min(1, progress));
            int index = (int) (progress * (points.size() - 1));
            
            return points.get(Math.min(index, points.size() - 1));
        }
    }

    /**
     * Специализированная траектория для волн
     */
    public static class WaveTrajectory {
        private final Vec3 center;
        private final List<Double> radiusOverTime;
        private final List<Long> timestamps;
        private final double maxRadius;

        public WaveTrajectory(Vec3 center, List<Double> radiusOverTime, List<Long> timestamps, double maxRadius) {
            this.center = center;
            this.radiusOverTime = new ArrayList<>(radiusOverTime);
            this.timestamps = new ArrayList<>(timestamps);
            this.maxRadius = maxRadius;
        }

        public Vec3 getCenter() { return center; }
        public double getMaxRadius() { return maxRadius; }
        
        public double getRadiusAtTime(long timeOffset) {
            for (int i = 0; i < timestamps.size(); i++) {
                if (timestamps.get(i) >= timeOffset) {
                    return radiusOverTime.get(i);
                }
            }
            return maxRadius; // Максимальный радиус если время вышло
        }
    }

    /**
     * Траектория для цепных заклинаний
     */
    public static class ChainTrajectory {
        private final List<Vec3> chainPoints;
        private final List<Entity> targets;

        public ChainTrajectory(List<Vec3> chainPoints, List<Entity> targets) {
            this.chainPoints = new ArrayList<>(chainPoints);
            this.targets = new ArrayList<>(targets);
        }

        public List<Vec3> getChainPoints() { return new ArrayList<>(chainPoints); }
        public List<Entity> getTargets() { return new ArrayList<>(targets); }
        public int getChainLength() { return Math.max(0, chainPoints.size() - 1); }
        
        public Vec3 getLinkDirection(int linkIndex) {
            if (linkIndex < 0 || linkIndex >= chainPoints.size() - 1) {
                return Vec3.ZERO;
            }
            return chainPoints.get(linkIndex + 1).subtract(chainPoints.get(linkIndex)).normalize();
        }
    }
}