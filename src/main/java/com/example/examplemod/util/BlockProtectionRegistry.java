package com.example.examplemod.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import com.example.examplemod.CombatMetaphysics;

import java.util.Set;
import java.util.HashSet;

/**
 * Реестр защищённых блоков - блоки, которые нельзя разрушить заклинаниями
 * 
 * Категории защиты:
 * 1. ABSOLUTE - никогда не разрушаются (бедрок, барьер)
 * 2. VALUABLE - ценные блоки (сундуки, алмазные руды, древние обломки)
 * 3. REINFORCED - прочные блоки (обсидиан, незерит, анкор возрождения)
 * 4. INFRASTRUCTURE - инфраструктурные блоки (рельсы, редстоун, факелы)
 * 5. PLAYER_PLACED - блоки, размещённые игроками (опционально)
 */
public class BlockProtectionRegistry {
    
    // Абсолютно защищённые блоки (никогда не ломаются)
    private static final Set<Block> ABSOLUTE_PROTECTED = Set.of(
        Blocks.BEDROCK,
        Blocks.BARRIER,
        Blocks.COMMAND_BLOCK,
        Blocks.CHAIN_COMMAND_BLOCK,
        Blocks.REPEATING_COMMAND_BLOCK,
        Blocks.STRUCTURE_BLOCK,
        Blocks.JIGSAW,
        Blocks.END_PORTAL,
        Blocks.END_PORTAL_FRAME,
        Blocks.NETHER_PORTAL
    );
    
    // Ценные блоки (сундуки, руды, редкие блоки)
    private static final Set<Block> VALUABLE_PROTECTED = Set.of(
        // Сундуки и хранилища
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
        Blocks.BLACK_SHULKER_BOX,
        
        // Ценные руды
        Blocks.DIAMOND_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.ANCIENT_DEBRIS,
        Blocks.NETHERITE_BLOCK,
        
        // Блоки с данными
        Blocks.SPAWNER,
        Blocks.ENCHANTING_TABLE,
        Blocks.ANVIL,
        Blocks.CHIPPED_ANVIL,
        Blocks.DAMAGED_ANVIL,
        Blocks.BEACON,
        Blocks.CONDUIT
    );
    
    // Прочные блоки (требуют высокую мощность для разрушения)
    private static final Set<Block> REINFORCED_PROTECTED = Set.of(
        Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN,
        Blocks.RESPAWN_ANCHOR,
        Blocks.REINFORCED_DEEPSLATE,
        Blocks.END_STONE,
        
        // Блоки Незерита
        Blocks.NETHERITE_BLOCK
    );
    
    // Инфраструктурные блоки (редстоун, механизмы)
    private static final Set<Block> INFRASTRUCTURE_PROTECTED = Set.of(
        // Редстоун компоненты
        Blocks.REDSTONE_WIRE,
        Blocks.REDSTONE_TORCH,
        Blocks.REDSTONE_WALL_TORCH,
        Blocks.REPEATER,
        Blocks.COMPARATOR,
        Blocks.OBSERVER,
        Blocks.PISTON,
        Blocks.STICKY_PISTON,
        Blocks.DISPENSER,
        Blocks.DROPPER,
        Blocks.HOPPER,
        
        // Рельсы
        Blocks.RAIL,
        Blocks.POWERED_RAIL,
        Blocks.DETECTOR_RAIL,
        Blocks.ACTIVATOR_RAIL,
        
        // Освещение
        Blocks.TORCH,
        Blocks.WALL_TORCH,
        Blocks.SOUL_TORCH,
        Blocks.SOUL_WALL_TORCH,
        Blocks.LANTERN,
        Blocks.SOUL_LANTERN,
        Blocks.GLOWSTONE,
        Blocks.SEA_LANTERN,
        Blocks.SHROOMLIGHT
    );
    
    // Настройки защиты (можно настраивать)
    public static class ProtectionSettings {
        public boolean protectAbsolute = true;      // Всегда включено
        public boolean protectValuable = true;     // Защищать ценные блоки
        public boolean protectReinforced = true;   // Защищать прочные блоки (можно сломать мощными заклинаниями)
        public boolean protectInfrastructure = false; // Защищать инфраструктуру (по умолчанию выключено)
        public float reinforcedPowerThreshold = 15.0f; // Минимальная мощность для прочных блоков
    }
    
    private static final ProtectionSettings settings = new ProtectionSettings();
    
