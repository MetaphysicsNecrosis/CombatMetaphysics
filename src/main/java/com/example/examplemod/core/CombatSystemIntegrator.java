package com.example.examplemod.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Интегратор для связи PlayerStateMachine с Minecraft системами
 * Обеспечивает полную интеграцию движения, состояний и Player объектов
 */
public class CombatSystemIntegrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatSystemIntegrator.class);
    
    private static CombatSystemIntegrator instance;
    private MinecraftServer server;
    private final Map<UUID, Long> lastMovementSync = new HashMap<>();
    
    public static CombatSystemIntegrator getInstance() {
        if (instance == null) {
            instance = new CombatSystemIntegrator();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Получает Player объект по UUID (server-side)
     */
    public Player getPlayer(UUID playerId) {
        if (server == null) {
            LOGGER.warn("MinecraftServer not set in CombatSystemIntegrator");
            return null;
        }
        
        return server.getPlayerList().getPlayer(playerId);
    }
    
    /**
     * Инициализирует PlayerStateMachine с полной интеграцией
     */
    public PlayerStateMachine initializePlayerCombat(Player player, ResourceManager resourceManager) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(player.getUUID(), resourceManager);
        
        // Устанавливаем Player объект для движения
        stateMachine.setPlayerInstance(player);
        
        // Синхронизируем начальное состояние движения
        PlayerMovementController.syncWithPlayerState(player.getUUID(), stateMachine.getCurrentState());
        
        LOGGER.info("Initialized combat system for player {}", player.getUUID());
        return stateMachine;
    }
    
    /**
     * Обновляет все боевые системы игрока (вызывается каждый tick)
     */
    public void tickPlayerCombat(UUID playerId) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine == null) {
            return; // Игрок не инициализирован
        }
        
        // Обновляем Player объект если его нет
        if (stateMachine.getPlayerInstance() == null) {
            Player player = getPlayer(playerId);
            if (player != null) {
                stateMachine.setPlayerInstance(player);
            }
        }
        
        // Обновляем state machine
        // Tick removed - Gothic system uses automatic state management
        
        // Синхронизируем движение каждые 5 тиков
        long currentTime = System.currentTimeMillis();
        Long lastSync = lastMovementSync.get(playerId);
        if (lastSync == null || currentTime - lastSync > 250) { // 250ms = 5 ticks
            syncMovementRestrictions(playerId);
            lastMovementSync.put(playerId, currentTime);
        }
    }
    
    /**
     * Синхронизирует ограничения движения с текущим состоянием
     */
    private void syncMovementRestrictions(UUID playerId) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine != null) {
            PlayerMovementController.syncWithPlayerState(playerId, stateMachine.getCurrentState());
        }
    }
    
    /**
     * Обрабатывает атаку ближнего боя с полной интеграцией движения
     */
    public DirectionalAttackSystem.AttackResult executeMeleeAttackWithMovement(
            UUID playerId, DirectionalAttackSystem.AttackDirection direction) {
        
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine == null) {
            return new DirectionalAttackSystem.AttackResult(false, "No combat state", 0, 0, false);
        }
        
        // Gothic system handles attack execution automatically
        // Create a stub result - actual attack is processed through state machine
        DirectionalAttackSystem.AttackResult result = new DirectionalAttackSystem.AttackResult(
            true, "Gothic attack in progress", 1, 10.0f, false);
        
        // Применяем движение если атака успешна
        if (result.isSuccess()) {
            Player player = getPlayer(playerId);
            if (player != null) {
                PlayerMovementController.applyAttackMovement(player, direction);
                LOGGER.debug("Applied attack movement for player {} with direction {}", playerId, direction);
            }
        }
        
        return result;
    }
    
    /**
     * Обрабатывает защитное действие с движением уклонения
     */
    public boolean executeDefenseWithMovement(UUID playerId, DefensiveActionsManager.DefensiveType type,
                                            PlayerMovementController.DodgeDirection dodgeDirection) {
        
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine == null) {
            return false;
        }
        
        // Convert to Gothic defense and execute
        GothicDefenseSystem.DefenseType gothicType = switch (type) {
            case BLOCK -> GothicDefenseSystem.DefenseType.BLOCK;
            case PARRY -> GothicDefenseSystem.DefenseType.PARRY;
            case DODGE -> GothicDefenseSystem.DefenseType.DODGE;
        };
        GothicDefenseSystem.DefenseActionResult result = stateMachine.startDefense(gothicType);
        boolean success = result.isSuccess();
        
        // Применяем движение уклонения если нужно
        if (success && type == DefensiveActionsManager.DefensiveType.DODGE) {
            Player player = getPlayer(playerId);
            if (player != null) {
                PlayerMovementController.applyDodgeMovement(player, dodgeDirection);
                LOGGER.debug("Applied dodge movement for player {} in direction {}", playerId, dodgeDirection);
            }
        }
        
        return success;
    }
    
    /**
     * Обрабатывает прерывание с восстановлением
     */
    public boolean processInterruptionWithRecovery(UUID playerId, InterruptionSystem.InterruptionType type, 
                                                  String reason) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine == null) {
            return false;
        }
        
        // Запоминаем состояние до прерывания
        PlayerState stateBeforeInterruption = stateMachine.getCurrentState();
        
        // Выполняем прерывание
        boolean success = stateMachine.interrupt(type, reason);
        
        if (success) {
            // Система восстановления уже интегрирована в PlayerStateMachine.interrupt()
            // Здесь можем добавить дополнительную логику если нужно
            LOGGER.debug("Processed interruption for player {}: {} -> INTERRUPTED", playerId, stateBeforeInterruption);
        }
        
        return success;
    }
    
    /**
     * Получает полную информацию о состоянии игрока для команд отладки
     */
    public Map<String, Object> getPlayerCombatDebugInfo(UUID playerId) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine == null) {
            return Map.of("error", "No combat state found for player " + playerId);
        }
        
        Map<String, Object> info = stateMachine.getDebugInfo();
        
        // Добавляем информацию от интегратора
        info.put("hasPlayerInstance", stateMachine.getPlayerInstance() != null);
        info.put("serverConnected", server != null);
        
        Player player = getPlayer(playerId);
        if (player != null) {
            info.put("playerOnline", true);
            info.put("playerPos", String.format("%.2f,%.2f,%.2f", 
                player.getX(), player.getY(), player.getZ()));
            info.put("playerLookDirection", String.format("%.2f,%.2f", 
                player.getXRot(), player.getYRot()));
        } else {
            info.put("playerOnline", false);
        }
        
        return info;
    }
    
    /**
     * Очищает боевое состояние игрока при отключении
     */
    public void cleanupPlayerCombat(UUID playerId) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine != null) {
            stateMachine.cleanup();
        }
        
        // Очищаем кэш движения
        lastMovementSync.remove(playerId);
        
        LOGGER.info("Cleaned up combat state for disconnected player {}", playerId);
    }
    
    /**
     * Принудительно синхронизирует все ограничения движения
     */
    public void forceMovementSync() {
        PlayerStateMachine.getAllInstances().forEach((playerId, stateMachine) -> {
            PlayerMovementController.syncWithPlayerState(playerId, stateMachine.getCurrentState());
        });
        LOGGER.debug("Force synced movement restrictions for all players");
    }
    
    /**
     * Получает статистику всех боевых систем
     */
    public Map<String, Object> getGlobalCombatStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Количество активных игроков
        stats.put("activePlayers", PlayerStateMachine.getAllInstances().size());
        
        // Статистика ограничений движения
        stats.put("movementRestrictions", PlayerMovementController.getRestrictionStats());
        
        // Статистика по состояниям
        Map<String, Integer> stateStats = new HashMap<>();
        PlayerStateMachine.getAllInstances().values().forEach(stateMachine -> {
            String state = stateMachine.getCurrentState().name();
            stateStats.put(state, stateStats.getOrDefault(state, 0) + 1);
        });
        stats.put("stateDistribution", stateStats);
        
        return stats;
    }
}