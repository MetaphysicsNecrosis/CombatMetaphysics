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
import net.minecraft.world.entity.item.ItemEntity;

import java.util.*;

/**
 * WorldEdit/FAWE inspired Block Change Buffer Pattern
 * Группирует изменения блоков по чанкам и применяет их batch'ами
 * для минимизации lag spikes при массовых операциях
 */
public class BlockChangeBuffer {
    
    private final Level world;
    private final Map<ChunkPos, List<BlockChange>> pendingChanges = new HashMap<>();
    private final List<ItemEntity> deferredDrops = new ArrayList<>();
    
    // Рекомендованные параметры от Gemini
    private static final int BATCH_SIZE = 2000; // блоков за тик
    private static final int VISUAL_FLAGS = 2; // визуальные обновления
    private static final int PHYS_QUOTA = 64; // физические обновления за тик
    private static final long TIME_BUDGET_MS = 40; // максимальное время на обработку
    
    public BlockChangeBuffer(Level world) {
        this.world = world;
    }
    
    /**
     * Добавляет блок в очередь для изменения (без немедленного применения)
     */
    public void queueBlockDestruction(BlockPos pos, BlockState currentState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        // Добавляем в очередь
        pendingChanges.computeIfAbsent(chunkPos, k -> new ArrayList<>())
                     .add(new BlockChange(pos, currentState, Blocks.AIR.defaultBlockState()));
        
        // Собираем дропы заранее (если это ServerLevel)
        if (world instanceof ServerLevel serverLevel) {
            List<ItemStack> drops = Block.getDrops(currentState, serverLevel, pos, null);
            drops.forEach(stack -> {
                ItemEntity entity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                deferredDrops.add(entity);
            });
        }
    }
    
    /**
     * Применяет накопленные изменения batch операциями с ограничениями производительности
     * Исправлено: deferred drops + light/physics updates + visual flags
     */
    public FlushResult flush() {
        if (pendingChanges.isEmpty()) {
            return new FlushResult(0, 0, 0);
        }
        
        long startTime = System.currentTimeMillis();
        
        CombatMetaphysics.LOGGER.info("BlockChangeBuffer: Starting optimized flush - {} chunks with {} total changes", 
            pendingChanges.size(), pendingChanges.values().stream().mapToInt(List::size).sum());
        
        int totalChanges = 0;
        int chunksModified = 0;
        int blocksDestroyed = 0;
        int processedBlocks = 0;
        
        // Обрабатываем блоки batch'ами размером BATCH_SIZE
        for (Map.Entry<ChunkPos, List<BlockChange>> entry : pendingChanges.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            List<BlockChange> changes = entry.getValue();
            
            // Загружаем чанк ОДИН раз для всех блоков в нём
            LevelChunk levelChunk = world.getChunk(chunkPos.x, chunkPos.z);
            
            // Обрабатываем блоки малыми batch'ами для контроля производительности
            for (int i = 0; i < changes.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, changes.size());
                List<BlockChange> batch = changes.subList(i, endIndex);
                
                // Применяем изменения с ВИЗУАЛЬНЫМИ ФЛАГАМИ (исправление основной проблемы)
                for (BlockChange change : batch) {
                    try {
                        // ИСПРАВЛЕНИЕ: используем VISUAL_FLAGS = 2 вместо 0
                        // Флаг 2 = Block.UPDATE_CLIENTS - уведомляет клиента об изменении
                        world.setBlock(change.pos, change.newState, VISUAL_FLAGS);
                        blocksDestroyed++;
                        processedBlocks++;
                    } catch (Exception e) {
                        CombatMetaphysics.LOGGER.warn("Failed to change block at {}: {}", change.pos, e.getMessage());
                    }
                }
                
                // Проверяем временной лимит
                if (System.currentTimeMillis() - startTime > TIME_BUDGET_MS) {
                    CombatMetaphysics.LOGGER.warn("BlockChangeBuffer: Time budget exceeded, processed {}/{} blocks", 
                        processedBlocks, pendingChanges.values().stream().mapToInt(List::size).sum());
                    break;
                }
            }
            
            // Помечаем чанк как изменённый и запускаем light/physics updates
            try {
                levelChunk.markUnsaved();
                
                // ИСПРАВЛЕНИЕ: принудительное обновление освещения по чанку
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.getChunkSource().getLightEngine().checkBlock(levelChunk.getPos().getWorldPosition());
                }
                
                chunksModified++;
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.warn("Failed to update chunk {}: {}", chunkPos, e.getMessage());
                chunksModified++; // Считаем как обработанный
            }
            
            totalChanges += changes.size();
        }
        
        // ИСПРАВЛЕНИЕ: spawn deferred drops в малых batch'ах (рекомендация)
        if (!deferredDrops.isEmpty()) {
            spawnDeferredDropsInBatches();
        }
        
        // Очищаем буферы
        pendingChanges.clear();
        deferredDrops.clear();
        
        long duration = System.currentTimeMillis() - startTime;
        CombatMetaphysics.LOGGER.info("BlockChangeBuffer: Optimized flush completed in {}ms - {} blocks in {} chunks", 
            duration, blocksDestroyed, chunksModified);
        
        return new FlushResult(totalChanges, chunksModified, blocksDestroyed);
    }
    
    /**
     * Spawn deferred drops в малых batch'ах для предотвращения lag spikes
     */
    private void spawnDeferredDropsInBatches() {
        final int DROP_BATCH_SIZE = 50; // дропов за batch
        
        for (int i = 0; i < deferredDrops.size(); i += DROP_BATCH_SIZE) {
            int endIndex = Math.min(i + DROP_BATCH_SIZE, deferredDrops.size());
            List<ItemEntity> batch = deferredDrops.subList(i, endIndex);
            
            batch.forEach(world::addFreshEntity);
        }
        
        CombatMetaphysics.LOGGER.debug("Spawned {} deferred item drops in batches of {}", 
            deferredDrops.size(), DROP_BATCH_SIZE);
    }

    /**
     * Получить количество накопленных изменений
     */
    public int getPendingChangesCount() {
        return pendingChanges.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Очистить буфер без применения изменений
     */
    public void clear() {
        pendingChanges.clear();
        deferredDrops.clear();
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