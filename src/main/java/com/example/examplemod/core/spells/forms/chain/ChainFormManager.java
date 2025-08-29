package com.example.examplemod.core.spells.forms.chain;

import com.example.examplemod.core.spells.entities.SpellEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ЕДИНЫЙ менеджер для цепных заклинаний (CHAIN)
 * Управляет переходами между целями через один SpellEntity корень
 * 
 * Принцип: SpellEntity НЕ дублируется - она перемещается между целями
 */
public class ChainFormManager {
    
    // === ГЛОБАЛЬНОЕ СОСТОЯНИЕ ЦЕПЕЙ ===
    private static final Map<UUID, ChainState> activeChains = new ConcurrentHashMap<>();
    
    /**
     * Инициализировать цепное заклинание
     */
    public static void initializeChain(SpellEntity spellEntity) {
        UUID spellId = spellEntity.getSpellInstanceId();
        
        ChainState chainState = new ChainState();
        chainState.currentTarget = findInitialTarget(spellEntity);
        chainState.remainingJumps = (int) spellEntity.getAppliedFloat("chain_length", 3.0f);
        chainState.maxRange = spellEntity.getAppliedFloat("chain_range", 10.0f);
        chainState.damageDecay = spellEntity.getAppliedFloat("chain_damage_decay", 0.8f);
        // visitedTargets уже инициализирован в конструкторе ChainState
        
        activeChains.put(spellId, chainState);
        
        // Телепортируем SpellEntity к первой цели
        if (chainState.currentTarget != null) {
            teleportToTarget(spellEntity, chainState.currentTarget);
            markTargetVisited(spellEntity, chainState.currentTarget);
        }
    }
    
    /**
     * ЕДИНАЯ точка обработки цепной формы - вызывается из SpellEntity.tick()
     */
    public static void processChainForm(SpellEntity spellEntity, Level level) {
        UUID spellId = spellEntity.getSpellInstanceId();
        ChainState chainState = activeChains.get(spellId);
        
        if (chainState == null) {
            initializeChain(spellEntity);
            return;
        }
        
        // === ПРОВЕРЯЕМ ГОТОВНОСТЬ К ПЕРЕХОДУ ===
        if (!isReadyForNextJump(spellEntity, chainState)) {
            return; // Ещё обрабатываем текущую цель
        }
        
        // === ПОИСК СЛЕДУЮЩЕЙ ЦЕЛИ ===
        LivingEntity nextTarget = findNextChainTarget(spellEntity, level, chainState);
        
        if (nextTarget == null || chainState.remainingJumps <= 0) {
            // Цепь завершена
            completeChain(spellEntity);
            return;
        }
        
        // === ПЕРЕХОД К СЛЕДУЮЩЕЙ ЦЕЛИ ===
        performChainJump(spellEntity, nextTarget, chainState);
    }
    
    /**
     * Выполнить переход к следующей цели
     */
    private static void performChainJump(SpellEntity spellEntity, LivingEntity target, ChainState chainState) {
        
        // === МОДИФИКАЦИЯ ПАРАМЕТРОВ ПРИ ПЕРЕХОДЕ ===
        
        // Урон уменьшается с каждым переходом
        float currentDamage = spellEntity.getSpellDamage();
        float newDamage = currentDamage * chainState.damageDecay;
        spellEntity.setSpellDamage(newDamage);
        
        // Размер может изменяться
        float sizeDecay = spellEntity.getAppliedFloat("chain_size_decay", 1.0f);
        if (sizeDecay != 1.0f) {
            spellEntity.setSpellSize(spellEntity.getSpellSize() * sizeDecay);
        }
        
        // === ТЕЛЕПОРТАЦИЯ К НОВОЙ ЦЕЛИ ===
        teleportToTarget(spellEntity, target);
        
        // === ОБНОВЛЕНИЕ СОСТОЯНИЯ ===
        chainState.currentTarget = target;
        chainState.remainingJumps--;
        markTargetVisited(spellEntity, target);
        chainState.jumpCooldown = (int) spellEntity.getAppliedFloat("chain_jump_delay", 5.0f);
        
        // === ВИЗУАЛЬНЫЙ ЭФФЕКТ ПЕРЕХОДА ===
        createChainJumpEffect(spellEntity, target);
    }
    
    /**
     * Найти следующую цель для цепи
     */
    private static LivingEntity findNextChainTarget(SpellEntity spellEntity, Level level, ChainState chainState) {
        
        Vec3 currentPos = spellEntity.position();
        float searchRadius = chainState.maxRange;
        
        // Поиск в радиусе
        AABB searchBox = new AABB(
            currentPos.x - searchRadius, currentPos.y - searchRadius, currentPos.z - searchRadius,
            currentPos.x + searchRadius, currentPos.y + searchRadius, currentPos.z + searchRadius
        );
        
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, 
            entity -> isValidChainTarget(entity, spellEntity, chainState));
        
        if (candidates.isEmpty()) return null;
        
