package tchojnacki.mcpcb.logic;

import java.util.BitSet;
import java.util.Comparator;

/**
 * Compares two bitsets by the integer they represent.
 */
public class BitSetComparator implements Comparator<BitSet> {
    // Created based on https://stackoverflow.com/questions/27331175/java-bitset-comparison

    @Override
    public int compare(BitSet bitSet1, BitSet bitSet2) {
        if (bitSet1.length() > bitSet2.length()) { // The longer bitset obviously contains a bigger number
            return 1;
        } else if (bitSet2.length() > bitSet1.length()) {
            return -1;
        } else { // Bit sets have the same lengths, compare individual bits starting from most significant one
            for (int i = bitSet1.length() - 1; i >= 0; i--) {
                // First difference between the two, the bigger bitset will be the one containing a 1, where the other has a 0
                if (bitSet1.get(i) != bitSet2.get(i)) {
                    return bitSet1.get(i) ? 1 : -1;
                }
            }
            // Both sets were iterated fully and have no differences - they are equal
            return 0;
        }
    }
}
