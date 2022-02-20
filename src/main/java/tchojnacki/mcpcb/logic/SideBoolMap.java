package tchojnacki.mcpcb.logic;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Map from relative direction to boolean.
 * Used for representing the state of inputs or outputs of a circuit.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SideBoolMap {
    /**
     * Internal representation of the data.
     */
    private final ImmutableMap<RelDir, Boolean> data;

    /**
     * Private constructor, use a factory method instead.
     *
     * @see #emptyMap()
     * @see #constructWith(Predicate)
     * @see #constructFromIterable(Iterable, Function)
     */
    private SideBoolMap(Map<RelDir, Boolean> map) {
        this.data = ImmutableMap.copyOf(map);
    }

    /**
     * Helper method that returns an empty map, to be filled by factory methods.
     * Note that this is not an instance of {@code SideBoolMap} (which is
     * immutable), but a modifiable {@link Map} to be edited before finalizing
     * into this class's instance.
     *
     * @see #getEmpty()
     */
    private static Map<RelDir, Boolean> emptyMap() {
        final var map = new HashMap<RelDir, Boolean>();

        for (RelDir side : RelDir.values()) {
            map.put(side, false);
        }

        return map;
    }

    /**
     * Factory method returning a map filled fully with false values.
     *
     * @return a new instance of an empty side boolean map
     */
    public static SideBoolMap getEmpty() {
        return new SideBoolMap(emptyMap());
    }

    /**
     * Construct a side boolean map using a predicate.
     *
     * @param sidePredicate a function taking a {@code RelDir} and returning {@code boolean}
     * @return side boolean map where each mapping is of the form {@code side} to {@code sidePredicate.test(side)}.
     */
    public static SideBoolMap constructWith(Predicate<RelDir> sidePredicate) {
        final var map = emptyMap();

        for (RelDir side : RelDir.values()) {
            map.put(side, sidePredicate.test(side));
        }

        return new SideBoolMap(map);
    }

    /**
     * Construct a side boolean map from any iterable.
     * Sometimes you know only about the state of some sides (where others are assumed to be false).
     * For instance you might use this to create a side boolean map from an array of circuit inputs
     * and a function mapping a single input to its on/off state.
     *
     * @param iterable an iterable of values of type T, where each gets applied to the {@code function}
     * @param function a function taking an argument of type T and returning {@code Map.Entry<RelDir, Boolean>} for each {@code iterable} element
     * @param <T>      any generic type, typically a complex object
     * @return side boolean map generated using rules above
     */
    public static <T> SideBoolMap constructFromIterable(Iterable<T> iterable, Function<T, Map.Entry<RelDir, Boolean>> function) {
        final var map = emptyMap();

        for (T item : iterable) {
            final var pair = function.apply(item);
            map.put(pair.getKey(), pair.getValue());
        }

        return new SideBoolMap(map);
    }

    /**
     * Deserialize a side boolean map from a byte.
     *
     * @param encoded byte encoding map data
     * @return a side boolean map with values taken from single byte bits
     * @see #toByte()
     */
    public static SideBoolMap fromByte(byte encoded) {
        return SideBoolMap.constructWith(dir ->
                (encoded & (1 << Arrays.asList(RelDir.values()).indexOf(dir))) != 0
        );
    }

    /**
     * Serialize a side boolean map into a byte.
     * Useful for NBT serialization.
     * The simpliest representation of a side boolean map is an ordered collection of four booleans.
     * It means that after full compression it should only use 4 bits, which fits in an 8 bit byte.
     * The booleans get encoded by setting i-th byte bit to 0 or 1, where i is the index of {@code RelDir}.
     *
     * @return a byte representing the map
     */
    public byte toByte() {
        byte encoding = 0;

        for (int i = 0; i < 4; i++) {
            if (Boolean.TRUE.equals(data.get(RelDir.values()[i]))) {
                encoding += 1 << i;
            }
        }

        return encoding;
    }

    public boolean get(RelDir side) {
        return Boolean.TRUE.equals(data.get(side));
    }

    public boolean isntEmpty() {
        return data.values().stream().anyMatch(b -> b);
    }

    // Two side bool maps with same values should be treated as equal even if they are different instances

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SideBoolMap that = (SideBoolMap) o;

        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
