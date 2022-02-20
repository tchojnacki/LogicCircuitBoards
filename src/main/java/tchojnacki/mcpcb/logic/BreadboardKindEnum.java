package tchojnacki.mcpcb.logic;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.StringRepresentable;

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
public enum BreadboardKindEnum implements StringRepresentable {
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
        return switch (this) {
            case NORMAL -> BoardSocket.State.Empty;
            case INPUT_NORTH, INPUT_EAST, INPUT_SOUTH, INPUT_WEST -> BoardSocket.State.Input;
            case OUTPUT_NORTH, OUTPUT_EAST, OUTPUT_SOUTH, OUTPUT_WEST -> BoardSocket.State.Output;
        };
    }

    public static BreadboardKindEnum getKindForSocket(BoardSocket socket) throws IllegalArgumentException {
        int data2D = socket.getDirection().get2DDataValue(); // 0 - S, 1 - W, 2 - N, 3 - E
        if (data2D == -1) {
            throw new IllegalArgumentException("Direction must be in the XZ plane.");
        }

        int socketNum = socket.getState().getNumber();

        return switch (data2D * 3 + socketNum) {
            case 0, 3, 6, 9 -> NORMAL;
            case 1 -> INPUT_SOUTH;
            case 2 -> OUTPUT_SOUTH;
            case 4 -> INPUT_WEST;
            case 5 -> OUTPUT_WEST;
            case 7 -> INPUT_NORTH;
            case 8 -> OUTPUT_NORTH;
            case 10 -> INPUT_EAST;
            case 11 -> OUTPUT_EAST;
            default -> throw new IllegalArgumentException("Illegal direction state combination.");
        };
    }

    @Override
    public String getSerializedName() {
        return switch (this) {
            case NORMAL -> "normal";
            case INPUT_NORTH -> "input_north";
            case INPUT_EAST -> "input_east";
            case INPUT_SOUTH -> "input_south";
            case INPUT_WEST -> "input_west";
            case OUTPUT_NORTH -> "output_north";
            case OUTPUT_EAST -> "output_east";
            case OUTPUT_SOUTH -> "output_south";
            case OUTPUT_WEST -> "output_west";
        };
    }
}
