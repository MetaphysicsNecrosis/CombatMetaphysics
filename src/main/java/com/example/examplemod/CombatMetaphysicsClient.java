package com.example.examplemod;

import com.example.examplemod.client.CombatClientManager;
import com.example.examplemod.client.CombatHUDRenderer;
import com.example.examplemod.client.qte.QTEClientManager;
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
        
        // Инициализируем клиентский менеджер combat системы
        CombatClientManager.getInstance();
        CombatMetaphysics.LOGGER.info("Combat Client Manager initialized");
        
        // Регистрируем обработчики событий
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onKeyInput);
        CombatMetaphysics.LOGGER.info("Event handlers registered (HUD, Tick, KeyInput)");
    }
    
    /**
     * SINGLEPLAYER: Главный обработчик тиков клиента для OSU QTE системы
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        // Обновляем Combat Client Manager каждый тик
        CombatClientManager.getInstance().tick();
    }
    
    /**
     * SINGLEPLAYER: Обработчик рендеринга HUD с OSU QTE
     */
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.options.hideGui) {
            // Используем новый интегрированный рендер через CombatClientManager
            CombatClientManager.getInstance().renderHUD(
                event.getGuiGraphics(), 
                mc.getWindow().getGuiScaledWidth(), 
                mc.getWindow().getGuiScaledHeight()
            );
        }
    }
    
    /**
     * SINGLEPLAYER: Обработчик ввода клавиш для OSU QTE
     */
    public static void onKeyInput(InputEvent.Key event) {
        // Передаем все нажатия клавиш в Combat Client Manager
        // Он сам решит, нужно ли их обрабатывать (если есть активное QTE)
        if (event.getAction() == 1) { // GLFW.GLFW_PRESS
            boolean handled = CombatClientManager.getInstance().handleKeyInput(
                event.getKey(), 
                event.getScanCode(), 
                event.getAction(), 
                event.getModifiers()
            );
            
            // Если QTE обработало клавишу, отменяем дальнейшую обработку
            // Примечание: В NeoForge InputEvent.Key не поддерживает setCanceled напрямую
            // QTE система сама управляет обработкой клавиш
        }
    }
}
