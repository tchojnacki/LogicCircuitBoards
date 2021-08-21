package tchojnacki.mcpcb.logic;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.IStringSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Enum describing breadboards kind.
 * The value is stored in the breadboard block's block state as a property.
 * It combines the information about block's direction and its state.
 * (basically there is an enum variant for each possible breadboard block's look)
 *
 * @see BoardSocket.State
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum BreadboardKindEnum implements IStringSerializable {
    NORMAL,
    // Inputs
    INPUT_NORTH,
    INPUT_EAST,
    INPUT_SOUTH,
    INPUT_WEST,
    // Outputs
    OUTPUT_NORTH,
    OUTPUT_EAST,
    OUTPUT_SOUTH,
    OUTPUT_WEST;

    public BoardSocket.State getState() {
        switch (this) {
            case NORMAL:
                return BoardSocket.State.Empty;
            case INPUT_NORTH:
            case INPUT_EAST:
            case INPUT_SOUTH:
            case INPUT_WEST:
                return BoardSocket.State.Input;
            case OUTPUT_NORTH:
            case OUTPUT_EAST:
            case OUTPUT_SOUTH:
            case OUTPUT_WEST:
                return BoardSocket.State.Output;
            default:
                throw new AssertionError("Illegal breadboard kind.");
        }
    }

    public static BreadboardKindEnum getKindForSocket(BoardSocket socket) throws IllegalArgumentException {
        int data2D = socket.getDirection().get2DDataValue(); // 0 - S, 1 - W, 2 - N, 3 - E
        if (data2D == -1) {
            throw new IllegalArgumentException("Direction must be in the XZ plane.");
        }

        int socketNum = socket.getState().getNumber();

        switch (data2D * 3 + socketNum) {
            case 0:
            case 3:
            case 6:
            case 9:
                return NORMAL;
            case 1:
                return INPUT_SOUTH;
            case 2:
                return OUTPUT_SOUTH;
            case 4:
                return INPUT_WEST;
            case 5:
                return OUTPUT_WEST;
            case 7:
                return INPUT_NORTH;
            case 8:
                return OUTPUT_NORTH;
            case 10:
                return INPUT_EAST;
            case 11:
                return OUTPUT_EAST;
            default:
                throw new IllegalArgumentException("Illegal direction state combination.");
        }
    }

    @Override
    public String getSerializedName() {
        switch (this) {
            case NORMAL:
                return "normal";
            case INPUT_NORTH:
                return "input_north";
            case INPUT_EAST:
                return "input_east";
            case INPUT_SOUTH:
                return "input_south";
            case INPUT_WEST:
                return "input_west";
            case OUTPUT_NORTH:
                return "output_north";
            case OUTPUT_EAST:
                return "output_east";
            case OUTPUT_SOUTH:
                return "output_south";
            case OUTPUT_WEST:
                return "output_west";
            default:
                throw new AssertionError("Illegal breadboard kind.");
        }
    }
}
