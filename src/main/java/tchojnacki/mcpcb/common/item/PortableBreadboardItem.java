package tchojnacki.mcpcb.common.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.logic.BoardManager;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Item that places a complete breadboard (8x8 area) on use.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PortableBreadboardItem extends Item {
    public final static String ID = "portable_breadboard";

    public PortableBreadboardItem() {
        super(new Properties().tab(MCPCB.MAIN_GROUP));
    }

    /**
     * Called when player uses the portable breadboard on a block.
     *
     * @param context use context of item
     * @return result of the action
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player != null) {
            // Set item cooldown to make sure the item doesn't lag the game
            player.getCooldowns().addCooldown(this, 20);

            // Get initial position and direction (always place the breadboard forward and to the right)
            BlockPos startPosition = context.getClickedPos().relative(context.getClickedFace());
            Direction dir = context.getHorizontalDirection();
            int xStep = dir.getAxis() == Direction.Axis.X ? dir.getStepX() : dir.getClockWise().getStepX();
            int zStep = dir.getAxis() == Direction.Axis.Z ? dir.getStepZ() : dir.getClockWise().getStepZ();

            // Check if there is space to place the breadboard
            for (int x = 0; x != xStep * BoardManager.BOARD_SIZE; x += xStep) {
                for (int z = 0; z != zStep * BoardManager.BOARD_SIZE; z += zStep) {
                    BlockPos currentPos = startPosition.offset(x, 0, z);
                    if (!level.isEmptyBlock(currentPos)) {
                        if (!level.isClientSide()) {
                            ((ServerPlayer) player).sendMessage(
                                    new TranslatableComponent(String.format("util.%s.%s.space_occupied", MCPCB.MOD_ID, ID)),
                                    ChatType.GAME_INFO, Util.NIL_UUID
                            );
                        }

                        return InteractionResult.FAIL;
                    }
                }
            }

            // Place blocks on the server side
            if (!level.isClientSide()) {
                for (int x = 0; x != xStep * BoardManager.BOARD_SIZE; x += xStep) {
                    for (int z = 0; z != zStep * BoardManager.BOARD_SIZE; z += zStep) {
                        BlockPos currentPos = startPosition.offset(x, 0, z);

                        level.setBlockAndUpdate(currentPos, Registration.BREADBOARD_BLOCK.get().defaultBlockState());
                    }
                }
            }

            // Remove item unless you are using creative
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.FAIL;
    }
}
