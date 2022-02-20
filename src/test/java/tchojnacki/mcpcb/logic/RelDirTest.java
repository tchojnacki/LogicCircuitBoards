package tchojnacki.mcpcb.logic;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RelDirTest {
    @Test
    void serialization() {
        RelDir[] testArray = new RelDir[]{RelDir.LEFT, RelDir.LEFT, RelDir.RIGHT, RelDir.FRONT, RelDir.RIGHT, RelDir.BACK};
        assertArrayEquals(testArray, RelDir.bytesToDirList(RelDir.dirListToBytes(Arrays.asList(testArray))).toArray());

        byte[] byteArray = new byte[]{1, 1, 2, 3, 1, 0, 0};
        assertArrayEquals(byteArray, RelDir.dirListToBytes(RelDir.bytesToDirList(byteArray)));
    }

    @Test
    void translationComponent() {
        assertEquals("util.mcpcb.direction.front", RelDir.FRONT.translationComponent().getKey());
        assertEquals("util.mcpcb.direction.right", RelDir.RIGHT.translationComponent().getKey());
        assertEquals("util.mcpcb.direction.back", RelDir.BACK.translationComponent().getKey());
        assertEquals("util.mcpcb.direction.left", RelDir.LEFT.translationComponent().getKey());
    }

    @Test
    void offsetFrom() {
        assertEquals(Direction.WEST, RelDir.LEFT.offsetFrom(Direction.NORTH));
        assertEquals(Direction.EAST, RelDir.RIGHT.offsetFrom(Direction.NORTH));
        assertEquals(Direction.SOUTH, RelDir.FRONT.offsetFrom(Direction.SOUTH));
        assertEquals(Direction.WEST, RelDir.BACK.offsetFrom(Direction.EAST));
        assertEquals(Direction.NORTH, RelDir.RIGHT.offsetFrom(Direction.WEST));
    }

    @Test
    void getClockwise() {
        assertEquals(RelDir.RIGHT, RelDir.FRONT.getClockWise());
        assertEquals(RelDir.FRONT, RelDir.LEFT.getClockWise());
    }

    @Test
    void getOffset() {
        assertEquals(RelDir.BACK, RelDir.getOffset(Direction.NORTH, Direction.SOUTH));
        assertEquals(RelDir.BACK, RelDir.getOffset(Direction.SOUTH, Direction.NORTH));
        assertEquals(RelDir.RIGHT, RelDir.getOffset(Direction.SOUTH, Direction.WEST));
        assertEquals(RelDir.LEFT, RelDir.getOffset(Direction.EAST, Direction.NORTH));
        assertEquals(RelDir.FRONT, RelDir.getOffset(Direction.WEST, Direction.WEST));
    }

    @Test
    void values() {
        assertArrayEquals(new RelDir[]{RelDir.FRONT, RelDir.RIGHT, RelDir.BACK, RelDir.LEFT}, RelDir.values());
    }
}