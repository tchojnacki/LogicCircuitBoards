package tchojnacki.mcpcb.util;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.block.BreadboardBlock;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.common.container.MultimeterContainer;
import tchojnacki.mcpcb.common.container.ScrewdriverContainer;
import tchojnacki.mcpcb.common.item.MultimeterItem;
import tchojnacki.mcpcb.common.item.PortableBreadboardItem;
import tchojnacki.mcpcb.common.item.ScrewdriverItem;
import tchojnacki.mcpcb.common.tileentities.CircuitBlockTileEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

/**
 * Registration of things used on both sides.
 */
@SuppressWarnings("unused")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Registration {
    // TODO: All methods are static, make it impossible to instance this class

    // Deferred registers
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MCPCB.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MCPCB.MOD_ID);
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, MCPCB.MOD_ID);
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MCPCB.MOD_ID);

    // Blocks
    public static final RegistryObject<Block> BREADBOARD_BLOCK = Registration.BLOCKS.register(BreadboardBlock.ID, BreadboardBlock::new);
    public static final RegistryObject<Block> CIRCUIT_BLOCK = Registration.BLOCKS.register(CircuitBlock.ID, CircuitBlock::new);

    // Items
    public static final RegistryObject<Item> SCREWDRIVER_ITEM = Registration.ITEMS.register(ScrewdriverItem.ID, ScrewdriverItem::new);
    public static final RegistryObject<Item> PORTABLE_BREADBOARD_ITEM = Registration.ITEMS.register(PortableBreadboardItem.ID, PortableBreadboardItem::new);
    public static final RegistryObject<Item> MULTIMETER_ITEM = Registration.ITEMS.register(MultimeterItem.ID, MultimeterItem::new);

    // BlockItems
    public static final RegistryObject<Item> BREADBOARD_BLOCK_ITEM = registerBlockItem(BREADBOARD_BLOCK, true, null);
    public static final RegistryObject<Item> CIRCUIT_BLOCK_ITEM = registerBlockItem(CIRCUIT_BLOCK, false, CircuitBlock::onCrafted);

    // Containers
    public static final RegistryObject<ContainerType<ScrewdriverContainer>> SCREWDRIVER_CONTAINER = Registration.CONTAINERS.register(ScrewdriverContainer.ID, () -> IForgeContainerType.create(ScrewdriverContainer::createContainerClientSide));
    public static final RegistryObject<ContainerType<MultimeterContainer>> MULTIMETER_CONTAINER = Registration.CONTAINERS.register(MultimeterContainer.ID, () -> IForgeContainerType.create(MultimeterContainer::createContainerClientSide));

    // Tile entities
    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<CircuitBlockTileEntity>> CIRCUIT_BLOCK_TILE_ENTITY = Registration.TILE_ENTITIES.register(CircuitBlockTileEntity.ID, () -> TileEntityType.Builder.of(CircuitBlockTileEntity::new, CIRCUIT_BLOCK.get()).build(null));

    public static void register() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
        TILE_ENTITIES.register(modEventBus);

        PacketHandler.INSTANCE.registerMessage(
                0,
                MultimeterScreenRenamePacket.class,
                MultimeterScreenRenamePacket::encode,
                MultimeterScreenRenamePacket::decode,
                MultimeterScreenRenamePacket::handle
        );

        // There is no deferred registry for criteria, so register our criterion directly
        CriteriaTriggers.register(CircuitCreateTrigger.TRIGGER);
    }

    /**
     * Helper method for block item registration.
     *
     * @param block block for which we register the block item
     * @param putInGroup whether to put the item in the creative tab
     * @param onCrafted optional handler for "onCraftedBy" event, pass null if unused
     * @return registry object containing the block item
     */
    private static RegistryObject<Item> registerBlockItem(RegistryObject<Block> block, boolean putInGroup, @Nullable BiConsumer<ItemStack, PlayerEntity> onCrafted) {
        return Registration.ITEMS.register(
                block.getId().getPath(),
                () -> new BlockItem(
                        block.get(),
                        putInGroup ? new Item.Properties().tab(MCPCB.MAIN_GROUP) : new Item.Properties()
                ) {
                    @Override
                    public void onCraftedBy(ItemStack itemStack, World world, PlayerEntity playerEntity) {
                        super.onCraftedBy(itemStack, world, playerEntity);

                        if (onCrafted != null) {
                            onCrafted.accept(itemStack, playerEntity);
                        }
                    }
                }
        );
    }
}
