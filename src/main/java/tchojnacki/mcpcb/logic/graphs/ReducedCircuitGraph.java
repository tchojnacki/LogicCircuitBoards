package tchojnacki.mcpcb.logic.graphs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.SideBoolMap;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.logic.graphs.nodes.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * A reduced circuit graph which cannot contain any wire nodes and thus doesn't contain trivial
 * cycles (two wires connecting with each other). It means that such graph is acyclical if it
 * represents a combinatorial circuit and contains cycles if it doesn't (if it has some states,
 * and the output doesn't depend purely on the inputs). This graph already takes into accounts
 * things such as two components connected by a wire chain longer than 15 blocks.
 * This graph can be used to generate a truth table by tracing from the outputs back to the inputs
 * recursively. It is used in the last stage of truth table generation.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ReducedCircuitGraph extends CircuitGraph {
    // Wire nodes can't be present inside this graph

    @Override
    public int addWireNode() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void connectWireTwoWay(int wire1, int wire2) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the graph is acyclic using depth-first search.
     *
     * @return whether the graph is acyclic (doesn't contain any cycles) or not
     * @see <a href="https://en.wikipedia.org/wiki/Directed_acyclic_graph">Wikipedia - Directed acyclic graph</a>
     * @see <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia - Depth-first search</a>
     */
    public boolean isAcyclic() {
        for (int outputNode : circuitOutputs) {
            HashSet<Integer> grayNodes = new HashSet<>();
            HashSet<Integer> blackNodes = new HashSet<>();

            Stack<Integer> stack = new Stack<>();
            stack.push(outputNode);

            while (!stack.isEmpty()) {
                int current = stack.peek();

                if (grayNodes.contains(current) || blackNodes.contains(current)) {
                    grayNodes.remove(current);
                    blackNodes.add(current);
                    stack.pop();
                } else {
                    grayNodes.add(current);

                    for (int v : getNode(current).getPredecessors()) {
                        if (grayNodes.contains(v)) {
                            return false;
                        } else if (!blackNodes.contains(v)) {
                            stack.push(v);
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Calculate the power (0 or 1) propagated from a node.
     * To evaluate a circuit pass a specific {@link CGNodeCircuitOutput} node to it
     * (because circuits can have multiple outputs with different signals).
     * Recursively calls the method until you hit circuit inputs (or dead ends).
     *
     * @param nodeId   node to evaluate
     * @param inputSet an integer representing the state of inputs (each int bit represents a true/false input)
     * @return whether the node provides power to others
     * @throws IllegalArgumentException if circuit node is passed directly
     */
    private boolean evaluateNode(int nodeId, int inputSet) throws IllegalArgumentException {
        CGNode node = getNode(nodeId);
        if (node instanceof CGNodeCircuit) {
            throw new IllegalArgumentException("Can't evaluate circuit node directly.");
        } else if (node instanceof CGNodeInput) {
            // Check whether the input is enabled currently
            int inputSetMask = 1 << Arrays.asList(circuitInputs.toArray()).indexOf(nodeId);

            return (inputSetMask & inputSet) != 0;
        } else if (node instanceof CGNodeCircuitOutput outputNode) {
            CGNodeCircuit circuitNode = (CGNodeCircuit) getNode(outputNode.getCircuit());

            SideBoolMap circuitInputs = SideBoolMap.constructFromIterable(
                    circuitNode.getPredecessors(),
                    inNode -> new AbstractMap.SimpleEntry<>(
                            ((CGNodeCircuitInput) getNode(inNode)).getDir(),
                            evaluateNode(inNode, inputSet)
                    )
            );

            return circuitNode.getTruthTable().getOutputsForInputs(circuitInputs).get(outputNode.getDir());
        } else {
            Set<Integer> nodePredecessors = getNode(nodeId).getPredecessors();

            boolean orResult = !nodePredecessors.isEmpty() && nodePredecessors
                    .stream()
                    .anyMatch(p -> evaluateNode(p, inputSet)); // Any true

            if (node instanceof CGNodeOutput || node instanceof CGNodeCircuitInput) {
                return orResult;
            } else if (node instanceof CGNodeTorch) {
                return !orResult;
            } else {
                throw new IllegalStateException("Illegal node type.");
            }
        }
    }

    /**
     * Get the truth table representing the circuit.
     *
     * @return truth table for this circuit
     * @throws IllegalStateException if the graph is cyclic
     */
    public TruthTable getTruthTable() throws IllegalStateException {
        if (!isAcyclic()) {
            throw new IllegalStateException("Graph contains cycles.");
        }

        int inputCount = circuitInputs.size();
        /*
        Count of possible input state combinations.
        Every input can be either 0 or 1 (there are 2 states).
        There is n input.
        Thus the number of combinations is 2^n = 1 << n.
         */
        int inputSetCount = 1 << inputCount;
        int outputCount = circuitOutputs.size();

        ArrayList<RelDir> inputs = new ArrayList<>();
        ArrayList<RelDir> outputs = new ArrayList<>();
        ArrayList<BitSet> mappings = new ArrayList<>();

        for (int idx : circuitInputs) {
            inputs.add(RelDir.getOffset(Direction.NORTH, getIODirection(idx)));
        }

        for (int idx : circuitOutputs) {
            mappings.add(new BitSet(inputSetCount));
            outputs.add(RelDir.getOffset(Direction.NORTH, getIODirection(idx)));
        }

        /*
        Iterate over all possible combinations of inputs.
        If there is 2^n possible combinations, then they can be represented
        as numbers from 0 to 2^n - 1, where each bit of the underlying binary
        representation represents an input.
         */
        for (int inputSet = 0; inputSet < inputSetCount; inputSet++) {
            // Calculate all outputs for a given input state combination
            for (int o = 0; o < outputCount; o++) {
                int outputNodeId = (int) circuitOutputs.toArray()[o];

                mappings.get(o).set(inputSet, evaluateNode(outputNodeId, inputSet));
            }
        }

        return new TruthTable(inputs, outputs, mappings);
    }
}
