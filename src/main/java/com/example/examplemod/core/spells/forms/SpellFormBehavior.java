package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Интерфейс поведения форм заклинаний
 * Определяет как заклинание взаимодействует с миром
 */
public interface SpellFormBehavior {
    
    /**
     * Инициализация поведения
     */
    void initialize(SpellFormContext context, SpellGeometry geometry);
    
    /**
     * Обновление каждый тик
     */
    void tick(Level level);
    
    /**
     * Обработка коллизии с сущностью
     */
    void onCollideWithEntity(Entity entity);
    
    /**
     * Обработка коллизии с блоком
     */
    void onCollideWithBlock(net.minecraft.core.BlockPos blockPos);
    
    /**
     * Проверка, должно ли заклинание продолжать существовать
     */
    boolean shouldContinue();
    
    /**
     * Очистка ресурсов при завершении
     */
    void cleanup();
    
    /**
     * Получить текущую геометрию
     */
    SpellGeometry getGeometry();
}