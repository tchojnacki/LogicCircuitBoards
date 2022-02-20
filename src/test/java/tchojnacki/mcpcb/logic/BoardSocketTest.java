package tchojnacki.mcpcb.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class BoardSocketTest {
    private BoardSocket socket1, socket2, socket3;

    @BeforeEach
    void setUp() {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            blocks.add(BlockPos.ZERO.offset(i, 0, 0));
        }

        socket1 = new BoardSocket(Direction.NORTH, blocks, BoardSocket.State.Input);
        socket2 = new BoardSocket(Direction.EAST, blocks, BoardSocket.State.Output);
        socket3 = new BoardSocket(Direction.SOUTH, blocks, BoardSocket.State.Empty);
    }

    @Test
    void getState() {
        assertEquals(BoardSocket.State.Input, socket1.getState());
        assertEquals(BoardSocket.State.Output, socket2.getState());
        assertEquals(BoardSocket.State.Empty, socket3.getState());
    }

    @Test
    void getBlocks() {
        assertEquals(8, socket1.getBlocks().size());
    }

    @Test
    void getDirection() {
        assertEquals(Direction.NORTH, socket1.getDirection());
        assertEquals(Direction.EAST, socket2.getDirection());
        assertEquals(Direction.SOUTH, socket3.getDirection());
    }

    @Test
    void containsBlock() {
        assertTrue(socket1.containsBlock(new BlockPos(0, 0, 0)));
        assertTrue(socket1.containsBlock(new BlockPos(3, 0, 0)));
        assertTrue(socket1.containsBlock(new BlockPos(7, 0, 0)));
        assertFalse(socket1.containsBlock(new BlockPos(-1, 0, 0)));
        assertFalse(socket1.containsBlock(new BlockPos(3, 1, 0)));
        assertFalse(socket1.containsBlock(new BlockPos(10, 0, -10)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void fromNumber() {
        assertEquals(BoardSocket.State.Empty, BoardSocket.State.fromNumber(0));
        assertEquals(BoardSocket.State.Input, BoardSocket.State.fromNumber(1));
        assertEquals(BoardSocket.State.Output, BoardSocket.State.fromNumber(2));

        assertThrows(IllegalArgumentException.class, () -> BoardSocket.State.fromNumber(3));
        assertThrows(IllegalArgumentException.class, () -> BoardSocket.State.fromNumber(-1));
    }
}