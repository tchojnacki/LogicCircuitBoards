package tchojnacki.mcpcb.logic;

import com.google.common.collect.ImmutableList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class representing a truth table, with sides of its inputs and outputs.
 * It holds at most 4 (one for each square side) inputs + outputs in sum.
 * <p>
 * If we have n inputs and m outputs then the table has:
 * - 2^n rows
 * - n + m columns:
 * - n columns for input states
 * - m columns for output states
 * Where each table cell is either a 0 or a 1.
 * The first n columns are implicit and never stored, because they only hold the "index"
 * of the input set (if we were to store them we would get the following array:
 * {0, 1, 2, 3, 4, ...}). Thus we end up with a table of m columns by 2^n rows.
 * It is then internally stored as a list of bitsets, where each list element (a bitset)
 * represents a single column. It means that the list's length is m, while each
 * bitset has a size o 2^n.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Truth_table">Wikipedia - Truth table</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TruthTable {
    private final ImmutableList<RelDir> inputs;
    private final ImmutableList<RelDir> outputs;

    /**
     * The internal representation of the truth table - a list of bitsets.
     * Each list element corresponds to one output (i-th mapping describes i-th output in {@link #outputs}).
     * <p>
     * Each bitset describes the value of a given output for each input set.
     * That means that length of each bit set is equal to 2^n = 1 << n, where n is the number of inputs,
     * to store information about every combination of input states. Value of 1 at i-th bit means that
     * that input set gives a true output, while 0 means false.
     */
    private final ImmutableList<BitSet> mappings; // TODO: For our purposes the bitset would fit into a primitive type, it might be faster

    public TruthTable(List<RelDir> inputs, List<RelDir> outputs, List<BitSet> mappings) {
        this.inputs = ImmutableList.copyOf(inputs);
        this.outputs = ImmutableList.copyOf(outputs);
        this.mappings = ImmutableList.copyOf(mappings);
    }

    /**
     * Constructs an empty truth table (truth table of a blank circuit).
     *
     * @return empty truth table
     */
    public static TruthTable empty() {
        return new TruthTable(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Construct a truth table using a boolean function.
     * Length of {@code outputs} must be the same as {@code funcs}, and their indexes correspond.
     *
     * @param inputs  list of table inputs
     * @param outputs list of table outputs
     * @param funcs   list of functions from list of booleans to a single output boolean used to generate mappings
     * @return truth table fulfilling the function set
     */
    public static TruthTable fromBoolFunc(List<RelDir> inputs, List<RelDir> outputs, List<Function<List<Boolean>, Boolean>> funcs) {
        int inputSetCount = 1 << inputs.size();

        ArrayList<BitSet> mappings = new ArrayList<>();

        for (Function<List<Boolean>, Boolean> func : funcs) {
            BitSet bitSet = new BitSet(inputSetCount);

            for (int inputSet = 0; inputSet < inputSetCount; inputSet++) {
                ArrayList<Boolean> arguments = new ArrayList<>();

                for (int bit = 0; bit < inputs.size(); bit++) {
                    arguments.add((inputSet & (1 << bit)) > 0);
                }

                bitSet.set(inputSet, func.apply(arguments));
            }

            mappings.add(bitSet);
        }

        return new TruthTable(new ArrayList<>(inputs), new ArrayList<>(outputs), mappings);
    }

    /**
     * @see #fromBoolFunc(List, List, List)
     */
    public static TruthTable fromBoolFunc(List<RelDir> inputs, RelDir output, Function<List<Boolean>, Boolean> func) {
        return fromBoolFunc(inputs, Collections.singletonList(output), Collections.singletonList(func));
    }

    /**
     * @see #fromBoolFunc(List, List, List)
     */
    public static TruthTable fromBoolFunc(RelDir input, RelDir output, Function<List<Boolean>, Boolean> func) {
        return fromBoolFunc(Collections.singletonList(input), Collections.singletonList(output), Collections.singletonList(func));
    }

    /**
     * Calculate outputs for a given input set.
     *
     * @param inputSet integer representing the set of inputs, where i-th bit corresponds to i-th input's state
     * @return list of values of outputs for given inputs
     */
    private ImmutableList<Boolean> outputsForInputSet(int inputSet) {
        final var builder = ImmutableList.<Boolean>builder();
        mappings.forEach(b -> builder.add(b.get(inputSet)));
        return builder.build();
    }

    /**
     * Map a side boolean map of input states to a side boolean map of output states according to the truth table.
     *
     * @param inputMap side boolean map of input states
     * @return side boolean map of output states
     */
    public SideBoolMap getOutputsForInputs(SideBoolMap inputMap) {
        int inputSet = 0;
        for (int i = 0; i < inputs.size(); i++) {
            inputSet += (1 << i) * (inputMap.get(inputs.get(i)) ? 1 : 0);
        }

        final var outputsForSet = outputsForInputSet(inputSet);

        return SideBoolMap.constructWith(
                side -> outputs.contains(side)
                        ? outputsForSet.get(outputs.indexOf(side))
                        : false
        );
    }

    public boolean hasInputOrOutput(RelDir side) {
        return hasInput(side) || hasOutput(side);
    }

    public boolean hasInput(RelDir side) {
        return inputs.contains(side);
    }

    public boolean hasOutput(RelDir side) {
        return outputs.contains(side);
    }

    /**
     * @see BoardSocket.State
     */
    public int stateForSide(RelDir side) {
        if (hasInput(side)) {
            return 1;
        } else if (hasOutput(side)) {
            return 2;
        } else {
            return 0;
        }
    }

    public ImmutableList<RelDir> getInputs() {
        return inputs;
    }

    public ImmutableList<RelDir> getOutputs() {
        return outputs;
    }

    /**
     * Serialize the truth table into an NBT tag.
     * <p>
     * It has 3 subtags: "Inputs", "Outputs" and "Mappings".
     * Inputs tag holds a byte array containing serialized {@link #inputs}.
     * Outputs tag holds a byte array containing serialized {@link #outputs}.
     * Mappings tag is a list of byte arrays containing serialized {@link #mappings}.
     *
     * @return returns an NBT tag containing the truth table
     * @see RelDir#dirListToBytes(List)
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putByteArray("Inputs", RelDir.dirListToBytes(inputs));
        tag.putByteArray("Outputs", RelDir.dirListToBytes(outputs));

        ListTag list = new ListTag();
        list.addAll(
                mappings
                        .stream()
                        .map(bitset -> new ByteArrayTag(bitset.toByteArray()))
                        .toList()
        );

        tag.put("Mappings", list);

        return tag;
    }

    /**
     * Returns the signature of the truth table.
     * <p>
     * A signature is an unique, human-readable string identifier of a truth table, which
     * ommits information about input and output sides. The signature has the following format:
     * "NUMBER_OF_INPUTS->NUMBER_OF_OUTPUTS;SORTED_LIST_OF_BITSETS_MAPPED_TO_STRINGS"
     * <p>
     * For instance:
     * - "0->0;" means: zero inputs, zero outputs, no mappings
     * - "0->1;{0}" means: zero inputs, one output, mapping of "{0}" - logical truth (it has the 0-th bit always on)
     * - "2->1;{0, 1, 2}" means: two inputs, one output, mapping of "{0, 1, 2}" - nand gate (it is true for input sets 0, 1, 2, but not 3)
     *
     * @return signature of the table
     */
    public String getSignature() {
        return inputs.size() +
                "->" +
                outputs.size() +
                ';' +
                mappings.stream()
                        .sorted(new BitSetComparator())
                        .map(BitSet::toString)
                        .collect(Collectors.joining(","));
    }

    /**
     * Detect if the table is a known table.
     *
     * @return instance of {@link KnownTable} or null if table can't be recognized
     * @see KnownTable
     */
    @Nullable
    public KnownTable recognize() {
        for (KnownTable knownCircuit : KnownTable.KNOWN_CIRCUITS) {
            if (knownCircuit.testTable(this)) {
                return knownCircuit;
            }
        }
        return null;
    }

    public String getTexture() {
        KnownTable recognized = recognize();
        return recognized != null ? recognized.getTexture() : KnownTable.DEFAULT_TEXTURE;
    }

    /**
     * Class representing the cost of creating a circuit block represented by this table.
     * <p>
     * Each circuit block costs exactly 1 terracotta.
     * The number of dust and torches corresponds to zeros and ones found inside mappings.
     */
    public static class CircuitCosts {
        public final int terracotta;
        public final int dust;
        public final int torches;

        CircuitCosts(int dust, int torches) {
            this.terracotta = 1;
            this.dust = dust;
            this.torches = torches;
        }
    }

    /**
     * @see CircuitCosts
     */
    public CircuitCosts calculateCost() {
        int dust = 0;
        int torches = 0;

        for (BitSet bitSet : mappings) {
            int enabledBits = bitSet.cardinality();

            dust += (1 << inputs.size()) - enabledBits; // number of zeros in a bitset
            torches += enabledBits; // number of ones in a bitset
        }

        return new CircuitCosts(dust, torches);
    }

    /**
     * Deserialize a truth table from an NBT tag.
     *
     * @param tag NBT tag containing a serialized truth table
     * @return the truth table
     * @see #toNBT()
     */
    public static TruthTable fromNBT(CompoundTag tag) {
        if (
                !tag.contains("Inputs", CompoundTag.TAG_BYTE_ARRAY) ||
                        !tag.contains("Outputs", CompoundTag.TAG_BYTE_ARRAY) ||
                        !tag.contains("Mappings", CompoundTag.TAG_LIST)
        ) {
            return TruthTable.empty();
        }

        final var inputs = RelDir.bytesToDirList(tag.getByteArray("Inputs"));
        final var outputs = RelDir.bytesToDirList(tag.getByteArray("Outputs"));

        ArrayList<BitSet> mappings = new ArrayList<>();
        ListTag list = tag.getList("Mappings", CompoundTag.TAG_BYTE_ARRAY);
        for (Tag elem : list) {
            if (elem instanceof ByteArrayTag) {
                mappings.add(BitSet.valueOf(((ByteArrayTag) elem).getAsByteArray()));
            } else {
                return TruthTable.empty();
            }
        }

        return new TruthTable(inputs, outputs, mappings);
    }
}