    /**
     * Проверяет, защищён ли блок от разрушения
     */
    public static boolean isBlockProtected(BlockState blockState, float destructionPower) {
        Block block = blockState.getBlock();
        
        // 1. АБСОЛЮТНАЯ ЗАЩИТА (никогда не ломается)
        if (settings.protectAbsolute && ABSOLUTE_PROTECTED.contains(block)) {
            return true;
        }
        
        // 2. ЦЕННЫЕ БЛОКИ (всегда защищены если включено)
        if (settings.protectValuable && VALUABLE_PROTECTED.contains(block)) {
            return true;
        }
        
        // 3. ПРОЧНЫЕ БЛОКИ (защищены от слабых заклинаний)
        if (settings.protectReinforced && REINFORCED_PROTECTED.contains(block)) {
            return destructionPower < settings.reinforcedPowerThreshold;
        }
        
        // 4. ИНФРАСТРУКТУРА (защищена если включено)
        if (settings.protectInfrastructure && INFRASTRUCTURE_PROTECTED.contains(block)) {
            return true;
        }
        
        // 5. ПРОВЕРКА ЧЕРЕЗ ТЕГИ (дополнительная защита)
        if (settings.protectValuable) {
            // Защищаем все блоки с тегом "ценные руды"
            if (blockState.is(BlockTags.DIAMOND_ORES) || 
                blockState.is(BlockTags.EMERALD_ORES) ||
                blockState.is(BlockTags.GOLD_ORES)) {
                return true;
            }
        }
        
        // 6. ПРОВЕРКА ПРОЧНОСТИ БЛОКА (стандартная логика)
        float blockHardness = blockState.getDestroySpeed(null, null);
        
        // Неразрушимые блоки (hardness < 0)
        if (blockHardness < 0) {
            return true;
        }
        
        // Блок слишком прочный для данной мощности
        if (destructionPower < blockHardness) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Получить причину защиты блока (для отладки/логирования)
     */
    public static String getProtectionReason(BlockState blockState, float destructionPower) {
        Block block = blockState.getBlock();
        
        if (settings.protectAbsolute && ABSOLUTE_PROTECTED.contains(block)) {
            return "ABSOLUTE_PROTECTED: " + block.getDescriptionId();
        }
        
        if (settings.protectValuable && VALUABLE_PROTECTED.contains(block)) {
            return "VALUABLE_PROTECTED: " + block.getDescriptionId();
        }
        
        if (settings.protectReinforced && REINFORCED_PROTECTED.contains(block)) {
            if (destructionPower < settings.reinforcedPowerThreshold) {
                return String.format("REINFORCED_PROTECTED: %s (power %.1f < threshold %.1f)", 
                    block.getDescriptionId(), destructionPower, settings.reinforcedPowerThreshold);
            }
        }
        
        if (settings.protectInfrastructure && INFRASTRUCTURE_PROTECTED.contains(block)) {
            return "INFRASTRUCTURE_PROTECTED: " + block.getDescriptionId();
        }
        
        float blockHardness = blockState.getDestroySpeed(null, null);
        if (blockHardness < 0) {
            return "UNBREAKABLE: hardness < 0";
        }
        
        if (destructionPower < blockHardness) {
            return String.format("TOO_HARD: power %.1f < hardness %.1f", destructionPower, blockHardness);
        }
        
        return "NOT_PROTECTED";
    }
    
    /**
     * Получить текущие настройки защиты
     */
    public static ProtectionSettings getSettings() {
        return settings;
    }
    
    /**
     * Логирует статистику защищённых блоков
     */
    public static void logProtectionStats() {
        CombatMetaphysics.LOGGER.info("Block Protection Registry Stats:");
        CombatMetaphysics.LOGGER.info("- Absolute Protected: {} blocks", ABSOLUTE_PROTECTED.size());
        CombatMetaphysics.LOGGER.info("- Valuable Protected: {} blocks", VALUABLE_PROTECTED.size()); 
        CombatMetaphysics.LOGGER.info("- Reinforced Protected: {} blocks (threshold: {})", 
            REINFORCED_PROTECTED.size(), settings.reinforcedPowerThreshold);
        CombatMetaphysics.LOGGER.info("- Infrastructure Protected: {} blocks", INFRASTRUCTURE_PROTECTED.size());
        
        CombatMetaphysics.LOGGER.info("Protection Settings:");
        CombatMetaphysics.LOGGER.info("- Protect Absolute: {}", settings.protectAbsolute);
        CombatMetaphysics.LOGGER.info("- Protect Valuable: {}", settings.protectValuable);
        CombatMetaphysics.LOGGER.info("- Protect Reinforced: {}", settings.protectReinforced);
        CombatMetaphysics.LOGGER.info("- Protect Infrastructure: {}", settings.protectInfrastructure);
    }
}