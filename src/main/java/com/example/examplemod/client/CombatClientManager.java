package com.example.examplemod.client;

import com.example.examplemod.client.qte.QTEClientManager;
import com.example.examplemod.client.qte.QTEEvent;
import com.example.examplemod.client.ui.QTEHUD;
import com.example.examplemod.client.ui.ResourceHUD;
import com.example.examplemod.core.PlayerStateMachine;
import com.example.examplemod.core.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatClientManager implements QTEClientManager.QTEEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatClientManager.class);
    private static CombatClientManager INSTANCE;
    
    private final Map<UUID, PlayerStateMachine> playerStates = new HashMap<>();
    private final Map<UUID, ResourceManager> playerResources = new HashMap<>();
    private final QTEClientManager qteManager;
    
    private QTEEvent currentQTE;
    
    private CombatClientManager() {
        this.qteManager = QTEClientManager.getInstance();
        this.qteManager.addListener(this);
    }
    
    public static CombatClientManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CombatClientManager();
        }
        return INSTANCE;
    }
    
    public void tick() {
        qteManager.tick();
        
        // Обновляем ресурсы
        for (ResourceManager resourceManager : playerResources.values()) {
            resourceManager.tick();
        }
        
        // Проверяем текущую QTE
        if (currentQTE != null && (currentQTE.isCompleted() || currentQTE.isExpired())) {
            currentQTE = null;
        }
        
        // Если нет активной QTE, берем первую доступную
        if (currentQTE == null) {
            var activeQTEs = qteManager.getAllActiveQTE();
            if (!activeQTEs.isEmpty()) {
                currentQTE = activeQTEs.iterator().next();
            }
        }
    }
    
    public void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        UUID playerId = player.getUUID();
        
        // Рендер полос ресурсов
        ResourceManager resourceManager = playerResources.get(playerId);
        if (resourceManager != null) {
            ResourceHUD.render(graphics, resourceManager, screenWidth, screenHeight);
        } else {
            // Используем тестовый рендер если нет данных сервера
            CombatHUDRenderer.render(graphics, screenWidth, screenHeight);
        }
        
        // Рендер QTE
        if (currentQTE != null) {
            QTEHUD.render(graphics, currentQTE, screenWidth, screenHeight);
        }
    }
    
    public boolean handleKeyInput(int keyCode, int scanCode, int action, int modifiers) {
        // Передаем ввод в QTE систему
        if (qteManager.hasActiveQTE()) {
            return qteManager.processKeyInput(keyCode);
        }
        
        return false;
    }
    
    // Управление состояниями игроков
    public PlayerStateMachine getPlayerState(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, id -> {
            ResourceManager resourceManager = getPlayerResources(id);
            return PlayerStateMachine.getInstance(id, resourceManager);
        });
    }
    
    public ResourceManager getPlayerResources(UUID playerId) {
        return playerResources.computeIfAbsent(playerId, id -> 
            new ResourceManager(id, 100f, 100f)); // Базовые значения
    }
    
    public void updatePlayerState(UUID playerId, com.example.examplemod.core.PlayerState state, String action, long stateChangeTime) {
        PlayerStateMachine stateMachine = getPlayerState(playerId);
        stateMachine.forceTransition(state, action);
        
        LOGGER.debug("Updated player {} state to {} (action: {})", playerId, state, action);
    }
    
    public void updatePlayerResources(UUID playerId, float currentMana, float reservedMana, float maxMana, 
                                     float currentStamina, float maxStamina) {
        ResourceManager resourceManager = getPlayerResources(playerId);
        resourceManager.syncMana(currentMana, reservedMana);
        resourceManager.syncStamina(currentStamina);
        
        LOGGER.debug("Updated player {} resources: mana={}/{} (reserved={}), stamina={}/{}", 
            playerId, currentMana, maxMana, reservedMana, currentStamina, maxStamina);
    }
    
    public void startQTE(UUID qteId, com.example.examplemod.client.qte.QTEType type, long duration, 
                        java.util.List<Integer> expectedKeys, int chainPosition) {
        qteManager.startQTE(qteId, type, duration, expectedKeys, chainPosition);
        LOGGER.debug("Started QTE {} of type {}", qteId, type);
    }
    
    public void cancelQTE(UUID qteId) {
        qteManager.cancelQTE(qteId);
        if (currentQTE != null && currentQTE.getId().equals(qteId)) {
            currentQTE = null;
        }
        LOGGER.debug("Cancelled QTE {}", qteId);
    }
    
    // QTE Event Listener implementation
    @Override
    public void onQTEStarted(QTEEvent event) {
        LOGGER.debug("QTE started: {} (type: {}, chain: {})", 
            event.getId(), event.getType(), event.getChainPosition());
        
        if (currentQTE == null) {
            currentQTE = event;
        }
    }
    
    @Override
    public void onQTECompleted(QTEEvent event) {
        LOGGER.debug("QTE completed: {} (score: {:.1f})", event.getId(), event.getScore());
        
        if (currentQTE != null && currentQTE.getId().equals(event.getId())) {
            currentQTE = null;
        }
        
        // TODO: Отправить результат на сервер
        // NetworkManager.sendQTEResult(event.getId(), event.getScore(), event.getScore() >= 50);
    }
    
    @Override
    public void onQTECancelled(QTEEvent event) {
        LOGGER.debug("QTE cancelled: {}", event.getId());
        
        if (currentQTE != null && currentQTE.getId().equals(event.getId())) {
            currentQTE = null;
        }
    }
    
    // Утилиты для дебага
    public void debugPrintState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        UUID playerId = mc.player.getUUID();
        PlayerStateMachine state = playerStates.get(playerId);
        ResourceManager resources = playerResources.get(playerId);
        
        LOGGER.info("=== Combat Client Debug ===");
        if (state != null) {
            LOGGER.info("State: {} (time in state: {}ms)", 
                state.getCurrentState(), state.getTimeInCurrentState());
        }
        if (resources != null) {
            LOGGER.info("Mana: {}/{} (reserved: {})", 
                resources.getCurrentMana(), resources.getMaxMana(), resources.getReservedMana());
            LOGGER.info("Stamina: {}/{}", 
                resources.getCurrentStamina(), resources.getMaxStamina());
        }
        LOGGER.info("Active QTEs: {}", qteManager.getActiveQTECount());
    }
}