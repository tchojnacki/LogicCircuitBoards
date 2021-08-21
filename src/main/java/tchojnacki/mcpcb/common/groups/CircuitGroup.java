package tchojnacki.mcpcb.common.groups;

import com.google.common.collect.ImmutableList;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.logic.KnownTable;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;

/**
 * Item group (creative tab) for circuits.
 * Populated manually from {@link #TAB_ITEMS}.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitGroup extends ItemGroup {
    public static final String ID = "mcpcb_circuit_tab";

    /**
     * List of circuits to include in the tab.
     */
    private static final ImmutableList<TruthTable> TAB_ITEMS = ImmutableList.of(
            // Const true
            TruthTable.fromBoolFunc(
                    Collections.emptyList(),
                    RelDir.FRONT,
                    l -> true
            ),
            // Buffer
            TruthTable.fromBoolFunc(
                    RelDir.BACK,
                    RelDir.FRONT,
                    l -> l.get(0)
            ),
            // NOT
            TruthTable.fromBoolFunc(
                    RelDir.BACK,
                    RelDir.FRONT,
                    l -> !l.get(0)
            ),
            // OR
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> l.get(0) || l.get(1)
            ),
            // AND
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> l.get(0) && l.get(1)
            ),
            // NOR
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> !(l.get(0) || l.get(1))
            ),
            // NAND
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> !(l.get(0) && l.get(1))
            ),
            // XOR
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> l.get(0) != l.get(1)
            ),
            // XNOR
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> l.get(0) == l.get(1)
            ),
            // OR (3)
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.BACK, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> l.get(0) || l.get(1) || l.get(2)
            ),
            // AND (3)
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.BACK, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> l.get(0) && l.get(1) && l.get(2)
            ),
            // NOR (3)
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.BACK, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> !(l.get(0) || l.get(1) || l.get(2))
            ),
            // NAND (3)
            TruthTable.fromBoolFunc(
                    Arrays.asList(RelDir.RIGHT, RelDir.BACK, RelDir.LEFT),
                    RelDir.FRONT,
                    l -> !(l.get(0) && l.get(1) && l.get(2))
            )
    );

    public CircuitGroup() {
        super(CircuitGroup.ID);
    }

    /**
     * Generates the icon of the tab (NOT gate).
     *
     * @return item stack used as tab icon
     */
    @Override
    public ItemStack makeIcon() {
        return ((CircuitBlock) Registration.CIRCUIT_BLOCK.get()).stackFromTable(
                // NOT
                TruthTable.fromBoolFunc(
                        RelDir.BACK,
                        RelDir.FRONT,
                        l -> !l.get(0)
                )
        );
    }

    /**
     * Fill items within the tab.
     * Add all previously specified items.
     *
     * @param itemsToAdd list, to which we add items
     * @see #TAB_ITEMS
     */
    @Override
    public void fillItemList(NonNullList<ItemStack> itemsToAdd) {
        CircuitBlock circuitBlock = (CircuitBlock) Registration.CIRCUIT_BLOCK.get();

        for (TruthTable table : TAB_ITEMS) {
            ItemStack itemStack = circuitBlock.stackFromTable(table);

            KnownTable knownTable = table.recognize();
            if (knownTable != null) {
                itemStack.setHoverName(knownTable.getTranslationKey());
            }

            itemsToAdd.add(itemStack);
        }
    }
}
