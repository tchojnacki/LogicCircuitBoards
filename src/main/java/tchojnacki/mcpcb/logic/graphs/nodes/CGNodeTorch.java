package tchojnacki.mcpcb.logic.graphs.nodes;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A node representing redstone torch.
 *
 * It's output signal is the inverse of the input signal.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeTorch extends CGNode {
    @Override
    public CGNode migrationCopy() {
        return new CGNodeTorch();
    }
}
