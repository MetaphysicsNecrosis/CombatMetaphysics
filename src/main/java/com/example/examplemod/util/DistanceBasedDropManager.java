package com.example.examplemod.util;

import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/**
 * Система специальных дропов с модификаторами расстояния
 * 
 * ПРИНЦИПЫ:
 * 1. Сундуки: лут зависит от расстояния до эпицентра заклинания
 * 2. Руды: автоплавка + количество зависит от расстояния
 * 3. Остальные блоки: БЕЗ дропов
 * 4. Максимальное расстояние для дропов: 16 блоков
 */
public class DistanceBasedDropManager {
    
    // Конфигурация дропов
    private static final double MAX_DROP_DISTANCE = 16.0;
    private static final double MIN_DISTANCE_MULTIPLIER = 0.1; // 10% на максимальном расстоянии
    private static final double MAX_DISTANCE_MULTIPLIER = 2.0; // 200% в эпицентре
    
    // Блоки сундуков и хранилищ
    private static final Set<Block> CHEST_BLOCKS = Set.of(
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.ENDER_CHEST,
        Blocks.BARREL,
        Blocks.SHULKER_BOX,
        Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX
    );
    
    // Блоки руд для автоплавки (используем HashMap для более 10 элементов)
    private static final Map<Block, ItemStack> ORE_SMELTING_MAP = createOreSmeltingMap();
    
