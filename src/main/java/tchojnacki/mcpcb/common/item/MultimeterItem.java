package tchojnacki.mcpcb.common.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.common.container.MultimeterContainer;
import tchojnacki.mcpcb.common.block.entities.CircuitBlockEntity;
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
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        final TruthTable truthTable;

        // Extract the table from the breadboard or show an error
        if (level.getBlockState(context.getClickedPos()).getBlock() instanceof CircuitBlock) {
            BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
            if (blockEntity instanceof CircuitBlockEntity circuitEntity) {
                truthTable = circuitEntity.getTruthTable();
            } else {
                truthTable = null;
            }
        } else {
            try {
                BoardManager boardManager = new BoardManager(level, context.getClickedPos());
                TruthTable boardTable = boardManager.generateTruthTable(level);

                if (boardTable == null) {
                    throw new BoardManagerException("graph_is_cyclic");
                }

                truthTable = boardTable;
            } catch (BoardManagerException error) {
                TranslatableComponent msg = error
                        .getTranslationTextComponent()
                        .getKey()
                        .equals("util.mcpcb.board_manager.error.target_isnt_breadboard")
                        ? new TranslatableComponent("util.mcpcb.multimeter.target")
                        : error.getTranslationTextComponent();

                if (player != null && !level.isClientSide()) {
                    ((ServerPlayer) player).sendMessage(
                            msg,
                            ChatType.GAME_INFO, Util.NIL_UUID
                    );
                }

                return InteractionResult.FAIL;
            }
        }

        if (truthTable == null) {
            return InteractionResult.FAIL;
        }

        // Create the container
        if (player != null && !level.isClientSide()) {
            MenuConstructor provider = (int winId, Inventory playerInv, Player _playerEnt) -> MultimeterContainer.createContainerServerSide(
                    winId, playerInv, truthTable
            );
            MenuProvider namedProvider = new SimpleMenuProvider(provider, MultimeterContainer.TITLE);
            NetworkHooks.openGui((ServerPlayer) player, namedProvider, (packetBuffer) -> packetBuffer.writeNbt(truthTable.toNBT()));
        }

        return InteractionResult.SUCCESS;
    }
}
