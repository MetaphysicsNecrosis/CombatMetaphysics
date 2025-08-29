package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.instances.SpellInstance;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Кэш состояний заклинаний для быстрого доступа
 * Оптимизированный для частых запросов в многопоточной среде
 */
public class SpellStateCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellStateCache.class);
    
    private final Map<UUID, CachedSpellState> cache = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> playerSpells = new ConcurrentHashMap<>();
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * Кэшированное состояние заклинания
     */
    private record CachedSpellState(
        UUID spellId,
        UUID playerId,
        String spellType,
        String currentState,
        float manaReserved,
        long lastUpdate,
        long cacheTime
    ) {}

    /**
     * Обновить кэш для заклинания
     */
    public void updateCache(SpellInstance spell) {
        CachedSpellState cachedState = new CachedSpellState(
            spell.getId(),
            spell.getCaster().getUUID(),
            spell.getDefinition().id().toString(),
            spell.getState().getId(),
            spell.getCurrentManaReserved(),
            spell.getLastUpdateTime(),
            System.currentTimeMillis()
        );

        cache.put(spell.getId(), cachedState);
        
        // Обновить индекс по игрокам
        playerSpells.computeIfAbsent(spell.getCaster().getUUID(), k -> new ArrayList<>())
                   .add(spell.getId());

        LOGGER.trace("Updated cache for spell {}", spell.getId());
    }

    /**
     * Получить кэшированное состояние заклинания
     */
    public CachedSpellState getCachedState(UUID spellId) {
        CachedSpellState state = cache.get(spellId);
        if (state != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return state;
    }

    /**
     * Получить все заклинания игрока
     */
    public List<UUID> getPlayerSpells(Player player) {
        return getPlayerSpells(player.getUUID());
    }

    public List<UUID> getPlayerSpells(UUID playerId) {
        List<UUID> spells = playerSpells.get(playerId);
        return spells != null ? new ArrayList<>(spells) : new ArrayList<>();
    }

    /**
     * Проверить, есть ли заклинание в кэше
     */
    public boolean hasSpell(UUID spellId) {
        boolean has = cache.containsKey(spellId);
        if (has) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return has;
    }

    /**
     * Удалить заклинание из кэша
     */
    public void removeSpell(UUID spellId) {
        CachedSpellState removed = cache.remove(spellId);
        if (removed != null) {
            // Удалить из индекса игрока
            List<UUID> spells = playerSpells.get(removed.playerId);
            if (spells != null) {
                spells.remove(spellId);
                if (spells.isEmpty()) {
                    playerSpells.remove(removed.playerId);
                }
            }
            LOGGER.trace("Removed spell {} from cache", spellId);
        }
    }

    /**
     * Очистить весь кэш
     */
    public void clear() {
        cache.clear();
        playerSpells.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        LOGGER.debug("Cleared spell state cache");
    }

    /**
     * Получить статистику кэша
     */
    public CacheStats getStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRatio = total > 0 ? (double) hits / total : 0.0;
        
        return new CacheStats(hits, misses, hitRatio, cache.size());
    }

    public record CacheStats(
        long hits,
        long misses,
        double hitRatio,
        int cacheSize
    ) {}
}