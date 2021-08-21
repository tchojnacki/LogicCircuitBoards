package tchojnacki.mcpcb.logic;

import com.google.common.collect.ImmutableSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * A truth table that is recognized by the mod as one of the popular logic gates.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class KnownTable {
    public static final String DEFAULT_TEXTURE = "blank";

    /**
     * Table's internal id.
     */
    private final String id;

    /**
     * Name of the circuit block's center texture, as stored in resources/assets/mcpcb/textures/block/circuits
     */
    private final String texture;

    /**
     * Set of truth table signatures that get recognized as a given table.
     *
     * @see TruthTable#getSignature()
     */
    private final ImmutableSet<String> signatureSet;

    private static Builder builder(String name) {
        return new Builder(name);
    }

    private static class Builder {
        private final String id;
        private String texture = DEFAULT_TEXTURE;
        private final Set<String> signatureSet = new HashSet<>();

        private Builder(String id) {
            this.id = id;
        }

        private Builder texture(String texture) {
            this.texture = texture;
            return this;
        }

        private Builder signs(String... signatures) {
            signatureSet.addAll(Arrays.asList(signatures));
            return this;
        }

        private KnownTable build() {
            return new KnownTable(id, texture, signatureSet);
        }
    }

    /**
     * Private constructor, use builder instead.
     *
     * @see Builder
     */
    private KnownTable(String id, String texture, Set<String> signatureSet) {
        this.id = id;
        this.texture = texture;
        this.signatureSet = ImmutableSet.copyOf(signatureSet);
    }

    public String getId() {
        return id;
    }

    public TranslationTextComponent getTranslationKey() {
        return new TranslationTextComponent("util.mcpcb.circuit." + id);
    }

    public String getTexture() {
        return texture;
    }

    /**
     * Whether a given truth table is an instance of this known table.
     *
     * @param table tested table
     * @return if signature of {@code table} matches this known table
     */
    public boolean testTable(TruthTable table) {
        return signatureSet.contains(table.getSignature());
    }

    /**
     * A list of all known truth tables.
     */
    public final static KnownTable[] KNOWN_CIRCUITS = new KnownTable[]{
            // Empty
            builder("empty")
                    .signs("0->0;", "1->0;", "2->0;", "3->0;", "4->0;")
                    .build(),
            // Consts
            builder("const_false")
                    .texture("false")
                    .signs("0->1;{}", "0->2;{},{}", "0->3;{},{},{}", "0->4;{},{},{},{}")
                    .build(),
            builder("const_true")
                    .texture("true")
                    .signs("0->1;{0}", "0->2;{0},{0}", "0->3;{0},{0},{0}", "0->4;{0},{0},{0},{0}")
                    .build(),
            // OR
            builder("or_1").texture("buffer").signs("1->1;{1}").build(),
            builder("or_2").texture("or").signs("2->1;{1, 2, 3}").build(),
            builder("or_3").texture("or").signs("3->1;{1, 2, 3, 4, 5, 6, 7}").build(),
            // NOR
            builder("not").texture("not").signs("1->1;{0}").build(),
            builder("nor_2").texture("nor").signs("2->1;{0}").build(),
            builder("nor_3").texture("nor").signs("3->1;{0}").build(),
            // AND
            builder("and_2").texture("and").signs("2->1;{3}").build(),
            builder("and_3").texture("and").signs("3->1;{7}").build(),
            // NAND
            builder("nand_2").texture("nand").signs("2->1;{0, 1, 2}").build(),
            builder("nand_3").texture("nand").signs("3->1;{0, 1, 2, 3, 4, 5, 6}").build(),
            // XOR
            builder("xor").texture("xor").signs("2->1;{1, 2}").build(),
            builder("xnor").texture("xnor").signs("2->1;{0, 3}").build(),
            // Implication
            builder("impl")
                    .texture("impl")
                    .signs("2->1;{0, 1, 3}", "2->1;{0, 2, 3}")
                    .build(),
            builder("not_impl")
                    .texture("not_impl")
                    .signs("2->1;{1}", "2->1;{2}")
                    .build(),
            // Half adder
            builder("half_adder").texture("half_adder").signs("2->2;{1, 2},{3}").build(),
            // MUX
            builder("mux_2_to_1")
                    .texture("mux")
                    .signs(
                            "3->1;{1, 3, 6, 7}", "3->1;{2, 3, 5, 7}",
                            "3->1;{2, 5, 6, 7}", "3->1;{3, 4, 6, 7}",
                            "3->1;{1, 5, 6, 7}", "3->1;{3, 4, 5, 7}"
                    )
                    .build(),
            // AOI
            builder("aoi_2_1")
                    .texture("aoi")
                    .signs("3->1;{0, 1, 2}", "3->1;{0, 1, 4}", "3->1;{0, 2, 4}")
                    .build(),
            // Half subtractor
            builder("half_subtractor")
                    .texture("half_subtractor")
                    .signs("2->2;{1},{1, 2}", "2->2;{2},{1, 2}")
                    .build()
    };
}