        // === СТРАТЕГИИ ВЫБОРА ЦЕЛИ ===
        String targetStrategy = spellEntity.getAppliedFloat("chain_target_strategy", 0.0f) == 0 ? "NEAREST" : "RANDOM";
        
        return switch (targetStrategy) {
            case "NEAREST" -> findNearestTarget(currentPos, candidates);
            case "RANDOM" -> candidates.get(level.getRandom().nextInt(candidates.size()));
            case "STRONGEST" -> findStrongestTarget(candidates);
            default -> findNearestTarget(currentPos, candidates);
        };
    }
    
    /**
     * Проверить готовность к следующему переходу
     */
    private static boolean isReadyForNextJump(SpellEntity spellEntity, ChainState chainState) {
        
        // Кулдаун между переходами
        if (chainState.jumpCooldown > 0) {
            chainState.jumpCooldown--;
            return false;
        }
        
        // Проверяем, завершили ли мы обработку текущей цели
        // (например, нанесли урон или применили эффекты)
        return chainState.hasProcessedCurrentTarget;
    }
    
    /**
     * Отметить цель как обработанную
     */
    public static void markTargetProcessed(SpellEntity spellEntity) {
        UUID spellId = spellEntity.getSpellInstanceId();
        ChainState chainState = activeChains.get(spellId);
        
        if (chainState != null) {
            chainState.hasProcessedCurrentTarget = true;
        }
    }
    
    /**
     * Завершить цепное заклинание
     */
    private static void completeChain(SpellEntity spellEntity) {
        UUID spellId = spellEntity.getSpellInstanceId();
        activeChains.remove(spellId);
        
        // Создаём финальный эффект
        createChainCompleteEffect(spellEntity);
        
        // Уничтожаем SpellEntity
        spellEntity.discard();
    }
    
    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===
    
    private static void teleportToTarget(SpellEntity spellEntity, LivingEntity target) {
        Vec3 targetPos = target.position().add(0, 1, 0); // Немного выше цели
        spellEntity.setPos(targetPos.x, targetPos.y, targetPos.z);
        
        // Сбрасываем скорость
        spellEntity.setDeltaMovement(Vec3.ZERO);
    }
    
    private static void markTargetVisited(SpellEntity spellEntity, LivingEntity target) {
        UUID spellId = spellEntity.getSpellInstanceId();
        ChainState chainState = activeChains.get(spellId);
        
        if (chainState != null) {
            chainState.visitedTargets.add(target.getUUID());
            chainState.hasProcessedCurrentTarget = false; // Сбрасываем флаг
        }
    }
    
    private static boolean isValidChainTarget(LivingEntity entity, SpellEntity spellEntity, ChainState chainState) {
        // Не цепляемся к кастеру
        if (entity == spellEntity.getCaster()) return false;
        
        // Не цепляемся к уже посещённым целям
        if (chainState.visitedTargets.contains(entity.getUUID())) return false;
        
        // Проверяем видимость цели
        return hasLineOfSight(spellEntity, entity);
    }
    
    private static LivingEntity findInitialTarget(SpellEntity spellEntity) {
        Level level = spellEntity.level();
        Vec3 pos = spellEntity.position();
        float range = spellEntity.getAppliedFloat("chain_range", 10.0f);
        
        AABB searchBox = new AABB(
            pos.x - range, pos.y - range, pos.z - range,
            pos.x + range, pos.y + range, pos.z + range
        );
        
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
            entity -> entity != spellEntity.getCaster() && hasLineOfSight(spellEntity, entity));
        
        return candidates.isEmpty() ? null : findNearestTarget(pos, candidates);
    }
    
    private static LivingEntity findNearestTarget(Vec3 position, List<LivingEntity> candidates) {
        return candidates.stream()
            .min((a, b) -> Double.compare(
                position.distanceToSqr(a.position()),
                position.distanceToSqr(b.position())
            ))
            .orElse(null);
    }
    
    private static LivingEntity findStrongestTarget(List<LivingEntity> candidates) {
        return candidates.stream()
            .max((a, b) -> Float.compare(a.getMaxHealth(), b.getMaxHealth()))
            .orElse(null);
    }
    
    private static boolean hasLineOfSight(SpellEntity spellEntity, LivingEntity target) {
        // Простая проверка видимости - можно расширить
        return !spellEntity.level().clip(
            new net.minecraft.world.level.ClipContext(
                spellEntity.position(),
                target.position(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                spellEntity
            )
        ).getType().equals(net.minecraft.world.phys.HitResult.Type.BLOCK);
    }
    
    // Заглушки для визуальных эффектов
    private static void createChainJumpEffect(SpellEntity spellEntity, LivingEntity target) {
        // TODO: Частицы молнии между текущей позицией и целью
    }
    
    private static void createChainCompleteEffect(SpellEntity spellEntity) {
        // TODO: Финальный взрыв или эффект завершения цепи
    }
    
    /**
     * Получить текущее состояние цепи
     */
    public static ChainState getChainState(UUID spellId) {
        return activeChains.get(spellId);
    }
    
    /**
     * Очистить все цепи (для cleanup)
     */
    public static void clearAllChains() {
        activeChains.clear();
    }
}