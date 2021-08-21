package tchojnacki.mcpcb.logic.graphs;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import tchojnacki.mcpcb.logic.BoardSocket;
import tchojnacki.mcpcb.logic.KnownTable;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.TruthTable;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CircuitGraphTest {
    private final ArrayList<BlockPos> mockBlocks = new ArrayList<>();

    @Test
    void buildingAndReducingAcyclic() {
        FullCircuitGraph graph = new FullCircuitGraph();

        int inputEast = graph.addInputNode(new BoardSocket(Direction.EAST, mockBlocks, BoardSocket.State.Input));
        int inputWest = graph.addInputNode(new BoardSocket(Direction.WEST, mockBlocks, BoardSocket.State.Input));

        assertThrows(IllegalArgumentException.class, () -> graph.addInputNode(new BoardSocket(Direction.EAST, mockBlocks, BoardSocket.State.Empty)));

        assertThrows(IllegalArgumentException.class, () -> graph.getNode(-1));
        assertThrows(IllegalArgumentException.class, () -> graph.getNode(graph.nodeCount()));

        ArrayList<Integer> wires = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int newWire = graph.addWireNode();
            assertTrue(graph.isWire(newWire));
            wires.add(newWire);
        }

        graph.connectFromTo(inputEast, wires.get(0));
        graph.connectFromTo(inputWest, wires.get(1));
        graph.connectWireTwoWay(wires.get(0), wires.get(2));
        graph.connectWireTwoWay(wires.get(0), wires.get(1));

        assertThrows(IllegalArgumentException.class, () -> graph.connectWireTwoWay(inputEast, wires.get(3)));
        assertThrows(IllegalArgumentException.class, () -> graph.connectWireTwoWay(inputEast, inputWest));

        assertThrows(IllegalArgumentException.class, () -> graph.getIODirection(wires.get(0)));

        int torch = graph.addTorchNode();

        graph.connectFromTo(wires.get(2), torch);
        graph.connectFromTo(torch, wires.get(3));
        graph.connectWireTwoWay(wires.get(3), wires.get(4));

        int output = graph.addOutputNode(new BoardSocket(Direction.SOUTH, mockBlocks, BoardSocket.State.Output));

        graph.connectFromTo(wires.get(4), output);

        assertThrows(IllegalArgumentException.class, () -> graph.addOutputNode(new BoardSocket(Direction.WEST, mockBlocks, BoardSocket.State.Input)));

        ReducedCircuitGraph reducedGraph = graph.reduce();
        assertTrue(reducedGraph.isAcyclic());

        assertThrows(UnsupportedOperationException.class, () -> reducedGraph.transferNodeFrom(wires.get(0), graph));
        assertThrows(IllegalArgumentException.class, () -> graph.transferNodeFrom(inputEast, graph));

        TruthTable table = reducedGraph.getTruthTable();

        assertEquals("2->1;{0}", table.getSignature());

        KnownTable recognized = table.recognize();
        assertNotNull(recognized);
        assertEquals("util.mcpcb.circuit.nor_2", recognized.getTranslationKey().getKey());
    }

    @Test
    void buildingAndReducingCyclic() {
        FullCircuitGraph graph = new FullCircuitGraph();

        int output = graph.addOutputNode(new BoardSocket(Direction.NORTH, mockBlocks, BoardSocket.State.Output));
        int lowerWire = graph.addWireNode();
        int upperWire = graph.addWireNode();
        int torch = graph.addTorchNode();

        graph.connectWireTwoWay(lowerWire, upperWire);
        graph.connectFromTo(lowerWire, output);
        graph.connectFromTo(lowerWire, torch);
        graph.connectFromTo(upperWire, torch);
        graph.connectFromTo(torch, upperWire);

        ReducedCircuitGraph reduced = graph.reduce();

        assertThrows(UnsupportedOperationException.class, reduced::addWireNode);
        assertThrows(UnsupportedOperationException.class, () -> reduced.connectWireTwoWay(0, 1));

        assertFalse(reduced.isAcyclic());
        assertThrows(IllegalStateException.class, reduced::getTruthTable);
    }

    @Test
    void graphWithNestedCircuit() {
        FullCircuitGraph graph = new FullCircuitGraph();

        int inputEast = graph.addInputNode(new BoardSocket(Direction.EAST, mockBlocks, BoardSocket.State.Input));
        int inputWest = graph.addInputNode(new BoardSocket(Direction.WEST, mockBlocks, BoardSocket.State.Input));

        int output = graph.addOutputNode(new BoardSocket(Direction.NORTH, mockBlocks, BoardSocket.State.Output));

        int circuit = graph.addCircuitNode(TruthTable.fromBoolFunc(
                Arrays.asList(RelDir.RIGHT, RelDir.LEFT),
                RelDir.FRONT,
                l -> l.get(0) && l.get(1)
        ));

        ArrayList<Integer> pre = new ArrayList<>(graph.getNode(circuit).getPredecessors());
        assertEquals(2, pre.size());

        ArrayList<Integer> succ = new ArrayList<>(graph.getNode(circuit).getSuccessors());
        assertEquals(1, succ.size());

        assertDoesNotThrow(() -> {
            graph.connectFromTo(inputEast, pre.get(0));
            graph.connectFromTo(inputWest, pre.get(1));

            graph.connectFromTo(succ.get(0), output);
        });

        assertEquals(succ.get(0), graph.getCircuitSideOutput(circuit, RelDir.FRONT));

        ReducedCircuitGraph reduced = graph.reduce();
        assertTrue(reduced.isAcyclic());

        KnownTable recognized = reduced.getTruthTable().recognize();
        assertNotNull(recognized);
        assertEquals("util.mcpcb.circuit.and_2", recognized.getTranslationKey().getKey());
    }
}
