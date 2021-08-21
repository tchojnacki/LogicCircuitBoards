package tchojnacki.mcpcb.logic.graphs.nodes;

import mcp.MethodsReturnNonnullByDefault;
import tchojnacki.mcpcb.logic.BoardSocket;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Node representing circuit output (breadboard's output blocks).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeOutput extends CGNode {
    private final BoardSocket outputSocket;

    public CGNodeOutput(BoardSocket outputSocket) {
        if (outputSocket.getState() != BoardSocket.State.Output) {
            throw new IllegalArgumentException("Socket is not an output.");
        }

        this.outputSocket = outputSocket;
    }

    public BoardSocket getOutputSocket() {
        return outputSocket;
    }

    @Override
    public CGNode migrationCopy() {
        return new CGNodeOutput(outputSocket);
    }
}
