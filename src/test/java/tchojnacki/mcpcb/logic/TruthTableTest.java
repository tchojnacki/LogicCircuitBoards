package tchojnacki.mcpcb.logic;

import net.minecraft.nbt.CompoundNBT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TruthTableTest {
    private TruthTable tableEmpty, tableNand, tableNot, tableAdder, tableOther;

    @BeforeEach
    void setUp() {
        tableEmpty = TruthTable.empty();

        tableNand = TruthTable.fromBoolFunc(
                Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                RelDir.FRONT,
                l -> !(l.get(0) && l.get(1))
        );

        tableNot = TruthTable.fromBoolFunc(
                RelDir.BACK,
                RelDir.FRONT,
                l -> !l.get(0)
        );

        tableAdder = TruthTable.fromBoolFunc(
                Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                Arrays.asList(RelDir.FRONT, RelDir.BACK),
                Arrays.asList(
                        l -> l.get(0) != l.get(1),
                        l -> l.get(0) && l.get(1)
                )
        );

        tableOther = TruthTable.fromBoolFunc(
                Arrays.asList(RelDir.LEFT, RelDir.BACK),
                RelDir.RIGHT,
                l -> l.get(0)
        );
    }

    @Test
    void empty() {
        assertEquals(0, tableEmpty.getInputs().size());
        assertEquals(0, tableEmpty.getOutputs().size());
        assertEquals("0->0;", tableEmpty.getSignature());
        assertFalse(tableEmpty.hasInputOrOutput(RelDir.FRONT));
    }

    @Test
    void getOutputsForInputs() {
        assertEquals(SideBoolMap.constructWith(d -> d == RelDir.FRONT), tableNot.getOutputsForInputs(SideBoolMap.getEmpty()));
        assertEquals(SideBoolMap.getEmpty(), tableEmpty.getOutputsForInputs(SideBoolMap.getEmpty()));
        assertEquals(SideBoolMap.constructWith(d -> d == RelDir.FRONT), tableNand.getOutputsForInputs(SideBoolMap.constructWith(d -> d == RelDir.LEFT)));
    }

    @Test
    void hasInputOrOutput() {
        assertTrue(tableAdder.hasInputOrOutput(RelDir.FRONT));
        assertEquals(2, tableAdder.stateForSide(RelDir.FRONT));

        assertTrue(tableNot.hasInputOrOutput(RelDir.BACK));
        assertEquals(1, tableNot.stateForSide(RelDir.BACK));

        assertTrue(tableOther.hasInputOrOutput(RelDir.RIGHT));
        assertEquals(2, tableOther.stateForSide(RelDir.RIGHT));

        assertFalse(tableEmpty.hasInputOrOutput(RelDir.RIGHT));
        assertEquals(0, tableEmpty.stateForSide(RelDir.RIGHT));

        assertFalse(tableEmpty.hasInputOrOutput(RelDir.LEFT));
        assertEquals(0, tableEmpty.stateForSide(RelDir.LEFT));

        assertFalse(tableNand.hasInputOrOutput(RelDir.BACK));
        assertEquals(0, tableNand.stateForSide(RelDir.BACK));
    }

    @Test
    void getTexture() {
        assertEquals(KnownTable.DEFAULT_TEXTURE, tableOther.getTexture());
    }

    @Test
    void calculateCost() {
        TruthTable.CircuitCosts costs = tableNot.calculateCost();
        assertEquals(1, costs.terracotta);
        assertEquals(1, costs.dust);
        assertEquals(1, costs.torches);
    }

    @Test
    void hasOutput() {
        assertTrue(tableAdder.hasOutput(RelDir.FRONT));
        assertFalse(tableEmpty.hasOutput(RelDir.RIGHT));
    }

    @Test
    void getInputsOutputs() {
        assertEquals(tableNot.getInputs().size(), tableNot.getOutputs().size());
        assertEquals(Arrays.asList(RelDir.RIGHT, RelDir.LEFT), tableNand.getInputs());
        assertEquals(2, tableAdder.getOutputs().size());
    }

    @Test
    void nbtSerialization() {
        assertEquals(tableAdder.getSignature(), TruthTable.fromNBT(tableAdder.toNBT()).getSignature());
        assertEquals(tableEmpty.getSignature(), TruthTable.fromNBT(tableEmpty.toNBT()).getSignature());

        CompoundNBT tag = tableNand.toNBT();
        assertEquals(tag, TruthTable.fromNBT(tag).toNBT());

        assertEquals("0->0;", TruthTable.fromNBT(new CompoundNBT()).getSignature());
    }

    @Test
    void getSignature() {
        assertEquals("2->1;{0, 1, 2}", tableNand.getSignature());
        assertEquals("1->1;{0}", tableNot.getSignature());
        assertEquals("2->2;{1, 2},{3}", tableAdder.getSignature());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void recognize() {
        assertEquals("empty", tableEmpty.recognize().getId());
        assertEquals("blank", tableEmpty.recognize().getTexture());
        assertEquals("nand_2", tableNand.recognize().getId());
        assertEquals("not", tableNot.recognize().getId());
        assertEquals("half_adder", tableAdder.recognize().getId());
        assertNull(tableOther.recognize());
    }
}
