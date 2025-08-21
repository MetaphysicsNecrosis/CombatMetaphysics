package com.example.examplemod.util;

import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * WorldEdit-inspired Block Change Buffer
 * Использует прямую модификацию chunk data вместо world.setBlock() для максимальной производительности
 * 
 * АРХИТЕКТУРА:
 * 1. Накопление всех изменений в памяти БЕЗ применения
 * 2. Группировка по чанкам и секциям
 * 3. Прямая модификация chunk data в обход world.setBlock()
 * 4. Один пакет на chunk section вместо пакета на блок
 * 5. Deferred lighting и physics
 */
public class WorldEditStyleBlockBuffer {
    
    private final Level world;
    private final CombatSideEffects sideEffects;
    
    // Группировка изменений по чанкам (как в WorldEdit ChunkBatchingExtent)
    private final Map<ChunkPos, ChunkChanges> chunkMap = new HashMap<>();
    
    // Deferred операции
    private final List<ItemEntity> deferredDrops = new ArrayList<>();
    private final Set<ChunkPos> deferredLightingChunks = new HashSet<>();
    private final Set<ChunkPos> deferredPhysicsChunks = new HashSet<>();
    
    public WorldEditStyleBlockBuffer(Level world, CombatSideEffects sideEffects) {
        this.world = world;
        this.sideEffects = sideEffects;
    }
    
    /**
     * Добавляет блок в очередь на изменение (НЕ применяет сразу!)
     * Аналог WorldEdit BlockMap.put()
     */
    public void queueBlockDestruction(BlockPos pos, BlockState currentState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        // Получаем или создаём ChunkChanges для этого чанка
        ChunkChanges chunkChanges = chunkMap.computeIfAbsent(chunkPos, k -> new ChunkChanges(chunkPos));
        
        // Добавляем изменение
        BlockChange change = new BlockChange(pos, currentState, Blocks.AIR.defaultBlockState());
        chunkChanges.addChange(change);
        
        // Планируем deferred операции
        if (sideEffects.shouldUpdateLighting()) {
            deferredLightingChunks.add(chunkPos);
        }
        
        if (sideEffects.shouldUpdatePhysics()) {
            deferredPhysicsChunks.add(chunkPos);
        }
        
        // Собираем дропы для deferred spawning
        if (sideEffects.shouldSpawnDrops() && world instanceof ServerLevel serverLevel) {
            List<ItemStack> drops = Block.getDrops(currentState, serverLevel, pos, null);
            drops.forEach(stack -> {
                ItemEntity entity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                deferredDrops.add(entity);
            });
        }
    }
    
    /**
     * Применяет ВСЕ накопленные изменения по WorldEdit архитектуре
     */
    public FlushResult flush() {
        if (chunkMap.isEmpty()) {
            return new FlushResult(0, 0, 0);
        }
        
        long startTime = System.currentTimeMillis();
        
        CombatMetaphysics.LOGGER.info("WorldEditStyleBuffer: Starting flush - {} chunks with {} total changes", 
            chunkMap.size(), chunkMap.values().stream().mapToInt(chunk -> chunk.changes.size()).sum());
        
        int totalChanges = 0;
        int chunksModified = 0;
        int blocksDestroyed = 0;
        
        // PHASE 1: Direct chunk data modification (как в WorldEdit)
        List<ChunkPos> sortedChunks = sortChunksOptimally(chunkMap.keySet());
        
        for (ChunkPos chunkPos : sortedChunks) {
            ChunkChanges chunkChanges = chunkMap.get(chunkPos);
            
            try {
                // Загружаем чанк ОДИН раз
                LevelChunk levelChunk = world.getChunk(chunkPos.x, chunkPos.z);
                
                // ПРЯМАЯ МОДИФИКАЦИЯ CHUNK DATA (обходим world.setBlock)
                int destroyed = applyDirectChunkModification(levelChunk, chunkChanges);
                blocksDestroyed += destroyed;
                
                // BATCH PACKET SENDING - один пакет на чанк
                if (sideEffects.shouldSendPackets()) {
                    sendChunkSectionUpdates(levelChunk, chunkChanges);
                }
                
                totalChanges += chunkChanges.changes.size();
                chunksModified++;
                
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.warn("Failed to flush chunk {}: {}", chunkPos, e.getMessage());
            }
        }
        
        // PHASE 2: Deferred drops (батчами, как рекомендовано)
        if (!deferredDrops.isEmpty()) {
            spawnDeferredDropsInBatches();
        }
        
        // PHASE 3: Deferred lighting и physics (асинхронно)
        if (!deferredLightingChunks.isEmpty() || !deferredPhysicsChunks.isEmpty()) {
            scheduleDeferredUpdates();
        }
        
        // Очищаем буферы
        clear();
        
        long duration = System.currentTimeMillis() - startTime;
        CombatMetaphysics.LOGGER.info("WorldEditStyleBuffer: Flush completed in {}ms - {} blocks in {} chunks", 
            duration, blocksDestroyed, chunksModified);
        
        return new FlushResult(totalChanges, chunksModified, blocksDestroyed);
    }
    
