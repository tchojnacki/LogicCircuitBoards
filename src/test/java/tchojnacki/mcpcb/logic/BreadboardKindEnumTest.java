package tchojnacki.mcpcb.logic;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class BreadboardKindEnumTest {

    @Test
    void getState() {
        assertEquals(BoardSocket.State.Empty, BreadboardKindEnum.NORMAL.getState());
        assertEquals(BoardSocket.State.Input, BreadboardKindEnum.INPUT_EAST.getState());
        assertEquals(BoardSocket.State.Input, BreadboardKindEnum.INPUT_NORTH.getState());
        assertEquals(BoardSocket.State.Output, BreadboardKindEnum.OUTPUT_SOUTH.getState());
        assertEquals(BoardSocket.State.Output, BreadboardKindEnum.OUTPUT_WEST.getState());
    }

    @Test
    void getKindForSocket() {
        ArrayList<BlockPos> mockBlocks = new ArrayList<>(Collections.emptyList());

        BoardSocket exampleSocket = new BoardSocket(Direction.NORTH, mockBlocks, BoardSocket.State.Output);
        assertEquals(BreadboardKindEnum.OUTPUT_NORTH, BreadboardKindEnum.getKindForSocket(exampleSocket));

        BoardSocket invalidSocket = new BoardSocket(Direction.UP, mockBlocks, BoardSocket.State.Empty);
        assertThrows(IllegalArgumentException.class, () -> BreadboardKindEnum.getKindForSocket(invalidSocket));
    }

    @Test
    void getSerializedName() {
        assertEquals("normal", BreadboardKindEnum.NORMAL.getSerializedName());
        assertEquals("input_north", BreadboardKindEnum.INPUT_NORTH.getSerializedName());
        assertEquals("input_east", BreadboardKindEnum.INPUT_EAST.getSerializedName());
        assertEquals("input_south", BreadboardKindEnum.INPUT_SOUTH.getSerializedName());
        assertEquals("input_west", BreadboardKindEnum.INPUT_WEST.getSerializedName());
        assertEquals("output_north", BreadboardKindEnum.OUTPUT_NORTH.getSerializedName());
        assertEquals("output_east", BreadboardKindEnum.OUTPUT_EAST.getSerializedName());
        assertEquals("output_south", BreadboardKindEnum.OUTPUT_SOUTH.getSerializedName());
        assertEquals("output_west", BreadboardKindEnum.OUTPUT_WEST.getSerializedName());
    }
}
