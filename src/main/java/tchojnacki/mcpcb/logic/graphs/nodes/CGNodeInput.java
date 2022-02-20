package tchojnacki.mcpcb.logic.graphs.nodes;

import net.minecraft.MethodsReturnNonnullByDefault;
import tchojnacki.mcpcb.logic.BoardSocket;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Node representing circuit input (breadboard's input blocks).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeInput extends CGNode {
    private final BoardSocket inputSocket;

    public CGNodeInput(BoardSocket inputSocket) {
        if (inputSocket.getState() != BoardSocket.State.Input) {
            throw new IllegalArgumentException("Socket is not an input.");
        }

        this.inputSocket = inputSocket;
    }

    public BoardSocket getInputSocket() {
        return inputSocket;
    }

    @Override
    public CGNode migrationCopy() {
        return new CGNodeInput(inputSocket);
    }
}
