package tchojnacki.mcpcb.logic;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SideBoolMapTest {
    @Test
    void getEmpty() {
        SideBoolMap empty = SideBoolMap.getEmpty();

        for (RelDir side : RelDir.values()) {
            assertFalse(empty.get(side));
        }

        assertFalse(empty.isntEmpty());
    }

    @Test
    void constructWith() {
        HashMap<RelDir, Boolean> testMap = new HashMap<>();

        testMap.put(RelDir.FRONT, true);
        testMap.put(RelDir.RIGHT, false);
        testMap.put(RelDir.LEFT, false);
        testMap.put(RelDir.BACK, true);

        SideBoolMap sideMap = SideBoolMap.constructWith(testMap::get);

        assertEquals(testMap.hashCode(), sideMap.hashCode());
    }

    @Test
    void constructFromIterable() {
        SideBoolMap sideMap = SideBoolMap.constructFromIterable(
                Collections.singletonList(RelDir.RIGHT),
                dir -> new AbstractMap.SimpleEntry<>(dir, true)
        );

        assertTrue(sideMap.get(RelDir.RIGHT));
    }

    @Test
    void testEquals() {
        SideBoolMap test1 = SideBoolMap.getEmpty();
        SideBoolMap test2 = SideBoolMap.getEmpty();

        assertEquals(test1, test2);
    }

    @Test
    void isntEmpty() {
        SideBoolMap sideMap = SideBoolMap.constructWith(dir -> dir == RelDir.FRONT);
        assertTrue(sideMap.isntEmpty());
    }

    @Test
    void byteSerialization() {
        SideBoolMap map = SideBoolMap.constructWith(dir -> dir == RelDir.LEFT || dir == RelDir.BACK);

        assertEquals(map, SideBoolMap.fromByte(map.toByte()));
        assertEquals(SideBoolMap.getEmpty(), SideBoolMap.fromByte(SideBoolMap.getEmpty().toByte()));
    }
}
