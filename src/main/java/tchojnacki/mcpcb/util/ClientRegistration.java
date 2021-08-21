package tchojnacki.mcpcb.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import tchojnacki.mcpcb.client.models.CircuitBlockModel;
import tchojnacki.mcpcb.client.models.CircuitItemBaseModel;
import tchojnacki.mcpcb.client.models.CircuitTopFaceBakery;
import tchojnacki.mcpcb.client.screen.MultimeterContainerScreen;
import tchojnacki.mcpcb.client.screen.ScrewdriverContainerScreen;

/**
 * Responsible for registering client-side only things: screens, models, textures.
 */
public class ClientRegistration {
    public static void register() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.register(ClientRegistration.class);
    }

    @SubscribeEvent
    public static void onClientSetupEvent(FMLClientSetupEvent event) {
        // Register screens for our containers

        ScreenManager.register(Registration.SCREWDRIVER_CONTAINER.get(), ScrewdriverContainerScreen::new);
        ScreenManager.register(Registration.MULTIMETER_CONTAINER.get(), MultimeterContainerScreen::new);
    }

    /**
     * Change default models loaded from JSON files to our custom models (for circuit block and item).
     */
    @SubscribeEvent
    public static void onModelBakeEvent(ModelBakeEvent event) {
        /*
        Based on:
        https://github.com/TheGreyGhost/MinecraftByExample/blob/e9862e606f6306463fccde5e3ebe576ea88f0745/src/main/java/minecraftbyexample/mbe04_block_dynamic_block_models/StartupClientOnly.java#L33
        https://github.com/TheGreyGhost/MinecraftByExample/blob/e9862e606f6306463fccde5e3ebe576ea88f0745/src/main/java/minecraftbyexample/mbe15_item_dynamic_item_model/StartupClientOnly.java#L22
         */

        // Replace block model
        for (BlockState blockState : Registration.CIRCUIT_BLOCK.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShapes.stateToModelLocation(blockState);
            IBakedModel baseModel = event.getModelRegistry().get(location);

            if (baseModel != null && !(baseModel instanceof CircuitBlockModel)) {
                event.getModelRegistry().put(location, new CircuitBlockModel(baseModel));
            }
        }

        // Replace item model
        ModelResourceLocation itemModelResourceLocation = CircuitItemBaseModel.MODEL_RESOURCE_LOCATION;
        IBakedModel baseModel = event.getModelRegistry().get(itemModelResourceLocation);
        if (baseModel != null && !(baseModel instanceof CircuitItemBaseModel)) {
            event.getModelRegistry().put(itemModelResourceLocation, new CircuitItemBaseModel(baseModel));
        }
    }

    /**
     * Inject our custom circuit textures (which aren't mentioned in JSON files) into block atlas.
     *
     * @see CircuitTopFaceBakery
     */
    @SubscribeEvent
    public static void onTextureStitchEvent(TextureStitchEvent.Pre event) {
        if (event.getMap().location() == PlayerContainer.BLOCK_ATLAS) {
            event.addSprite(CircuitTopFaceBakery.CORNER_TEXTURE);
            event.addSprite(CircuitTopFaceBakery.SOCKET_TEXTURE);

            for (ResourceLocation resource : CircuitTopFaceBakery.CENTER_TEXTURE_MAP.values()) {
                event.addSprite(resource);
            }
        }
    }
}
