package tchojnacki.mcpcb.common.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
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
import net.minecraftforge.network.NetworkHooks;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.container.ScrewdriverContainer;
import tchojnacki.mcpcb.logic.BoardManager;
import tchojnacki.mcpcb.logic.BoardManagerException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Screwdriver item is used for circuit customization.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ScrewdriverItem extends Item {
    public final static String ID = "screwdriver";

    public ScrewdriverItem() {
        super(
                new Properties()
                        .stacksTo(1)
                        .tab(MCPCB.MAIN_GROUP)
        );
    }

    /**
     * Called when player uses the screwdriver on a block.
     *
     * @param context use context of item
     * @return result of the action
     * @see ScrewdriverContainer
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();

        try {
            BoardManager boardManager = new BoardManager(world, context.getClickedPos());

            if (player != null && !world.isClientSide()) {
                MenuConstructor provider = (int winId, Inventory _playerInv, Player _playerEnt) -> ScrewdriverContainer.createContainerServerSide(winId, boardManager);
                MenuProvider namedProvider = new SimpleMenuProvider(provider, ScrewdriverContainer.TITLE);
                NetworkHooks.openGui((ServerPlayer) player, namedProvider);
            }

            return InteractionResult.SUCCESS;
        } catch (BoardManagerException error) {
            if (player != null && !world.isClientSide()) {
                ((ServerPlayer) player).sendMessage(
                        error.getTranslationTextComponent(),
                        ChatType.GAME_INFO, Util.NIL_UUID
                );
            }

            return InteractionResult.FAIL;
        }
    }
}
