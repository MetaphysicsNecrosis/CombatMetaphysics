package com.example.examplemod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Простой блок магического верстака для тестирования системы параметров
 * 
 * Позволяет игрокам:
 * 1. Выбирать форму заклинания (PROJECTILE, BEAM, AREA, etc.)
 * 2. Настраивать параметры (урон, радиус, скорость, элементы)
 * 3. Тестировать математическую модель параметров
 * 4. Создавать SpellEntity с вычисленными характеристиками
 */
public class SpellCraftingTableBlock extends Block {
    
    public SpellCraftingTableBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, 
                                Player player, BlockHitResult hit) {
        
        // Открываем интерфейс тестирования на обеих сторонах
        openSpellCraftingInterface(player, level, pos);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Открыть интерфейс создания заклинаний
     */
    private void openSpellCraftingInterface(Player player, Level level, BlockPos pos) {
        
        if (level.isClientSide) {
            // Открываем GUI на клиенте
            net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.example.examplemod.client.gui.SpellCraftingScreen()
            );
        } else {
            player.displayClientMessage(Component.literal("§aМагический верстак активирован!"), false);
            player.displayClientMessage(Component.literal("§eИспользуй GUI или команды /spell"), false);
        }
    }
}