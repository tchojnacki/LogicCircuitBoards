package tchojnacki.mcpcb.logic.graphs.nodes;

import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A node representing a redstone wire.
 *
 * It can only exist in a {@link tchojnacki.mcpcb.logic.graphs.FullCircuitGraph} and can't be present in
 * {@link tchojnacki.mcpcb.logic.graphs.ReducedCircuitGraph}. During reduction all wire nodes between two other
 * nodes (A and B) are replaced by a single connection from A to B (or removed if the distance is greater than
 * 15 (the maximum distance a redstone wire can conduct power through)).
 *
 * Those are the only nodes which can connect in two ways. That means, that after reducing, the graph has only
 * one-way edges connecting the nodes. And doesn't contain trivial cycles (A connects to B, which connects to A).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CGNodeWire extends CGNode {
    @Override
    public CGNode migrationCopy() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Reduced graph can't contain wires.");
    }
}
