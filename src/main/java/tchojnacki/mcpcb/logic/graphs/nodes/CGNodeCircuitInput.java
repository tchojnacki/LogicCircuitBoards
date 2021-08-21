package tchojnacki.mcpcb.logic.graphs.nodes;

import mcp.MethodsReturnNonnullByDefault;
import tchojnacki.mcpcb.logic.RelDir;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @see CGNodeCircuit
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeCircuitInput extends CGNode {
    private final RelDir dir;

    public CGNodeCircuitInput(RelDir dir) {
        this.dir = dir;
    }

    public RelDir getDir() {
        return dir;
    }

    @Override
    public CGNode migrationCopy() {
        return new CGNodeCircuitInput(dir);
    }
}
