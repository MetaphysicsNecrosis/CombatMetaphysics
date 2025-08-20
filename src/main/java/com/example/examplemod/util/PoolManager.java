package com.example.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Центральный менеджер object pools в стиле FAWE
 * Управляет пулами для часто используемых объектов
 */
public class PoolManager {
    // Object pools для часто создаваемых объектов
    private static final ObjectPool<BlockState> BLOCKSTATE_POOL = new ObjectPool<>(
        () -> Blocks.AIR.defaultBlockState(),
        1000
    );
    
    private static final ObjectPool<BlockPos> BLOCKPOS_POOL = new ObjectPool<>(
        () -> new BlockPos(0, 0, 0),
        2000
    );
    
    private static final ObjectPool<Vec3> VEC3_POOL = new ObjectPool<>(
        () -> Vec3.ZERO,
        500
    );
    
    private static final ObjectPool<ItemStack> ITEMSTACK_POOL = new ObjectPool<>(
        () -> new ItemStack(Items.AIR),
        1000
    );
    
    private static final ObjectPool<List<BlockPos>> BLOCKPOS_LIST_POOL = new ObjectPool<>(
        ArrayList::new,
        100
    );
    
    /**
     * BlockState pooling
     */
    public static BlockState acquireBlockState() {
        return BLOCKSTATE_POOL.acquire();
    }
    
    public static void releaseBlockState(BlockState blockState) {
        BLOCKSTATE_POOL.release(blockState);
    }
    
    /**
     * BlockPos pooling
     */
    public static BlockPos acquireBlockPos() {
        return BLOCKPOS_POOL.acquire();
    }
    
    public static void releaseBlockPos(BlockPos blockPos) {
        BLOCKPOS_POOL.release(blockPos);
    }
    
    /**
     * Vec3 pooling
     */
    public static Vec3 acquireVec3() {
        return VEC3_POOL.acquire();
    }
    
    public static void releaseVec3(Vec3 vec3) {
        VEC3_POOL.release(vec3);
    }
    
    /**
     * ItemStack pooling
     */
    public static ItemStack acquireItemStack() {
        return ITEMSTACK_POOL.acquire();
    }
    
    public static void releaseItemStack(ItemStack itemStack) {
        ITEMSTACK_POOL.release(itemStack);
    }
    
    /**
     * List<BlockPos> pooling для массовых операций
     */
    public static List<BlockPos> acquireBlockPosList() {
        List<BlockPos> list = BLOCKPOS_LIST_POOL.acquire();
        list.clear(); // Очищаем перед использованием
        return list;
    }
    
    public static void releaseBlockPosList(List<BlockPos> list) {
        if (list != null) {
            list.clear(); // Очищаем перед возвратом
            BLOCKPOS_LIST_POOL.release(list);
        }
    }
    
    /**
     * Очистка всех пулов (вызывается при выходе из мира)
     */
    public static void clearAllPools() {
        BLOCKSTATE_POOL.clear();
        BLOCKPOS_POOL.clear();
        VEC3_POOL.clear();
        ITEMSTACK_POOL.clear();
        BLOCKPOS_LIST_POOL.clear();
    }
    
    /**
     * Статистика пулов для отладки
     */
    public static String getPoolStats() {
        return String.format(
            "ObjectPools: BlockState=%d, BlockPos=%d, Vec3=%d, ItemStack=%d, Lists=%d",
            BLOCKSTATE_POOL.size(),
            BLOCKPOS_POOL.size(),
            VEC3_POOL.size(),
            ITEMSTACK_POOL.size(),
            BLOCKPOS_LIST_POOL.size()
        );
    }
}