package tchojnacki.mcpcb.logic.graphs.nodes;

import mcp.MethodsReturnNonnullByDefault;
import tchojnacki.mcpcb.logic.RelDir;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @see CGNodeCircuit
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeCircuitOutput extends CGNode {
    private final RelDir dir;

    public CGNodeCircuitOutput(RelDir dir) {
        this.dir = dir;
    }

    public RelDir getDir() {
        return dir;
    }

    public int getCircuit() {
        return getPredecessors().iterator().next();
    }

    @Override
    public CGNode migrationCopy() {
        return new CGNodeCircuitOutput(dir);
    }
}
