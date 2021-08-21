package tchojnacki.mcpcb.logic.graphs.nodes;

import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base, abstract class representing a graph node.
 *
 * Nodes contain zero or more predecessors (all nodes which have an edge connecting them to the circuit).
 * Nodes contain zero or more successors (all nodes connected by an edge going from this circuit to them).
 *
 * When a node has multiple predecessors, the input signal is an OR of all connecting nodes' outputs.
 * Node's output is calculated depending on its subclass.
 *
 * @see tchojnacki.mcpcb.logic.graphs.CircuitGraph
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class CGNode {
    private final TreeSet<Integer> successors = new TreeSet<>();
    private final TreeSet<Integer> predecessors = new TreeSet<>();

    public void addSuccessor(int node) {
        successors.add(node);
    }

    public Set<Integer> getSuccessors() {
        return successors;
    }

    public void addPredecessor(int node) {
        predecessors.add(node);
    }

    public Set<Integer> getPredecessors() {
        return predecessors;
    }

    public abstract CGNode migrationCopy();
}