    /**
     * КЛЮЧЕВОЙ МЕТОД: Прямая модификация chunk data в обход world.setBlock()
     * Аналог WorldEdit direct chunk access
     */
    private int applyDirectChunkModification(LevelChunk levelChunk, ChunkChanges chunkChanges) {
        int blocksChanged = 0;
        
        // Группируем изменения по секциям чанка (16x16x16 блоков)
        Map<Integer, List<BlockChange>> sectionChanges = new HashMap<>();
        
        for (BlockChange change : chunkChanges.changes) {
            int sectionY = (change.pos.getY() + 64) >> 4; // NeoForge 1.21.1: Y offset +64
            sectionChanges.computeIfAbsent(sectionY, k -> new ArrayList<>()).add(change);
        }
        
        // Применяем изменения по секциям
        for (Map.Entry<Integer, List<BlockChange>> entry : sectionChanges.entrySet()) {
            int sectionIndex = entry.getKey();
            List<BlockChange> changes = entry.getValue();
            
            try {
                // Получаем секцию чанка
                LevelChunkSection section = levelChunk.getSection(sectionIndex);
                
                // Прямая модификация состояний блоков
                for (BlockChange change : changes) {
                    int localX = change.pos.getX() & 15;
                    int localY = change.pos.getY() & 15;
                    int localZ = change.pos.getZ() & 15;
                    
                    // ОБХОДИМ world.setBlock() - прямая модификация
                    section.setBlockState(localX, localY, localZ, change.newState, false);
                    blocksChanged++;
                }
                
                // Помечаем секцию как изменённую
                section.recalcBlockCounts();
                
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.warn("Failed to modify section {} in chunk {}: {}", 
                    sectionIndex, levelChunk.getPos(), e.getMessage());
            }
        }
        
        // Помечаем чанк как изменённый для сохранения
        levelChunk.markUnsaved();
        
        return blocksChanged;
    }
    
    /**
     * Отправка ОДНОГО пакета на chunk section вместо множества BlockUpdate пакетов
     * Аналог WorldEdit chunk packet batching
     */
    private void sendChunkSectionUpdates(LevelChunk chunk, ChunkChanges changes) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        
        try {
            // Группируем изменения по секциям для пакетов
            Map<Integer, List<BlockChange>> sectionUpdates = new HashMap<>();
            
            for (BlockChange change : changes.changes) {
                int sectionY = (change.pos.getY() + 64) >> 4;
                sectionUpdates.computeIfAbsent(sectionY, k -> new ArrayList<>()).add(change);
            }
            
            // Отправляем один пакет на секцию
            for (Map.Entry<Integer, List<BlockChange>> entry : sectionUpdates.entrySet()) {
                // TODO: Реализовать отправку ClientboundSectionBlocksUpdatePacket
                // Требует больше знаний о NeoForge packet API
                CombatMetaphysics.LOGGER.debug("Would send section update packet for section {} with {} blocks", 
                    entry.getKey(), entry.getValue().size());
            }
            
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.warn("Failed to send chunk updates for {}: {}", chunk.getPos(), e.getMessage());
        }
    }
    
    /**
     * Spawn deferred drops в батчах для предотвращения lag spikes
     */
    private void spawnDeferredDropsInBatches() {
        final int DROP_BATCH_SIZE = 50;
        
        for (int i = 0; i < deferredDrops.size(); i += DROP_BATCH_SIZE) {
            int endIndex = Math.min(i + DROP_BATCH_SIZE, deferredDrops.size());
            List<ItemEntity> batch = deferredDrops.subList(i, endIndex);
            
            batch.forEach(world::addFreshEntity);
        }
        
        CombatMetaphysics.LOGGER.debug("Spawned {} deferred drops in batches of {}", 
            deferredDrops.size(), DROP_BATCH_SIZE);
    }
    
    /**
     * Асинхронное выполнение deferred lighting и physics updates
     * Аналог WorldEdit fixAfterFastMode()
     */
    private void scheduleDeferredUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                // Небольшая задержка чтобы клиент успел отрисовать визуальные изменения
                Thread.sleep(50);
                
                // Lighting updates
                if (world instanceof ServerLevel serverLevel) {
                    for (ChunkPos chunkPos : deferredLightingChunks) {
                        try {
                            serverLevel.getChunkSource().getLightEngine().checkBlock(
                                chunkPos.getWorldPosition());
                        } catch (Exception e) {
                            CombatMetaphysics.LOGGER.warn("Failed deferred lighting for {}: {}", 
                                chunkPos, e.getMessage());
                        }
                    }
                }
                
                // Physics updates (neighbor notifications)
                // TODO: Реализовать deferred neighbor updates
                
                CombatMetaphysics.LOGGER.debug("Completed deferred updates for {} lighting chunks, {} physics chunks", 
                    deferredLightingChunks.size(), deferredPhysicsChunks.size());
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Сортировка чанков для оптимального обхода (аналог WorldEdit RegionOptimizedVectorSorter)
     * Минимизирует chunk loading/unloading
     */
    private List<ChunkPos> sortChunksOptimally(Set<ChunkPos> chunks) {
        List<ChunkPos> sorted = new ArrayList<>(chunks);
        
        // Простая сортировка по X, потом по Z для locality
        sorted.sort((a, b) -> {
            int xCompare = Integer.compare(a.x, b.x);
            return xCompare != 0 ? xCompare : Integer.compare(a.z, b.z);
        });
        
        return sorted;
    }
    
    public void clear() {
        chunkMap.clear();
        deferredDrops.clear();
        deferredLightingChunks.clear();
        deferredPhysicsChunks.clear();
    }
    
    /**
     * Информация об изменениях в одном чанке
     */
    private static class ChunkChanges {
        final ChunkPos chunkPos;
        final List<BlockChange> changes = new ArrayList<>();
        
        ChunkChanges(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
        }
        
        void addChange(BlockChange change) {
            changes.add(change);
        }
    }
    
    /**
     * Представляет одно изменение блока
     */
    private record BlockChange(BlockPos pos, BlockState oldState, BlockState newState) {}
    
    /**
     * Результат flush операции
     */
    public record FlushResult(int totalChanges, int chunksModified, int blocksDestroyed) {}
}