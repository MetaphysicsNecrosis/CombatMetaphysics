package com.example.examplemod.network;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.core.DirectionalAttackSystem;
import com.example.examplemod.core.DefensiveActionsManager;
import com.example.examplemod.core.InterruptionSystem;
import com.example.examplemod.core.PlayerStateMachine;
import com.example.examplemod.server.CombatServerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Обработчик сетевых пакетов для combat системы
 */
public class CombatNetworkHandler {
    
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(CombatMetaphysics.MODID);
        
        // Регистрируем пакеты
        registrar.playToServer(
            CombatActionPacket.TYPE,
            CombatActionPacket.STREAM_CODEC,
            CombatNetworkHandler::handleCombatAction
        );
        
        registrar.playToClient(
            CombatStatePacket.TYPE,
            CombatStatePacket.STREAM_CODEC,
            CombatNetworkHandler::handleCombatState
        );
        
        CombatMetaphysics.LOGGER.info("Combat network handlers registered");
    }
    
    /**
     * Обрабатывает действия combat от клиента на сервере
     */
    private static void handleCombatAction(CombatActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    UUID playerId = packet.playerId();
                    
                    // Проверяем, что пакет от правильного игрока
                    if (!serverPlayer.getUUID().equals(playerId)) {
                        CombatMetaphysics.LOGGER.warn("Combat action packet from wrong player: expected {}, got {}", 
                            serverPlayer.getUUID(), playerId);
                        return;
                    }
                    
                    PlayerStateMachine stateMachine = CombatServerManager.getInstance().getPlayerStateMachine(playerId);
                    
                    switch (packet.actionType()) {
                        case MAGIC_PREPARE -> handleMagicPrepare(stateMachine, packet.actionData());
                        case MAGIC_CAST -> handleMagicCast(stateMachine, packet.actionData());
                        case MELEE_START -> handleMeleeStart(stateMachine, packet.actionData());
                        case MELEE_EXECUTE -> handleMeleeExecute(stateMachine);
                        case MELEE_DIRECTION_CHANGE -> handleMeleeDirectionChange(stateMachine, packet.actionData());
                        case DEFENSE_ACTIVATE -> handleDefenseActivate(stateMachine, packet.actionData());
                        case ACTION_CANCEL -> handleActionCancel(stateMachine, packet.actionData());
                        case INTERRUPT_REQUEST -> handleInterruptRequest(stateMachine, packet.actionData());
                    }
                    
                    // Отправляем обновленное состояние обратно клиенту
                    sendStateUpdate(serverPlayer, stateMachine);
                    
                } else {
                    CombatMetaphysics.LOGGER.warn("Combat action packet from non-server player");
                }
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.error("Error handling combat action: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * Обрабатывает обновления состояния на клиенте
     */
    private static void handleCombatState(CombatStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getUUID().equals(packet.playerId())) {
                    // Обновляем клиентское состояние
                    // Можно добавить синхронизацию с клиентскими системами
                    CombatMetaphysics.LOGGER.debug("Received state update: {} -> {}", 
                        packet.playerId(), packet.currentState());
                }
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.error("Error handling combat state: {}", e.getMessage(), e);
            }
        });
    }
    
    // === ОБРАБОТЧИКИ КОНКРЕТНЫХ ДЕЙСТВИЙ ===
    
    private static void handleMagicPrepare(PlayerStateMachine stateMachine, String actionData) {
        // Парсим JSON данные (упрощенно)
        if (actionData.contains("\"spell\":")) {
            String spellName = extractJsonValue(actionData, "spell");
            float cost = Float.parseFloat(extractJsonValue(actionData, "cost"));
            
            boolean success = stateMachine.startMagicPreparation(spellName, cost);
            CombatMetaphysics.LOGGER.debug("Magic preparation: {} (spell: {}, cost: {})", 
                success ? "SUCCESS" : "FAILED", spellName, cost);
        }
    }
    
    private static void handleMagicCast(PlayerStateMachine stateMachine, String actionData) {
        String spellName = extractJsonValue(actionData, "spell");
        boolean success = stateMachine.startMagicCasting(spellName);
        CombatMetaphysics.LOGGER.debug("Magic casting: {} (spell: {})", 
            success ? "SUCCESS" : "FAILED", spellName);
    }
    
    private static void handleMeleeStart(PlayerStateMachine stateMachine, String actionData) {
        String direction = extractJsonValue(actionData, "direction");
        try {
            DirectionalAttackSystem.AttackDirection attackDirection = 
                DirectionalAttackSystem.AttackDirection.valueOf(direction);
            boolean success = stateMachine.startMeleePreparation(attackDirection);
            CombatMetaphysics.LOGGER.debug("Melee start: {} (direction: {})", 
                success ? "SUCCESS" : "FAILED", direction);
        } catch (IllegalArgumentException e) {
            CombatMetaphysics.LOGGER.warn("Invalid attack direction: {}", direction);
        }
    }
    
    private static void handleMeleeExecute(PlayerStateMachine stateMachine) {
        var result = stateMachine.executeMeleeAttack();
        CombatMetaphysics.LOGGER.debug("Melee execute: {} (damage: {})", 
            result.isSuccess() ? "SUCCESS" : "FAILED", result.getDamage());
    }
    
    private static void handleMeleeDirectionChange(PlayerStateMachine stateMachine, String actionData) {
        String direction = extractJsonValue(actionData, "direction");
        try {
            DirectionalAttackSystem.AttackDirection attackDirection = 
                DirectionalAttackSystem.AttackDirection.valueOf(direction);
            
            UUID playerId = stateMachine.getPlayerId();
            stateMachine.getAttackSystem().cancelCharging(playerId);
            boolean success = stateMachine.getAttackSystem().startCharging(playerId, attackDirection);
            
            CombatMetaphysics.LOGGER.debug("Melee direction change: {} (direction: {})", 
                success ? "SUCCESS" : "FAILED", direction);
        } catch (IllegalArgumentException e) {
            CombatMetaphysics.LOGGER.warn("Invalid attack direction: {}", direction);
        }
    }
    
    private static void handleDefenseActivate(PlayerStateMachine stateMachine, String actionData) {
        String defenseType = extractJsonValue(actionData, "type");
        try {
            DefensiveActionsManager.DefensiveType type = 
                DefensiveActionsManager.DefensiveType.valueOf(defenseType);
            boolean success = stateMachine.startDefensiveAction(type);
            CombatMetaphysics.LOGGER.debug("Defense activate: {} (type: {})", 
                success ? "SUCCESS" : "FAILED", defenseType);
        } catch (IllegalArgumentException e) {
            CombatMetaphysics.LOGGER.warn("Invalid defense type: {}", defenseType);
        }
    }
    
    private static void handleActionCancel(PlayerStateMachine stateMachine, String actionData) {
        String reason = extractJsonValue(actionData, "reason");
        boolean success = stateMachine.completeAction();
        CombatMetaphysics.LOGGER.debug("Action cancel: {} (reason: {})", 
            success ? "SUCCESS" : "FAILED", reason);
    }
    
    private static void handleInterruptRequest(PlayerStateMachine stateMachine, String actionData) {
        String interruptType = extractJsonValue(actionData, "type");
        String reason = extractJsonValue(actionData, "reason");
        try {
            InterruptionSystem.InterruptionType type = 
                InterruptionSystem.InterruptionType.valueOf(interruptType);
            boolean success = stateMachine.interrupt(type, reason);
            CombatMetaphysics.LOGGER.debug("Interrupt request: {} (type: {}, reason: {})", 
                success ? "SUCCESS" : "FAILED", interruptType, reason);
        } catch (IllegalArgumentException e) {
            CombatMetaphysics.LOGGER.warn("Invalid interrupt type: {}", interruptType);
        }
    }
    
    // === УТИЛИТЫ ===
    
    /**
     * Отправляет обновление состояния клиенту
     */
    public static void sendStateUpdate(ServerPlayer player, PlayerStateMachine stateMachine) {
        var resourceManager = stateMachine.getResourceManager();
        
        CombatStatePacket packet = new CombatStatePacket(
            stateMachine.getPlayerId(),
            stateMachine.getCurrentState(),
            stateMachine.getCurrentAction(),
            stateMachine.getStateChangeTime(),
            resourceManager.getCurrentMana(),
            resourceManager.getMaxMana(),
            resourceManager.getReservedMana(),
            resourceManager.getCurrentStamina(),
            resourceManager.getMaxStamina()
        );
        
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    /**
     * Отправляет действие от клиента к серверу
     */
    public static void sendAction(CombatActionPacket packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    /**
     * Простой парсер JSON значений
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return "";
        
        startIndex += searchKey.length();
        if (json.charAt(startIndex) == '"') {
            startIndex++; // Пропускаем открывающую кавычку
            int endIndex = json.indexOf('"', startIndex);
            return endIndex != -1 ? json.substring(startIndex, endIndex) : "";
        } else {
            // Числовое значение
            int endIndex = startIndex;
            while (endIndex < json.length() && 
                   (Character.isDigit(json.charAt(endIndex)) || json.charAt(endIndex) == '.')) {
                endIndex++;
            }
            return json.substring(startIndex, endIndex);
        }
    }
}