    private static Map<Block, ItemStack> createOreSmeltingMap() {
        Map<Block, ItemStack> map = new HashMap<>();
        
        // Железные руды
        map.put(Blocks.IRON_ORE, new ItemStack(Items.IRON_INGOT));
        map.put(Blocks.DEEPSLATE_IRON_ORE, new ItemStack(Items.IRON_INGOT));
        map.put(Blocks.RAW_IRON_BLOCK, new ItemStack(Items.IRON_INGOT, 9));
        
        // Золотые руды
        map.put(Blocks.GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        map.put(Blocks.DEEPSLATE_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        map.put(Blocks.NETHER_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        map.put(Blocks.RAW_GOLD_BLOCK, new ItemStack(Items.GOLD_INGOT, 9));
        
        // Медные руды
        map.put(Blocks.COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
        map.put(Blocks.DEEPSLATE_COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
        map.put(Blocks.RAW_COPPER_BLOCK, new ItemStack(Items.COPPER_INGOT, 9));
        
        // Редкие руды остаются как есть
        map.put(Blocks.DIAMOND_ORE, new ItemStack(Items.DIAMOND));
        map.put(Blocks.DEEPSLATE_DIAMOND_ORE, new ItemStack(Items.DIAMOND));
        map.put(Blocks.EMERALD_ORE, new ItemStack(Items.EMERALD));
        map.put(Blocks.DEEPSLATE_EMERALD_ORE, new ItemStack(Items.EMERALD));
        
        // Другие руды
        map.put(Blocks.COAL_ORE, new ItemStack(Items.COAL));
        map.put(Blocks.DEEPSLATE_COAL_ORE, new ItemStack(Items.COAL));
        map.put(Blocks.REDSTONE_ORE, new ItemStack(Items.REDSTONE, 4));
        map.put(Blocks.DEEPSLATE_REDSTONE_ORE, new ItemStack(Items.REDSTONE, 4));
        map.put(Blocks.LAPIS_ORE, new ItemStack(Items.LAPIS_LAZULI, 4));
        map.put(Blocks.DEEPSLATE_LAPIS_ORE, new ItemStack(Items.LAPIS_LAZULI, 4));
        
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * Генерирует дропы для блока с учётом расстояния до эпицентра
     */
    public static List<ItemEntity> generateCustomDrops(Level world, BlockPos blockPos, BlockState blockState, BlockPos spellCenter) {
        List<ItemEntity> drops = new ArrayList<>();
        
        // Вычисляем расстояние до эпицентра
        double distance = Math.sqrt(blockPos.distSqr(spellCenter));
        if (distance > MAX_DROP_DISTANCE) {
            // Слишком далеко - никаких дропов
            return drops;
        }
        
        // Вычисляем модификатор количества (ближе = больше дропов)
        double distanceMultiplier = calculateDistanceMultiplier(distance);
        
        Block block = blockState.getBlock();
        
        // ОБРАБОТКА СУНДУКОВ И ХРАНИЛИЩ
        if (CHEST_BLOCKS.contains(block)) {
            drops.addAll(generateChestDrops(world, blockPos, blockState, distanceMultiplier));
        }
        // ОБРАБОТКА РУД (автоплавка)
        else if (ORE_SMELTING_MAP.containsKey(block)) {
            drops.addAll(generateOreDrops(world, blockPos, block, distanceMultiplier));
        }
        // ВСЕ ОСТАЛЬНЫЕ БЛОКИ: БЕЗ ДРОПОВ
        else {
            CombatMetaphysics.LOGGER.debug("Block {} at {} generates no drops (not chest/ore)", 
                block.getDescriptionId(), blockPos);
        }
        
        return drops;
    }
    
    /**
     * Вычисляет модификатор количества дропов на основе расстояния
     * Ближе к эпицентру = больше дропов
     */
    private static double calculateDistanceMultiplier(double distance) {
        // Нормализуем расстояние (0.0 = эпицентр, 1.0 = максимальное расстояние)
        double normalizedDistance = Math.min(distance / MAX_DROP_DISTANCE, 1.0);
        
        // Линейная интерполяция от MAX к MIN
        double multiplier = MAX_DISTANCE_MULTIPLIER - (normalizedDistance * (MAX_DISTANCE_MULTIPLIER - MIN_DISTANCE_MULTIPLIER));
        
        CombatMetaphysics.LOGGER.debug("Distance: {:.2f} blocks -> multiplier: {:.2f}x", distance, multiplier);
        return multiplier;
    }
    
    /**
     * Генерирует дропы из сундуков с модификатором расстояния
     */
    private static List<ItemEntity> generateChestDrops(Level world, BlockPos blockPos, BlockState blockState, double multiplier) {
        List<ItemEntity> drops = new ArrayList<>();
        
        try {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            Container container = null;
            
            // Определяем тип контейнера
            if (blockEntity instanceof ChestBlockEntity chestEntity) {
                container = chestEntity;
            } else if (blockEntity instanceof BarrelBlockEntity barrelEntity) {
                container = barrelEntity;
            } else if (blockEntity instanceof ShulkerBoxBlockEntity shulkerEntity) {
                container = shulkerEntity;
            }
            
            if (container == null) {
                CombatMetaphysics.LOGGER.warn("Chest block {} at {} has no valid container", 
                    blockState.getBlock().getDescriptionId(), blockPos);
                return drops;
            }
            
            // Извлекаем и модифицируем содержимое
            int totalItems = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                
                // Применяем модификатор расстояния к количеству
                int originalCount = stack.getCount();
                int modifiedCount = Math.max(1, (int) Math.round(originalCount * multiplier));
                
                // Создаём новый стак с модифицированным количеством
                ItemStack modifiedStack = stack.copy();
                modifiedStack.setCount(modifiedCount);
                
                // Создаём ItemEntity для дропа
                ItemEntity dropEntity = new ItemEntity(world, 
                    blockPos.getX() + 0.5, 
                    blockPos.getY() + 0.5, 
                    blockPos.getZ() + 0.5, 
                    modifiedStack);
                
                drops.add(dropEntity);
                totalItems += modifiedCount;
            }
            
            CombatMetaphysics.LOGGER.info("Chest at {} dropped {} items (multiplier: {:.2f}x)", 
                blockPos, totalItems, multiplier);
                
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.error("Failed to generate chest drops at {}: {}", blockPos, e.getMessage());
        }
        
        return drops;
    }
    
    /**
     * Генерирует автоплавленные дропы из руд с модификатором расстояния
     */
    private static List<ItemEntity> generateOreDrops(Level world, BlockPos blockPos, Block oreBlock, double multiplier) {
        List<ItemEntity> drops = new ArrayList<>();
        
        try {
            ItemStack smeltedResult = ORE_SMELTING_MAP.get(oreBlock);
            if (smeltedResult == null) {
                CombatMetaphysics.LOGGER.warn("No smelting result defined for ore: {}", 
                    oreBlock.getDescriptionId());
                return drops;
            }
            
            // Применяем модификатор расстояния
            int baseCount = smeltedResult.getCount();
            int modifiedCount = Math.max(1, (int) Math.round(baseCount * multiplier));
            
            // Создаём результат автоплавки
            ItemStack resultStack = smeltedResult.copy();
            resultStack.setCount(modifiedCount);
            
            ItemEntity dropEntity = new ItemEntity(world,
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5,
                resultStack);
            
            drops.add(dropEntity);
            
            CombatMetaphysics.LOGGER.info("Ore {} at {} auto-smelted to {} x{} (multiplier: {:.2f}x)",
                oreBlock.getDescriptionId(), blockPos, 
                resultStack.getItem().getDescriptionId(), modifiedCount, multiplier);
                
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.error("Failed to generate ore drops at {}: {}", blockPos, e.getMessage());
        }
        
        return drops;
    }
    
    /**
     * Проверяет, должен ли блок давать дропы
     */
    public static boolean shouldGenerateDrops(BlockState blockState) {
        Block block = blockState.getBlock();
        return CHEST_BLOCKS.contains(block) || ORE_SMELTING_MAP.containsKey(block);
    }
    
    /**
     * Проверяет, является ли блок сундуком/хранилищем
     */
    public static boolean isChestBlock(Block block) {
        return CHEST_BLOCKS.contains(block);
    }
    
    /**
     * Проверяет, является ли блок рудой для автоплавки
     */
    public static boolean isOreBlock(Block block) {
        return ORE_SMELTING_MAP.containsKey(block);
    }
    
    /**
     * Логирует статистику системы дропов
     */
    public static void logDropSystemStats() {
        CombatMetaphysics.LOGGER.info("Distance-Based Drop System Stats:");
        CombatMetaphysics.LOGGER.info("- Chest blocks supported: {}", CHEST_BLOCKS.size());
        CombatMetaphysics.LOGGER.info("- Ore auto-smelting supported: {}", ORE_SMELTING_MAP.size());
        CombatMetaphysics.LOGGER.info("- Max drop distance: {} blocks", MAX_DROP_DISTANCE);
        CombatMetaphysics.LOGGER.info("- Distance multiplier range: {:.1f}x - {:.1f}x", 
            MIN_DISTANCE_MULTIPLIER, MAX_DISTANCE_MULTIPLIER);
    }
}