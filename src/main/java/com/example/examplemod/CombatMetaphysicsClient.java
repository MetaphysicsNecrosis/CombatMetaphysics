package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CombatMetaphysics.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = CombatMetaphysics.MODID, value = Dist.CLIENT)
public class CombatMetaphysicsClient {
    public CombatMetaphysicsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        CombatMetaphysics.LOGGER.info("HELLO FROM CLIENT SETUP");
        CombatMetaphysics.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        
        // TODO: Инициализировать клиентские компоненты системы магии когда они будут готовы
        CombatMetaphysics.LOGGER.info("Combat Magic Client components will be initialized here");
        
        // Регистрируем обработчики событий
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onKeyInput);
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onPlayerLoggedOut);
        CombatMetaphysics.LOGGER.info("Event handlers registered (HUD, Tick, KeyInput, Player Events)");
    }
    
    /**
     * Главный обработчик тиков клиента для системы магии
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        // TODO: Обновлять клиентские компоненты каждый тик
        // Пока просто проверяем что система инициализирована
        var system = com.example.examplemod.core.CombatMagicSystem.getInstance();
        if (system.isInitialized()) {
            // Система готова для клиентских операций
        }
    }
    
    /**
     * Обработчик рендеринга HUD
     */
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.options.hideGui) {
            // TODO: Рендерить HUD элементы системы магии
            // - Полоски маны
            // - QTE интерфейс
            // - Активные заклинания
        }
    }
    
    /**
     * Обработчик ввода клавиш
     */
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // TODO: Обработка клавиш для системы магии
        // - QTE события
        // - Быстрые заклинания
        // - Отмена заклинаний
        
        if (event.getAction() == 1) { // GLFW.GLFW_PRESS
            // Обработка нажатия клавиш
        } else if (event.getAction() == 0) { // GLFW.GLFW_RELEASE
            // Обработка отпускания клавиш
        }
    }
    
    /**
     * Регистрация игрока при входе в мир
     */
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (event.getPlayer() != null) {
            // TODO: Инициализация клиентских данных игрока
            CombatMetaphysics.LOGGER.info("Player {} logged in - initializing client magic system", 
                event.getPlayer().getName().getString());
        }
    }
    
    /**
     * Отмена регистрации при выходе из мира
     */
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (event.getPlayer() != null) {
            // TODO: Очистка клиентских данных игрока
            CombatMetaphysics.LOGGER.info("Player {} logged out - cleaning up client magic system", 
                event.getPlayer().getName().getString());
        }
    }
}