package com.example.examplemod.api;

import com.example.examplemod.core.PlayerStateMachine;
import com.example.examplemod.core.ResourceManager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Центральный контроллер для управления Gothic Combat System
 * Предоставляет простые методы для интеграции с клавишами, UI и событиями
 */
public class CombatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatController.class);
    
    // Singleton instance
    private static CombatController instance;
    
    // Карта активных игроков
    private final Map<UUID, Player> activePlayers = new HashMap<>();
    
    private CombatController() {}
    
    public static CombatController getInstance() {
        if (instance == null) {
            instance = new CombatController();
        }
        return instance;
    }
    
    // === РЕГИСТРАЦИЯ ИГРОКОВ ===
    
    /**
     * Регистрирует игрока в системе боя
     */
    public void registerPlayer(Player player) {
        UUID playerId = player.getUUID();
        
        // Проверяем что игрок еще не зарегистрирован
        if (activePlayers.containsKey(playerId)) {
            return; // Уже зарегистрирован
        }
        
        activePlayers.put(playerId, player);
        
        // Создаем ResourceManager для игрока
        ResourceManager resourceManager = new ResourceManager(playerId, 100f, 100f);
        
        // Инициализируем state machine с ResourceManager и устанавливаем Player объект
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId, resourceManager);
        stateMachine.setPlayerInstance(player);
        
        LOGGER.debug("Registered player {} for Gothic combat", player.getName().getString());
    }
    
    /**
     * Отменяет регистрацию игрока
     */
    public void unregisterPlayer(Player player) {
        UUID playerId = player.getUUID();
        activePlayers.remove(playerId);
        
        // Очищаем state machine
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine != null) {
            stateMachine.cleanup();
        }
        PlayerStateMachine.removeInstance(playerId);
        
        LOGGER.debug("Unregistered player {} from Gothic combat", player.getName().getString());
    }
    
    // === ПРОСТЫЕ КОМАНДЫ ДЛЯ КЛАВИШ ===
    
    /**
     * Атака влево (Q или аналогичная клавиша)
     */
    public void attackLeft(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, GothicCombatAPI.AttackDirection.LEFT);
        sendFeedback(player, result);
    }
    
    /**
     * Атака вправо (E или аналогичная клавиша)
     */
    public void attackRight(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, GothicCombatAPI.AttackDirection.RIGHT);
        sendFeedback(player, result);
    }
    
    /**
     * Атака сверху (ЛКМ + W или аналогичная комбинация)
     */
    public void attackTop(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, GothicCombatAPI.AttackDirection.TOP);
        sendFeedback(player, result);
    }
    
    /**
     * Выпад вперед (ЛКМ + Shift или аналогичная комбинация)
     */
    public void attackThrust(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, GothicCombatAPI.AttackDirection.THRUST);
        sendFeedback(player, result);
    }
    
    /**
     * Блокирование (ПКМ удерживается)
     */
    public void startBlocking(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.defend(player, GothicCombatAPI.DefenseType.BLOCK);
        sendFeedback(player, result);
    }
    
    /**
     * Парирование (ПКМ быстрый клик)
     */
    public void parry(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.defend(player, GothicCombatAPI.DefenseType.PARRY);
        sendFeedback(player, result);
    }
    
    /**
     * Уклонение (Пробел или аналогичная клавиша)
     */
    public void dodge(Player player) {
        GothicCombatAPI.CombatResult result = GothicCombatAPI.defend(player, GothicCombatAPI.DefenseType.DODGE);
        sendFeedback(player, result);
    }
    
    /**
     * Переключение боевой стойки (R или аналогичная клавиша)
     */
    public void toggleCombatStance(Player player) {
        GothicCombatAPI.CombatInfo info = GothicCombatAPI.getCombatInfo(player);
        
        GothicCombatAPI.CombatResult result;
        if (info.isInCombat()) {
            result = GothicCombatAPI.exitToPeaceful(player);
        } else {
            result = GothicCombatAPI.enterCombatStance(player);
        }
        
        sendFeedback(player, result);
    }
    
    // === АВТОМАТИЧЕСКИЕ ТРИГГЕРЫ ===
    
    /**
     * Автоматически входит в боевую стойку при получении урона
     */
    public void onPlayerHurt(Player player) {
        GothicCombatAPI.CombatInfo info = GothicCombatAPI.getCombatInfo(player);
        
        if (!info.isInCombat()) {
            GothicCombatAPI.enterCombatStance(player);
            sendMessage(player, "Entered combat stance due to taking damage!");
        }
    }
    
    /**
     * Автоматически входит в боевую стойку при обнаружении врага
     */
    public void onEnemyNearby(Player player) {
        GothicCombatAPI.CombatInfo info = GothicCombatAPI.getCombatInfo(player);
        
        if (!info.isInCombat()) {
            GothicCombatAPI.enterCombatStance(player);
            sendMessage(player, "Enemy detected - entering combat stance!");
        }
    }
    
    // === ОБНОВЛЕНИЕ СИСТЕМЫ ===
    
    /**
     * Обновляет всех зарегистрированных игроков (вызывается каждый тик)
     */
    public void tick() {
        for (Player player : activePlayers.values()) {
            PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(player.getUUID());
            if (stateMachine != null) {
                // Tick removed - Gothic system uses automatic state management
            }
        }
    }
    
    /**
     * Get state machine for player
     */
    public PlayerStateMachine getStateMachine(UUID playerId) {
        return PlayerStateMachine.getInstance(playerId);
    }
    
    // === ИНФОРМАЦИОННЫЕ МЕТОДЫ ===
    
    /**
     * Получает информацию о состоянии боя для UI
     */
    public GothicCombatAPI.CombatInfo getCombatInfo(Player player) {
        return GothicCombatAPI.getCombatInfo(player);
    }
    
    /**
     * Проверяет, может ли игрок выполнить действие (для отображения UI)
     */
    public boolean canPlayerPerformAction(Player player, GothicCombatAPI.CombatAction action) {
        return GothicCombatAPI.canPerformAction(player, action);
    }
    
    /**
     * Получает цвет для индикатора выносливости
     */
    public int getStaminaBarColor(Player player) {
        GothicCombatAPI.CombatInfo info = getCombatInfo(player);
        return info.getStaminaColor();
    }
    
    /**
     * Получает процент заполнения полосы выносливости
     */
    public float getStaminaBarFill(Player player) {
        GothicCombatAPI.CombatInfo info = getCombatInfo(player);
        return info.getStaminaPercentage();
    }
    
    /**
     * Получает текстовое описание текущего состояния
     */
    public String getStateDescription(Player player) {
        GothicCombatAPI.CombatInfo info = getCombatInfo(player);
        return info.getStateDescription();
    }
    
    // === УТИЛИТЫ ===
    
    /**
     * Отправляет обратную связь игроку о результате действия
     */
    private void sendFeedback(Player player, GothicCombatAPI.CombatResult result) {
        if (result.isSuccess()) {
            if (result.hasExtraInfo()) {
                sendMessage(player, result.getMessage() + " - " + result.getExtraInfo());
            } else {
                sendMessage(player, result.getMessage());
            }
        } else {
            sendErrorMessage(player, result.getMessage());
        }
    }
    
    /**
     * Отправляет сообщение игроку
     */
    private void sendMessage(Player player, String message) {
        // TODO: Интеграция с системой сообщений Minecraft
        LOGGER.info("Message to {}: {}", player.getName().getString(), message);
    }
    
    /**
     * Отправляет сообщение об ошибке игроку
     */
    private void sendErrorMessage(Player player, String message) {
        // TODO: Интеграция с системой сообщений Minecraft (красный цвет)
        LOGGER.warn("Error message to {}: {}", player.getName().getString(), message);
    }
    
    // === КОНФИГУРАЦИЯ ===
    
    private boolean enableAutoStance = true;
    private boolean enableComboNotifications = true;
    private boolean enableDebugMessages = false;
    
    public void setAutoStanceEnabled(boolean enabled) {
        this.enableAutoStance = enabled;
    }
    
    public void setComboNotificationsEnabled(boolean enabled) {
        this.enableComboNotifications = enabled;
    }
    
    public void setDebugMessagesEnabled(boolean enabled) {
        this.enableDebugMessages = enabled;
    }
    
    public boolean isAutoStanceEnabled() { return enableAutoStance; }
    public boolean isComboNotificationsEnabled() { return enableComboNotifications; }
    public boolean isDebugMessagesEnabled() { return enableDebugMessages; }
    
    // === СТАТИСТИКА И ОТЛАДКА ===
    
    /**
     * Получает количество активных игроков
     */
    public int getActivePlayerCount() {
        return activePlayers.size();
    }
    
    /**
     * Получает детальную отладочную информацию
     */
    public Map<String, Object> getControllerDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activePlayerCount", getActivePlayerCount());
        info.put("enableAutoStance", enableAutoStance);
        info.put("enableComboNotifications", enableComboNotifications);
        info.put("enableDebugMessages", enableDebugMessages);
        
        // Статистика по игрокам
        Map<String, Object> playerStats = new HashMap<>();
        for (Map.Entry<UUID, Player> entry : activePlayers.entrySet()) {
            PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(entry.getKey());
            if (stateMachine != null) {
                playerStats.put(entry.getValue().getName().getString(), stateMachine.getDebugInfo());
            }
        }
        info.put("playerStats", playerStats);
        
        return info;
    }
    
    /**
     * Очищает все данные контроллера
     */
    public void cleanup() {
        for (Player player : activePlayers.values()) {
            unregisterPlayer(player);
        }
        activePlayers.clear();
        LOGGER.info("CombatController cleaned up");
    }
}