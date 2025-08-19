package com.example.examplemod.client;

import com.example.examplemod.client.qte.QTEClientManager;
import com.example.examplemod.client.qte.QTEEvent;
import com.example.examplemod.client.qte.OSUStyleQTEEvent;
import com.example.examplemod.client.qte.QTEVisualizer;
import com.example.examplemod.client.qte.QTESoundManager;
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

public class CombatClientManager implements QTEClientManager.OSUQTEEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatClientManager.class);
    private static CombatClientManager INSTANCE;
    
    private final Map<UUID, PlayerStateMachine> playerStates = new HashMap<>();
    private final Map<UUID, ResourceManager> playerResources = new HashMap<>();
    private final QTEClientManager qteManager;
    private final QTESoundManager soundManager;
    
    // Legacy QTE support  
    private QTEEvent currentQTE;
    
    // OSU-style QTE support
    private OSUStyleQTEEvent currentOSUQTE;
    
    private CombatClientManager() {
        this.qteManager = QTEClientManager.getInstance();
        this.soundManager = QTESoundManager.getInstance();
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
        
        // Обновляем OSU-style QTE
        updateOSUQTE();
        
        // Legacy QTE support
        updateLegacyQTE();
    }
    
    /**
     * SINGLEPLAYER: Обновление текущего OSU-style QTE
     */
    private void updateOSUQTE() {
        // Проверяем текущее OSU QTE
        if (currentOSUQTE != null && currentOSUQTE.isCompleted()) {
            currentOSUQTE = null;
        }
        
        // Если нет активного OSU QTE, берем первое доступное
        if (currentOSUQTE == null) {
            var activeOSUQTEs = qteManager.getActiveOSUQTEs();
            if (!activeOSUQTEs.isEmpty()) {
                currentOSUQTE = activeOSUQTEs.iterator().next();
            }
        }
    }
    
    /**
     * Backward compatibility: обновление legacy QTE
     */
    private void updateLegacyQTE() {
        // Проверяем текущую legacy QTE
        if (currentQTE != null && (currentQTE.isCompleted() || currentQTE.isExpired())) {
            currentQTE = null;
        }
        
        // Если нет активной legacy QTE, берем первую доступную
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
        
        // Рендер OSU-style QTE (приоритет над legacy)
        if (currentOSUQTE != null && currentOSUQTE.isActive()) {
            QTEVisualizer visualizer = qteManager.getVisualizer();
            visualizer.renderQTE(graphics, currentOSUQTE.getHitPoints(), currentOSUQTE.getStartTime());
        } 
        // Backward compatibility: рендер legacy QTE
        else if (currentQTE != null) {
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
        LOGGER.info("Active OSU QTEs: {}", qteManager.getActiveOSUQTEs().size());
    }
    
    // === OSU-STYLE QTE EVENT LISTENERS ===
    
    @Override
    public void onOSUQTEStarted(OSUStyleQTEEvent qteEvent) {
        LOGGER.debug("OSU QTE started: {} (type: {}, spell: {})", 
            qteEvent.getEventId(), qteEvent.getQTEType(), qteEvent.getSpellName());
        
        if (currentOSUQTE == null) {
            currentOSUQTE = qteEvent;
        }
        
        // Проигрываем звук начала QTE
        soundManager.playQTEStart();
    }
    
    @Override
    public void onOSUQTECompleted(OSUStyleQTEEvent qteEvent) {
        var result = qteEvent.getFinalResult();
        LOGGER.debug("OSU QTE completed: {} - Overall efficiency: {:.1f}%, Success: {}", 
            qteEvent.getEventId(), result.getOverallEfficiency() * 100, result.isSuccess());
        
        if (currentOSUQTE != null && currentOSUQTE.getEventId().equals(qteEvent.getEventId())) {
            currentOSUQTE = null;
        }
        
        // Проигрываем звук завершения
        if (result.isSuccess()) {
            soundManager.playQTEComboSuccess();
        } else {
            soundManager.playQTEComboFailed();
        }
        
        // TODO: SINGLEPLAYER - применяем результат к ресурсам игрока локально
        // applyQTEResultToResources(qteEvent, result);
    }
    
    @Override
    public void onOSUQTECancelled(OSUStyleQTEEvent qteEvent) {
        LOGGER.debug("OSU QTE cancelled: {}", qteEvent.getEventId());
        
        if (currentOSUQTE != null && currentOSUQTE.getEventId().equals(qteEvent.getEventId())) {
            currentOSUQTE = null;
        }
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Получает текущее активное OSU QTE
     */
    public OSUStyleQTEEvent getCurrentOSUQTE() {
        return currentOSUQTE;
    }
    
    /**
     * Проверяет, активно ли OSU QTE
     */
    public boolean hasActiveOSUQTE() {
        return currentOSUQTE != null && currentOSUQTE.isActive();
    }
    
    /**
     * Запускает тестовое OSU QTE (для debug команд)
     */
    public UUID startTestOSUQTE(OSUStyleQTEEvent.QTEType type, String spellName) {
        return qteManager.startOSUStyleQTE(type, spellName);
    }
}