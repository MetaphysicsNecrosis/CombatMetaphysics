package com.example.examplemod.core.geometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Пространственный индекс для быстрого поиска заклинаний
 * Использует сетку для оптимизации коллизий в многопоточной среде
 */
public class SpatialIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpatialIndex.class);
    
    private final double cellSize;
    private final Map<GridCell, Set<IndexedSpell>> spatialGrid = new ConcurrentHashMap<>();
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();

    public SpatialIndex(double cellSize) {
        this.cellSize = Math.max(cellSize, 1.0); // Минимальный размер ячейки
    }

    /**
     * Добавить заклинание в индекс
     */
    public void addSpell(UUID spellId, SpellShape shape) {
        IndexedSpell indexedSpell = new IndexedSpell(spellId, shape);
        
        indexLock.writeLock().lock();
        try {
            // Найти все ячейки, которые пересекает заклинание
            Set<GridCell> cells = getCellsForShape(shape);
            
            for (GridCell cell : cells) {
                spatialGrid.computeIfAbsent(cell, k -> ConcurrentHashMap.newKeySet())
                          .add(indexedSpell);
            }
            
            LOGGER.trace("Added spell {} to {} grid cells", spellId, cells.size());
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * Удалить заклинание из индекса
     */
    public void removeSpell(UUID spellId) {
        indexLock.writeLock().lock();
        try {
            int removedCount = 0;
            var iterator = spatialGrid.entrySet().iterator();
            
            while (iterator.hasNext()) {
                var entry = iterator.next();
                Set<IndexedSpell> spells = entry.getValue();
                
                boolean removed = spells.removeIf(spell -> spell.spellId.equals(spellId));
                if (removed) removedCount++;
                
                // Удалить пустые ячейки
                if (spells.isEmpty()) {
                    iterator.remove();
                }
            }
            
            LOGGER.trace("Removed spell {} from {} grid cells", spellId, removedCount);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * Найти все заклинания в указанной области
     */
    public List<UUID> findSpellsInArea(AABB area) {
        Set<UUID> foundSpells = new HashSet<>();
        
        indexLock.readLock().lock();
        try {
            Set<GridCell> searchCells = getCellsForAABB(area);
            
            for (GridCell cell : searchCells) {
                Set<IndexedSpell> cellSpells = spatialGrid.get(cell);
                if (cellSpells != null) {
                    for (IndexedSpell spell : cellSpells) {
                        if (spell.shape.getBoundingBox().intersects(area)) {
                            foundSpells.add(spell.spellId);
                        }
                    }
                }
            }
        } finally {
            indexLock.readLock().unlock();
        }
        
        return new ArrayList<>(foundSpells);
    }

    /**
     * Найти заклинания, пересекающиеся с данной формой
     */
    public List<UUID> findIntersectingSpells(SpellShape queryShape) {
        Set<UUID> foundSpells = new HashSet<>();
        
        indexLock.readLock().lock();
        try {
            Set<GridCell> searchCells = getCellsForShape(queryShape);
            
            for (GridCell cell : searchCells) {
                Set<IndexedSpell> cellSpells = spatialGrid.get(cell);
                if (cellSpells != null) {
                    for (IndexedSpell spell : cellSpells) {
                        if (spell.shape.intersects(queryShape)) {
                            foundSpells.add(spell.spellId);
                        }
                    }
                }
            }
        } finally {
            indexLock.readLock().unlock();
        }
        
        return new ArrayList<>(foundSpells);
    }

    /**
     * Найти ближайшие заклинания к точке
     */
    public List<SpellDistance> findNearestSpells(Vec3 point, double maxDistance, int maxCount) {
        List<SpellDistance> nearbySpells = new ArrayList<>();
        
        indexLock.readLock().lock();
        try {
            // Создать область поиска
            AABB searchArea = new AABB(
                point.x - maxDistance, point.y - maxDistance, point.z - maxDistance,
                point.x + maxDistance, point.y + maxDistance, point.z + maxDistance
            );
            
            Set<GridCell> searchCells = getCellsForAABB(searchArea);
            
            for (GridCell cell : searchCells) {
                Set<IndexedSpell> cellSpells = spatialGrid.get(cell);
                if (cellSpells != null) {
                    for (IndexedSpell spell : cellSpells) {
                        double distance = point.distanceTo(spell.shape.getCenter());
                        if (distance <= maxDistance) {
                            nearbySpells.add(new SpellDistance(spell.spellId, distance));
                        }
                    }
                }
            }
            
            // Отсортировать по расстоянию и ограничить количество
            nearbySpells.sort(Comparator.comparingDouble(SpellDistance::distance));
            if (nearbySpells.size() > maxCount) {
                nearbySpells = nearbySpells.subList(0, maxCount);
            }
            
        } finally {
            indexLock.readLock().unlock();
        }
        
        return nearbySpells;
    }

    /**
     * Получить ячейки сетки для формы
     */
    private Set<GridCell> getCellsForShape(SpellShape shape) {
        return getCellsForAABB(shape.getBoundingBox());
    }

    /**
     * Получить ячейки сетки для AABB
     */
    private Set<GridCell> getCellsForAABB(AABB bounds) {
        Set<GridCell> cells = new HashSet<>();
        
        int minX = (int) Math.floor(bounds.minX / cellSize);
        int maxX = (int) Math.floor(bounds.maxX / cellSize);
        int minY = (int) Math.floor(bounds.minY / cellSize);
        int maxY = (int) Math.floor(bounds.maxY / cellSize);
        int minZ = (int) Math.floor(bounds.minZ / cellSize);
        int maxZ = (int) Math.floor(bounds.maxZ / cellSize);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cells.add(new GridCell(x, y, z));
                }
            }
        }
        
        return cells;
    }

    /**
     * Очистить весь индекс
     */
    public void clear() {
        indexLock.writeLock().lock();
        try {
            spatialGrid.clear();
            LOGGER.debug("Cleared spatial index");
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * Получить статистику индекса
     */
    public IndexStats getStats() {
        indexLock.readLock().lock();
        try {
            int totalCells = spatialGrid.size();
            int totalSpells = spatialGrid.values().stream()
                                       .mapToInt(Set::size)
                                       .sum();
            
            double avgSpellsPerCell = totalCells > 0 ? (double) totalSpells / totalCells : 0;
            
            return new IndexStats(totalCells, totalSpells, avgSpellsPerCell, cellSize);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Ячейка пространственной сетки
     */
    private record GridCell(int x, int y, int z) {}

    /**
     * Заклинание в индексе
     */
    private record IndexedSpell(UUID spellId, SpellShape shape) {}

    /**
     * Результат поиска с расстоянием
     */
    public record SpellDistance(UUID spellId, double distance) {}

    /**
     * Статистика индекса
     */
    public record IndexStats(
        int totalCells,
        int totalSpells,
        double averageSpellsPerCell,
        double cellSize
    ) {}
}