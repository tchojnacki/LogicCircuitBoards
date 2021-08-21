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
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.logic.BoardManager;
import tchojnacki.mcpcb.logic.BoardManagerException;
import tchojnacki.mcpcb.common.container.ScrewdriverContainer;

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
    public ActionResultType useOn(ItemUseContext context) {
        World world = context.getLevel();
        PlayerEntity player = context.getPlayer();

        try {
            BoardManager boardManager = new BoardManager(world, context.getClickedPos());

            if (player != null && !world.isClientSide()) {
                IContainerProvider provider = (int winId, PlayerInventory _playerInv, PlayerEntity _playerEnt) -> ScrewdriverContainer.createContainerServerSide(winId, boardManager);
                INamedContainerProvider namedProvider = new SimpleNamedContainerProvider(provider, ScrewdriverContainer.TITLE);
                NetworkHooks.openGui((ServerPlayerEntity) player, namedProvider);
            }

            return ActionResultType.SUCCESS;
        } catch (BoardManagerException error) {
            if (player != null && !world.isClientSide()) {
                ((ServerPlayerEntity) player).sendMessage(
                        error.getTranslationTextComponent(),
                        ChatType.GAME_INFO, Util.NIL_UUID
                );
            }

            return ActionResultType.FAIL;
        }
    }
}
