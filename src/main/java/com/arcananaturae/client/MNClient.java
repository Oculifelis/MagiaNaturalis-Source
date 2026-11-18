package com.arcananaturae.client;

import com.arcananaturae.MNRegistry;
import com.arcananaturae.ArcanaNaturae;
import com.arcananaturae.client.trunk.EvilTrunkModel;
import com.arcananaturae.client.trunk.EvilTrunkRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ArcanaNaturae.MODID, value = Dist.CLIENT)
public final class MNClient {
    private MNClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ClientAutotest.init();
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MNRegistry.GREATWOOD_CHEST_MENU.get(), ArcaneChestScreen::new);
        event.register(MNRegistry.SILVERWOOD_CHEST_MENU.get(), ArcaneChestScreen::new);
        event.register(MNRegistry.TRANSCRIBING_TABLE_MENU.get(), TranscribingTableScreen::new);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // A revenant is a baby zombie and looks like one.
        event.registerEntityRenderer(MNRegistry.REVENANT.get(), (EntityRendererProvider) ZombieRenderer::new);
        event.registerEntityRenderer(MNRegistry.EVIL_TRUNK.get(), EvilTrunkRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EvilTrunkRenderer.LAYER, EvilTrunkModel::createLayer);
    }

    @SubscribeEvent
    public static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(ArcanaNaturae.id("connected"), ConnectedModel.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ArcanaNaturae.id("spectacles"), SpectaclesHud::render);
    }
}
