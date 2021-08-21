package tchojnacki.mcpcb.logic;

import com.google.common.collect.ImmutableList;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import tchojnacki.mcpcb.common.block.BreadboardBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * A class representing a single socket of a breadboard.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BoardSocket {
    private final Direction direction;
    private final ImmutableList<BlockPos> blocks;
    private State state;

    public BoardSocket(Direction direction, List<BlockPos> blocks, State state) {
        this.direction = direction;
        this.blocks = ImmutableList.copyOf(blocks);
        this.state = state;
    }

    /**
     * Change socket's state as well as update the blocks placed in the world to reflect it.
     *
     * @param state new state
     * @param world breadboard's world
     */
    public void setState(State state, World world) {
        this.state = state;

        BreadboardKindEnum kind = BreadboardKindEnum.getKindForSocket(this);
        for (BlockPos socketBlock : blocks) {
            if (world.getBlockState(socketBlock).getBlock() instanceof BreadboardBlock) {
                world.setBlockAndUpdate(socketBlock, world.getBlockState(socketBlock).setValue(BreadboardBlock.KIND, kind));
            }
        }
    }

    public State getState() {
        return state;
    }

    public ImmutableList<BlockPos> getBlocks() {
        return blocks;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean containsBlock(BlockPos targetPos) {
        for (BlockPos block : blocks) {
            if (block.equals(targetPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Enum holding the state of the breadboard socket.
     * A socket may be an input (1), output (2), or neither (0).
     * In some places state is passed as a pure integer because Minecraft's existing
     * codebase requires it (for instance {@link net.minecraft.util.IntReferenceHolder}).
     * In any other place, the enum is used.
     */
    public enum State {
        Empty(0),
        Input(1),
        Output(2);

        private final int number;

        State(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        public static State fromNumber(int number) throws IllegalArgumentException {
            switch (number) {
                case 0:
                    return Empty;
                case 1:
                    return Input;
                case 2:
                    return Output;
                default:
                    throw new IllegalArgumentException("Incorrect state number.");
            }
        }
    }
}
