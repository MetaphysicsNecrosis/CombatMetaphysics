package com.example.examplemod;

import com.example.examplemod.client.CombatClientManager;
import com.example.examplemod.client.CombatHUDRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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
        
        // Регистрируем обработчик рендера HUD
        NeoForge.EVENT_BUS.addListener(CombatMetaphysicsClient::onRenderGui);
        CombatMetaphysics.LOGGER.info("HUD Renderer registered");
    }
    
    public static void onRenderGui(RenderGuiEvent.Post event) {
        // Принудительно рендерим HUD всегда
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.options.hideGui) {
            CombatHUDRenderer.render(
                event.getGuiGraphics(), 
                mc.getWindow().getGuiScaledWidth(), 
                mc.getWindow().getGuiScaledHeight()
            );
        }
    }
}
