package com.example.examplemod.core;

import com.example.examplemod.core.spells.*;
import com.example.examplemod.core.spells.forms.impl.*;
import com.example.examplemod.core.spells.forms.SpellFormType;
import com.example.examplemod.core.resources.*;
import com.example.examplemod.core.geometry.SpatialIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Центральная система боевой магии
 * Объединяет все Core Modules и предоставляет единый API
 */
public class CombatMagicSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatMagicSystem.class);
    private static CombatMagicSystem INSTANCE;

    // Core Modules
    private final SpellDefinitionRegistry spellRegistry;
    private final SpellFactory spellFactory;
    private final SpellLifecycleManager lifecycleManager;
    private final SpellStateCache stateCache;
    private final ResourceTransactionManager transactionManager;
    private final SpatialIndex spatialIndex;

    // Player data
    private final Map<UUID, ManaPool> playerManaPools = new ConcurrentHashMap<>();

    private boolean initialized = false;

    private CombatMagicSystem() {
        this.spellRegistry = new SpellDefinitionRegistry();
        this.spellFactory = new SpellFactory(spellRegistry);
        this.lifecycleManager = new SpellLifecycleManager();
        this.stateCache = new SpellStateCache();
        this.transactionManager = new ResourceTransactionManager();
        this.spatialIndex = new SpatialIndex(4.0); // 4-блочные ячейки
    }

    public static synchronized CombatMagicSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CombatMagicSystem();
        }
        return INSTANCE;
    }

    /**
     * Инициализация системы при запуске мода
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("Combat Magic System already initialized");
            return;
        }

        try {
            registerDefaultSpells();
            initialized = true;
            LOGGER.info("Combat Magic System initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Combat Magic System", e);
        }
    }

    /**
     * Завершение работы системы
     */
    public void shutdown() {
        if (!initialized) return;

        try {
            lifecycleManager.shutdown();
            transactionManager.clearAllTransactions();
            spatialIndex.clear();
            stateCache.clear();
            playerManaPools.clear();
            
            initialized = false;
            LOGGER.info("Combat Magic System shut down");
        } catch (Exception e) {
            LOGGER.error("Error during Combat Magic System shutdown", e);
        }
    }

    /**
     * Регистрация базовых заклинаний
     */
    private void registerDefaultSpells() {
        // Простой снаряд
        ResourceLocation fireballId = ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "fireball");
        SpellDefinition fireball = SpellDefinition.builder(fireballId)
            .form(SpellFormType.PROJECTILE)
            .parameters(createFireballParameters())
            .metadata(new SpellMetadata(
                Component.literal("Огненный шар"),
                Component.literal("Простой огненный снаряд")
            ))
            .build();
        spellRegistry.register(fireballId, fireball);

        // Простой барьер
        ResourceLocation barrierI = ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "magic_barrier");
        SpellDefinition barrier = SpellDefinition.builder(barrierI)
            .form(SpellFormType.BARRIER)
            .parameters(createBarrierParameters())
            .metadata(new SpellMetadata(
                Component.literal("Магический барьер"),
                Component.literal("Защитный барьер")
            ))
            .build();
        spellRegistry.register(barrierI, barrier);

        LOGGER.info("Registered {} default spells", spellRegistry.size());
    }

    private com.example.examplemod.core.spells.parameters.SpellParameters createFireballParameters() {
        var params = new com.example.examplemod.core.spells.parameters.SpellParameters();
        params.setParameter("damage", 20f);
        params.setParameter("range", 15f);
        params.setParameter("speed", 10f);
        params.setParameter("radius", 1.5f);
        return params;
    }

    private com.example.examplemod.core.spells.parameters.SpellParameters createBarrierParameters() {
        var params = new com.example.examplemod.core.spells.parameters.SpellParameters();
        params.setParameter("duration", 300); // 15 секунд
        params.setParameter("radius", 3f);
        params.setParameter("durability", 100f);
        return params;
    }

    /**
     * Получить или создать мана пул для игрока
     */
    public ManaPool getPlayerManaPool(Player player) {
        return playerManaPools.computeIfAbsent(player.getUUID(), 
            uuid -> new ManaPool(100f, 200f)); // Базовые значения
    }

    /**
     * API для применения заклинания
     */
    public boolean castSpell(Player caster, String spellName) {
        if (!initialized) {
            LOGGER.warn("Attempted to cast spell before system initialization");
            return false;
        }

        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("combatmetaphysics", spellName);
        var definition = spellRegistry.getDefinition(spellId);
        
        if (definition.isEmpty()) {
            LOGGER.warn("Unknown spell: {}", spellName);
            return false;
        }

        ManaPool manaPool = getPlayerManaPool(caster);
        var validation = ResourceValidator.validateCasting(definition.get(), caster, manaPool);
        
        if (!validation.isValid()) {
            LOGGER.debug("Spell casting validation failed: {}", validation.getReason());
            return false;
        }

        try {
            var spellInstance = spellFactory.createSpell(spellId, caster);
            if (spellInstance.isPresent()) {
                lifecycleManager.registerSpell(spellInstance.get());
                stateCache.updateCache(spellInstance.get());
                
                LOGGER.info("Successfully cast spell {} for player {}", 
                           spellName, caster.getName().getString());
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Error casting spell {} for player {}", spellName, caster.getName().getString(), e);
        }

        return false;
    }

    // Getters для доступа к модулям (для расширенного использования)
    public SpellDefinitionRegistry getSpellRegistry() { return spellRegistry; }
    public SpellFactory getSpellFactory() { return spellFactory; }
    public SpellLifecycleManager getLifecycleManager() { return lifecycleManager; }
    public SpellStateCache getStateCache() { return stateCache; }
    public ResourceTransactionManager getTransactionManager() { return transactionManager; }
    public SpatialIndex getSpatialIndex() { return spatialIndex; }

    public boolean isInitialized() { return initialized; }

    /**
     * Получить статистику системы
     */
    public SystemStats getStats() {
        return new SystemStats(
            spellRegistry.size(),
            lifecycleManager.getActiveSpellCount(),
            playerManaPools.size(),
            transactionManager.getActivePransactionCount(),
            spatialIndex.getStats()
        );
    }

    public record SystemStats(
        int registeredSpells,
        int activeSpells,
        int playersWithMana,
        int activeTransactions,
        SpatialIndex.IndexStats spatialStats
    ) {}
}