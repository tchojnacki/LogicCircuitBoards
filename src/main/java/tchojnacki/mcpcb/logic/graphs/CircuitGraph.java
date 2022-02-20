package tchojnacki.mcpcb.logic.graphs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import tchojnacki.mcpcb.logic.BoardSocket;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.logic.graphs.nodes.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * A directed (potentially cyclic) graph representing the logical structure of the built circuit.
 * Used to represent the placed redstone components in a more abstract way, so that it is easier
 * to analyze them (and synthetize a truth table).
 * <p>
 * The graph is represented using adjacency lists, stored in {@link CGNode}. The nodes also contain
 * information about their predecessors to make some algorithms faster and simplier. We don't need
 * to concern ourselves about the space efficiency, because the {@code CircuitGraph} objects are
 * mostly short-lived and get quickly turned into their truth table counterpart. The nodes (graph vertices)
 * are almost always referred to by their index in the {@link #nodeList} to simplify some design choices.
 * A directed connection between two components means that power can be propagated that way between them.
 * <p>
 * There are several benefits to dealing with circuits this way. An obvious alternative would be to store
 * a "mini-world" for each circuit, just power according blocks and see which outputs get powered. That way
 * it wouldn't need any emulation and would support every circuit (note that this way supports only acyclic
 * circuits containing only redstone wires, torches and other circuit blocks). However that solution has
 * following cons:
 * - it is much less size-efficient to store the entire section of the world instead of a simple truth table
 * representation (which is basically a couple of ints)
 * - it is much faster to calculate circuit's output based on inputs, as we just read the answer from the truth
 * table instead of running the entire simulation
 * - circuit block with different implementation of the exact same logic gates wouldn't be able to be stacked
 *
 * @see CGNode
 * @see <a href="https://en.wikipedia.org/wiki/Directed_graph">Wikipedia - Directed graph</a>
 * @see <a href="https://en.wikipedia.org/wiki/Adjacency_list">Wikipedia - Adjacency list</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class CircuitGraph {
    /*
    TODO: It might be possible to store non-combinatorial logic circuits using this system. We would have
        to store current circuit's state (basically whether its flip-flops output 0 or 1) and make the output
        truth table depend on latch outputs apart from circuit inputs.
        It is definitely worth checking if that would allow for a logical and manageable way to add support
        for non-combinatorial circuits.
     */

    /**
     * List of the graph nodes. Nodes are commonly referred to by their index in this list.
     */
    private final ArrayList<CGNode> nodeList = new ArrayList<>();

    protected final TreeSet<Integer> circuitInputs = new TreeSet<>();
    protected final TreeSet<Integer> circuitOutputs = new TreeSet<>();

    /**
     * Check if the index is correct for a given graph.
     *
     * @param index node's index to check
     * @throws IllegalArgumentException if the node doesn't exist in this graph
     */
    private void verifyIndex(int index) throws IllegalArgumentException {
        if (index < 0 || index >= nodeList.size()) {
            throw new IllegalArgumentException("Node index out of range.");
        }
    }

    protected CGNode getNode(int index) throws IllegalArgumentException {
        verifyIndex(index);
        return nodeList.get(index);
    }

    public int nodeCount() {
        return nodeList.size();
    }

    protected int addNode(CGNode node) {
        nodeList.add(node);
        return nodeList.indexOf(node);
    }

    public int addInputNode(BoardSocket inputSocket) throws IllegalArgumentException {
        circuitInputs.add(nodeList.size());
        return addNode(new CGNodeInput(inputSocket));
    }

    public int addOutputNode(BoardSocket outputSocket) throws IllegalArgumentException {
        circuitOutputs.add(nodeList.size());
        return addNode(new CGNodeOutput(outputSocket));
    }

    /**
     * Adding wire nodes is only possible for full graphs, as reduced graphs can't contain them.
     *
     * @return added wire node's index
     * @throws UnsupportedOperationException if you are trying to add a wire node to a reduced graph
     * @see ReducedCircuitGraph
     */
    public abstract int addWireNode() throws UnsupportedOperationException;

    public int addTorchNode() {
        return addNode(new CGNodeTorch());
    }

    /**
     * Adds a circuit block node to the graph as well as its input and output nodes.
     *
     * @param truthTable truth table representing the nested circuit
     * @return index of the circuit node
     * @see CGNodeCircuit
     */
    public int addCircuitNode(TruthTable truthTable) {
        CGNodeCircuit circuitNode = new CGNodeCircuit(truthTable);
        int circuitIdx = addNode(circuitNode);

        TruthTable table = circuitNode.getTruthTable();
        for (RelDir inputDir : table.getInputs()) {
            int inputIdx = addNode(new CGNodeCircuitInput(inputDir));
            connectFromTo(inputIdx, circuitIdx);
        }
        for (RelDir outputDir : table.getOutputs()) {
            int outputIdx = addNode(new CGNodeCircuitOutput(outputDir));
            connectFromTo(circuitIdx, outputIdx);
        }

        return circuitIdx;
    }

    /**
     * Get index of {@link CGNodeCircuitOutput} for a given index of {@link CGNodeCircuit}.
     *
     * @param nodeIdx index of a circuit node
     * @param relDir  direction of the output relatively to the circuit
     * @return index of the found output
     * @throws IllegalArgumentException if no such output exists
     */
    public int getCircuitSideOutput(int nodeIdx, RelDir relDir) throws IllegalArgumentException {
        if (!isCircuit(nodeIdx)) {
            throw new IllegalArgumentException("Given node must be a circuit.");
        }

        for (int outputIdx : getNode(nodeIdx).getSuccessors()) {
            if (((CGNodeCircuitOutput) getNode(outputIdx)).getDir().equals(relDir)) {
                return outputIdx;
            }
        }

        throw new IllegalArgumentException("No outputs found on given circuit side.");
    }

    /**
     * Wire is always connected in two ways (because it can propagate power in both ways).
     * It cannot be used for reduced circuit graphs as they can't contain wires.
     *
     * @param wire1 index of the first wire (order doesn't matter)
     * @param wire2 index of the second wire (order doesn't matter)
     * @throws UnsupportedOperationException if the graph is reduced
     * @throws IllegalArgumentException      if any of the specified nodes is not a wire
     */
    public abstract void connectWireTwoWay(int wire1, int wire2) throws UnsupportedOperationException, IllegalArgumentException;

    /**
     * Connect two components in a directed manner (directed from {@code fromIndex} to {@code toIndex}).
     * Makes sure you can't wrongly connect circuit, circuit input and circuit output nodes.
     *
     * @param fromIndex index of the starting node
     * @param toIndex   index of the finish node
     * @throws IllegalArgumentException if you are incorrectly connecting circuit-related nodes
     */
    public void connectFromTo(int fromIndex, int toIndex) throws IllegalArgumentException {
        if (isCircuit(fromIndex) && !(getNode(toIndex) instanceof CGNodeCircuitOutput)) {
            throw new IllegalArgumentException("Circuit can only be connected to circuit output.");
        }

        if (!isCircuit(fromIndex) && getNode(toIndex) instanceof CGNodeCircuitOutput) {
            throw new IllegalArgumentException("Circuit outputs can only connect from circuits.");
        }

        if (isCircuit(toIndex) && !(getNode(fromIndex) instanceof CGNodeCircuitInput)) {
            throw new IllegalArgumentException("Circuit can only be connected from circuit input.");
        }

        if (!isCircuit(toIndex) && getNode(fromIndex) instanceof CGNodeCircuitInput) {
            throw new IllegalArgumentException("Circuit inputs can only connect to circuits.");
        }

        getNode(fromIndex).addSuccessor(toIndex);
        getNode(toIndex).addPredecessor(fromIndex);
    }

    public boolean isWire(int index) throws IllegalArgumentException {
        return getNode(index) instanceof CGNodeWire;
    }

    public boolean isCircuit(int index) throws IllegalArgumentException {
        return getNode(index) instanceof CGNodeCircuit;
    }

    /**
     * Returns the direction a given {@link CGNodeCircuitInput} or {@link CGNodeCircuitOutput} is facing.
     *
     * @param index node's index
     * @return direction the specified node if facing
     * @throws IllegalArgumentException if the node is neither a circuit input nor output node
     */
    public Direction getIODirection(int index) throws IllegalArgumentException {
        CGNode node = getNode(index);

        return switch (node) {
            case CGNodeInput nodeInput -> nodeInput.getInputSocket().getDirection();
            case CGNodeOutput nodeOutput -> nodeOutput.getOutputSocket().getDirection();
            default -> throw new IllegalArgumentException("Illegal node type.");
        };
    }

    /**
     * Used to move nodes from one graph to another.
     * The second graph must be a {@link ReducedCircuitGraph}.
     *
     * @param originalIndex index that the node had before moving (in the old graph)
     * @param originalGraph the graph we are moving the node from
     * @return index of node's position in the new graph
     * @throws IllegalArgumentException if we are transferring node to a graph that isn't reduced
     */
    protected int transferNodeFrom(int originalIndex, FullCircuitGraph originalGraph) throws IllegalArgumentException {
        if (!(this instanceof ReducedCircuitGraph)) {
            throw new IllegalArgumentException("Can only transfer node when reducing graph.");
        }

        int newIndex = addNode(originalGraph.getNode(originalIndex).migrationCopy());

        if (originalGraph.circuitInputs.contains(originalIndex)) {
            circuitInputs.add(newIndex);
        } else if (originalGraph.circuitOutputs.contains(originalIndex)) {
            circuitOutputs.add(newIndex);
        }

        return newIndex;
    }
}
