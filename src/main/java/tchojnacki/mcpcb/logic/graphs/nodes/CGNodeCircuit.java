package tchojnacki.mcpcb.logic.graphs.nodes;

import mcp.MethodsReturnNonnullByDefault;
import tchojnacki.mcpcb.logic.TruthTable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Graph node representing a circuit.
 * Each analyzed circuit consists of a single {@code CGNodeCircuit} and multiple
 * {@link CGNodeCircuitInput} and {@link CGNodeCircuitOutput} (one for each input/output).
 * <p>
 * A circuit node can be only connected to other nodes in one of two following ways:
 * - one way connection from input node to circuit node
 * - one way connection from circuit node to output node
 * <p>
 * Each input node can have only one successor (the circuit).
 * Each ouput node can have only one predecessor (the circuit).
 * <p>
 * The input and output nodes are then connected to the rest of the circuit.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeCircuit extends CGNode {
    private final TruthTable truthTable;

    public CGNodeCircuit(TruthTable truthTable) {
        this.truthTable = truthTable;
    }

    public TruthTable getTruthTable() {
        return truthTable;
    }

    @Override
    public CGNode migrationCopy() {
        return new CGNodeCircuit(truthTable);
    }
}
