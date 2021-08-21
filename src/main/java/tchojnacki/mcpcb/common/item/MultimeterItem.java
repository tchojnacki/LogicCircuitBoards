package tchojnacki.mcpcb.common.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.common.container.MultimeterContainer;
import tchojnacki.mcpcb.common.tileentities.CircuitBlockTileEntity;
import tchojnacki.mcpcb.logic.BoardManager;
import tchojnacki.mcpcb.logic.BoardManagerException;
import tchojnacki.mcpcb.logic.TruthTable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Multimeter item is used for circuit creation.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultimeterItem extends Item {
    public final static String ID = "multimeter";

    public MultimeterItem() {
        super(
                new Properties()
                        .stacksTo(1) // max stack size
                        .tab(MCPCB.MAIN_GROUP)
        );
    }

    /**
     * Called when player uses the multimeter on a block.
     *
     * @param context use context of item
     * @return result of the action
     * @see MultimeterContainer
     */
    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World world = context.getLevel();
        PlayerEntity player = context.getPlayer();

        final TruthTable truthTable;

        // Extract the table from the breadboard or show an error
        if (world.getBlockState(context.getClickedPos()).getBlock() instanceof CircuitBlock) {
            TileEntity tileEntity = world.getBlockEntity(context.getClickedPos());
            if (tileEntity instanceof CircuitBlockTileEntity) {
                CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;

                truthTable = circuitEntity.getTruthTable();
            } else {
                truthTable = null;
            }
        } else {
            try {
                BoardManager boardManager = new BoardManager(world, context.getClickedPos());
                TruthTable boardTable = boardManager.generateTruthTable(world);

                if (boardTable == null) {
                    throw new BoardManagerException("graph_is_cyclic");
                }

                truthTable = boardTable;
            } catch (BoardManagerException error) {
                TranslationTextComponent msg = error
                        .getTranslationTextComponent()
                        .getKey()
                        .equals("util.mcpcb.board_manager.error.target_isnt_breadboard")
                        ? new TranslationTextComponent("util.mcpcb.multimeter.target")
                        : error.getTranslationTextComponent();

                if (player != null && !world.isClientSide()) {
                    ((ServerPlayerEntity) player).sendMessage(
                            msg,
                            ChatType.GAME_INFO, Util.NIL_UUID
                    );
                }

                return ActionResultType.FAIL;
            }
        }

        if (truthTable == null) {
            return ActionResultType.FAIL;
        }

        // Create the container
        if (player != null && !world.isClientSide()) {
            IContainerProvider provider = (int winId, PlayerInventory playerInv, PlayerEntity _playerEnt) -> MultimeterContainer.createContainerServerSide(
                    winId, playerInv, IWorldPosCallable.create(world, context.getClickedPos()), truthTable
            );
            INamedContainerProvider namedProvider = new SimpleNamedContainerProvider(provider, MultimeterContainer.TITLE);
            NetworkHooks.openGui((ServerPlayerEntity) player, namedProvider, (packetBuffer) -> packetBuffer.writeNbt(truthTable.toNBT()));
        }

        return ActionResultType.SUCCESS;
    }
}
