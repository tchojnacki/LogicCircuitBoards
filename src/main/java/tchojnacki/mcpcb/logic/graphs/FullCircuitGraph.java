package tchojnacki.mcpcb.logic.graphs;

import net.minecraft.MethodsReturnNonnullByDefault;
import tchojnacki.mcpcb.logic.graphs.nodes.CGNodeWire;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * A circuit graph before reduction, which can contain wire nodes and thus
 * is almost always trivially cyclic.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FullCircuitGraph extends CircuitGraph {
    @Override
    public int addWireNode() {
        return addNode(new CGNodeWire());
    }

    @Override
    public void connectWireTwoWay(int wire1, int wire2) throws IllegalArgumentException {
        if (!(isWire(wire1) && isWire(wire2))) {
            throw new IllegalArgumentException("Only wires can be connected both ways.");
        }

        connectFromTo(wire1, wire2);
        connectFromTo(wire2, wire1);
    }

    /**
     * Creates a reduced graph by removing all of the wire nodes.
     * The redstone components are connected with new edges if they could transfer power
     * between themselves using the removed wire nodes. Note that if two components were
     * connected by a chain of redstone wires longer than the maximum redstone wire power
     * propagation length (15 blocks) they won't be connected after the reduction.
     * <p>
     * Uses breadth-first search to check the connections between particular components.
     *
     * @return reduced graph (containing no wire nodes)
     * @see #wireOnlyDistanceBFS(int)
     */
    public ReducedCircuitGraph reduce() {
        ReducedCircuitGraph reducedGraph = new ReducedCircuitGraph();

        // Transfer all nodes different then wire to the new graph
        final var oldToNewVertexMap = new HashMap<Integer, Integer>();
        for (int oldIdx = 0; oldIdx < nodeCount(); oldIdx++) {
            if (!isWire(oldIdx)) {
                int newIdx = reducedGraph.transferNodeFrom(oldIdx, this);

                oldToNewVertexMap.put(oldIdx, newIdx);
            }
        }

        /*
        Add edges between the vertices.
        Check all combinations of nodes (all possible pairs of nodes contained in the original graph),
        such that neither element of the pair is a wire node. Add a connection between them if there
        existed a connection between them through a chain of less than or equals 15 wire nodes.
         */
        for (int source = 0; source < nodeCount(); source++) {
            if (oldToNewVertexMap.containsKey(source)) {
                /*
                Calculate distances from source node to all other non-wire nodes.
                No entry is added to the map if such connection does not exist.
                 */
                final var distances = wireOnlyDistanceBFS(source);

                for (int target : distances.keySet()) {
                    if (oldToNewVertexMap.containsKey(target)) {
                        if (distances.get(target) - 1 <= 15) { // n edges = n - 1 nodes between
                            reducedGraph.connectFromTo(oldToNewVertexMap.get(source), oldToNewVertexMap.get(target));
                        }
                    }
                }
            }
        }

        return reducedGraph;
    }

    /**
     * Calculate the smallest distance of a connection between a non-wire node and all other non-wire nodes
     * going exclusively through wire nodes. If such connection doesn't exist the resulting map won't contain
     * any distance. Includes connection to self if it exists.
     * Uses a modified version of breadth-first search.
     *
     * @param sourceIdx index from which to calculate the distances
     * @return a map, with entries containing destination node as the first element and the distance as the second one
     * @throws IllegalArgumentException if incorrect node index was passed
     * @see <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Wikipedia - Breadth-first search</a>
     */
    private TreeMap<Integer, Integer> wireOnlyDistanceBFS(int sourceIdx) throws IllegalArgumentException {
        final var visited = new HashSet<Integer>();
        final var distances = new TreeMap<Integer, Integer>();
        Queue<Integer> queue = new LinkedList<>();

        visited.add(sourceIdx);
        queue.add(sourceIdx);
        distances.put(sourceIdx, 0);

        int selfDistance = -1;

        while (!queue.isEmpty()) {
            int v = queue.poll();

            for (int nextVertex : getNode(v).getSuccessors()) {
                if (!visited.contains(nextVertex)) {
                    visited.add(nextVertex);
                    distances.put(nextVertex, distances.get(v) + 1);

                    // Can only go through wire blocks
                    if (isWire(nextVertex)) {
                        queue.add(nextVertex);
                    }
                } else if (nextVertex == sourceIdx && selfDistance == -1) { // Detect self-connections
                    // It is guaranteed to be the closest such connection because of how BFS works
                    selfDistance = distances.get(v) + 1;
                }
            }
        }

        // If connection to self exists put it in the map
        if (selfDistance != -1) {
            distances.put(sourceIdx, selfDistance);
        } else {
            distances.remove(sourceIdx);
        }

        return distances;
    }
}
