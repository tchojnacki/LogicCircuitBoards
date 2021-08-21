package tchojnacki.mcpcb.logic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class BitSetComparatorTest {
    @Test
    void compare() {
        BitSet bitSet1 = BitSet.valueOf(new byte[] {3});
        BitSet bitSet2 = BitSet.valueOf(new byte[] {1, 2});
        BitSet bitSet3 = BitSet.valueOf(new byte[] {2, 2});

        BitSetComparator comparator = new BitSetComparator();

        assertTrue(comparator.compare(bitSet1, bitSet2) < 0);
        assertTrue(comparator.compare(bitSet2, bitSet3) < 0);
        assertTrue(comparator.compare(bitSet3, bitSet1) > 0);
        //noinspection EqualsWithItself
        assertEquals(0, comparator.compare(bitSet1, bitSet1));


        BitSet[] bitSets = new BitSet[] {bitSet3, bitSet1, bitSet2};
        BitSet[] correctlySortedSet = new BitSet[] {bitSet1, bitSet2, bitSet3};

        BitSet[] comparatorSortedSet = Arrays.copyOf(bitSets, 3);
        Arrays.sort(comparatorSortedSet, comparator);

        assertArrayEquals(correctlySortedSet, comparatorSortedSet);
    }
}