package tchojnacki.mcpcb.logic.graphs;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWallTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.common.tileentities.CircuitBlockTileEntity;
import tchojnacki.mcpcb.logic.BoardManager;
import tchojnacki.mcpcb.logic.BoardSocket;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.graphs.nodes.CGNodeCircuitInput;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;

import static net.minecraft.block.HorizontalBlock.FACING;

/**
 * Circuit graph builder.
 * Used to convert Minecraft world's block layout (the blocks on top of a breadboard) into
 * a {@link FullCircuitGraph}. The circuit then gets reduced to a {@link ReducedCircuitGraph}
 * and turned into a truth table stored in the circuit block.
 * Traces redstone connections consisting of wires, torches and circuit blocks going back from outputs to inputs.
 *
 * @see CircuitGraph
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CGBuilder {
    private final World world;

    private final BoardManager boardManager;

    private final FullCircuitGraph graph = new FullCircuitGraph();

    /**
     * Stores blocks that were already visited to avoid infinite loops when traversing a circuit
     * containing multiple paths between components. The value of a map entry represents a vertex
     * index in the {@link #graph}.
     */
    private final HashMap<BlockPos, Integer> visitedBlocks = new HashMap<>();

    /**
     * Map of input nodes based on their direction.
     * Entires map board socket sides to input node vertex indices.
     *
     * @see tchojnacki.mcpcb.logic.graphs.nodes.CGNodeInput
     */
    private final HashMap<Direction, Integer> inputNodes = new HashMap<>();

    /**
     * Factory static method creating a circuit graph based on Minecraft world and a board manager.
     *
     * @param world        world which we are analyzing
     * @param boardManager board manager storing breadboard location and other info
     * @return full circuit graph representing the built circuit
     */
    public static FullCircuitGraph create(World world, BoardManager boardManager) {
        CGBuilder graphBuilder = new CGBuilder(world, boardManager);
        graphBuilder.prepareInputs();
        graphBuilder.buildFromOutputs();
        return graphBuilder.graph;
    }

    /**
     * Private constructor, use factory method for building circuit graphs instead.
     *
     * @see #create(World, BoardManager)
     */
    private CGBuilder(World world, BoardManager boardManager) {
        this.world = world;
        this.boardManager = boardManager;
    }

    private boolean doesConduct(BlockPos blockPos) {
        return world.getBlockState(blockPos).isRedstoneConductor(world, blockPos);
    }

    /**
     * Populate {@link #inputNodes} before analyzing other blocks.
     */
    private void prepareInputs() {
        for (BoardSocket socket : boardManager.getInputs()) {
            inputNodes.put(socket.getDirection(), graph.addInputNode(socket));
        }
    }

    /**
     * Goes over all circuit outputs and starts tracing connections back to inputs.
     */
    private void buildFromOutputs() {
        for (BoardSocket socket : boardManager.getOutputs()) {
            int outputNode = graph.addOutputNode(socket);

            for (BlockPos socketBlock : socket.getBlocks()) {
                BlockPos curOutputBlock = socketBlock.above();

                if (world.getBlockState(curOutputBlock).getBlock() instanceof RedstoneWireBlock) {
                    traceWire(outputNode, curOutputBlock);
                }
            }
        }
    }

    /**
     * Traces single redstone wire, checking for its connections to other redstone components.
     *
     * @param traceSource index of the graph node, from which this method was called
     * @param tracePos    position of the wire
     */
    private void traceWire(int traceSource, BlockPos tracePos) {
        // Ignore blocks outside of the breadboard
        if (boardManager.outsideOfBoardArea(tracePos)) {
            return;
        }

        int wireNode = visitedBlocks.containsKey(tracePos)
                ? visitedBlocks.get(tracePos)
                : graph.addWireNode();

        // Redstone wires are the only component propagating power both ways
        if (graph.isWire(traceSource)) {
            graph.connectWireTwoWay(wireNode, traceSource);
        } else {
            graph.connectFromTo(wireNode, traceSource);
        }

        // Ignore already visisted blocks to avoid infinite loops
        if (visitedBlocks.containsKey(tracePos)) {
            return;
        } else {
            visitedBlocks.put(tracePos, wireNode);
        }

        // Check if the wire is placed on top of a breadboard input block
        Integer inputNode = getInputNode(tracePos);
        if (inputNode != null) {
            graph.connectFromTo(inputNode, wireNode);
        }

        if (doesConduct(tracePos.below())) {
            traceAllPoweringABlock(wireNode, tracePos.below(), true, tracePos);
        }

        // Trace other connections

        if (doesConduct(tracePos.above())) {
            traceAllPoweringABlock(wireNode, tracePos.above(), true, tracePos);
        } else {
            if (world.getBlockState(tracePos.above()).getBlock() instanceof RedstoneTorchBlock) {
                traceTorch(wireNode, tracePos.above());
            }

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos offsetPos = tracePos.above().relative(dir);

                if (world.getBlockState(offsetPos).getBlock() instanceof RedstoneWireBlock) {
                    traceWire(wireNode, offsetPos);
                }
            }
        }


        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos offsetPos = tracePos.relative(dir);
            if (doesConduct(offsetPos)) {
                traceAllPoweringABlock(wireNode, offsetPos, true, tracePos);
            } else {
                traceInwards(wireNode, offsetPos, dir);

                if (world.getBlockState(offsetPos.below()).getBlock() instanceof RedstoneWireBlock) {
                    traceWire(wireNode, offsetPos.below());
                }
            }
        }
    }

    /**
     * Traces a redstone torch. Note that this method gets called from a component, that
     * can be powered by this torch, and should in turn trace components affecting this
     * torch's state.
     *
     * @param traceSource index of the graph node, from which this method was called
     * @param tracePos    position of the torch
     */
    private void traceTorch(int traceSource, BlockPos tracePos) {
        // Ignore blocks outside of the breadboard
        if (boardManager.outsideOfBoardArea(tracePos)) {
            return;
        }

        int torchNode = visitedBlocks.containsKey(tracePos)
                ? visitedBlocks.get(tracePos)
                : graph.addTorchNode();

        graph.connectFromTo(torchNode, traceSource);

        // Ignore already visisted blocks to avoid infinite loops
        if (visitedBlocks.containsKey(tracePos)) {
            return;
        } else {
            visitedBlocks.put(tracePos, torchNode);
        }

        // Get the block the torch is placed on
        BlockPos activatorBlock = tracePos.below();
        if (world.getBlockState(tracePos).getBlock() instanceof RedstoneWallTorchBlock) {
            activatorBlock = tracePos.relative(world.getBlockState(tracePos).getValue(RedstoneWallTorchBlock.FACING).getOpposite());
        }

        if (doesConduct(activatorBlock)) {
            traceAllPoweringABlock(torchNode, activatorBlock, false, tracePos);
        }
    }

    /**
     * Traces nested circuit BLOCK outputs. All circuit blocks have two methods - one for tracing its outputs
     * and one for tracing inputs. Tracing of the circuit starts here (and can happen multiple times if
     * the circuit has multiple outputs). This method adds all of the nodes associated with circuits
     * (input, output nodes and the circuit node itself).
     *
     * @param traceSource   index of the graph node, from which this method was called
     * @param tracePos      position of the circuit block
     * @param circuitEntity tile entity of the circuit block
     * @param sourceDir     direction which we are checking
     * @see tchojnacki.mcpcb.logic.graphs.nodes.CGNodeCircuit
     * @see #traceCircuitInput(int, BlockPos, BlockPos, Direction)
     * @see #traceIfHasCircuitFacing(int, BlockPos, Direction)
     */
    private void traceCircuitOutput(int traceSource, BlockPos tracePos, CircuitBlockTileEntity circuitEntity, RelDir sourceDir) {
        // Ignore blocks outside of the breadboard
        if (boardManager.outsideOfBoardArea(tracePos)) {
            return;
        }

        int circuitNode = visitedBlocks.containsKey(tracePos)
                ? visitedBlocks.get(tracePos)
                : graph.addCircuitNode(circuitEntity.getTruthTable());

        graph.connectFromTo(graph.getCircuitSideOutput(circuitNode, sourceDir), traceSource);

        // Ignore already visisted blocks to avoid infinite loops
        if (visitedBlocks.containsKey(tracePos)) {
            return;
        } else {
            visitedBlocks.put(tracePos, circuitNode);
        }

        for (int inputIdx : graph.getNode(circuitNode).getPredecessors()) {
            CGNodeCircuitInput inputNode = (CGNodeCircuitInput) graph.getNode(inputIdx);
            Direction inputSide = inputNode.getDir().offsetFrom(world.getBlockState(tracePos).getValue(FACING));

            /*
            Trace all inputs even if they are not associated with the output,
            the truth table generation will cover that case too.
             */
            traceCircuitInput(inputIdx, tracePos.relative(inputSide), tracePos, inputSide);
        }
    }

    /**
     * Traces nested circuit BLOCK inputs.
     *
     * @param inputNode  index of traced node
     * @param inputPos   position of the input - this is offset from the circuit block's direction by one block
     * @param circuitPos circuit block's position
     * @param inputSide  side the input is on
     * @see #traceCircuitOutput(int, BlockPos, CircuitBlockTileEntity, RelDir)
     */
    private void traceCircuitInput(int inputNode, BlockPos inputPos, BlockPos circuitPos, Direction inputSide) {
        // Ignore blocks outside of the breadboard
        if (boardManager.outsideOfBoardArea(inputPos)) {
            return;
        }

        if (doesConduct(inputPos)) {
            traceAllPoweringABlock(inputNode, inputPos, false, circuitPos);
        } else {
            traceInwards(inputNode, inputPos, inputSide);
        }
    }

    /**
     * Helper function to trace all redstone components powering a block (strongly or not
     * depending on {@code mustBeStrong}). It makes it easier to trace things like redstone
     * powered by a strong source through a block or redstone torches affected by the block
     * they are placed on.
     *
     * @param traceSource  index of the graph node, from which this method was called
     * @param tracePos     position of the circuit block
     * @param mustBeStrong whether the power source must provide strong power (sometimes weak is enough)
     * @param exceptFor    excludes a position from search to avoid looping back to itself (wire powering a block isn't powered by that block)
     * @throws IllegalArgumentException if passsed block position is not a full block
     */
    private void traceAllPoweringABlock(int traceSource, BlockPos tracePos, boolean mustBeStrong, BlockPos exceptFor) throws IllegalArgumentException {
        // Ignore blocks outside of the breadboard
        if (boardManager.outsideOfBoardArea(tracePos)) {
            return;
        }

        if (!doesConduct(tracePos)) {
            throw new IllegalArgumentException("Traced position is not a full block.");
        }

        for (Direction dir : Direction.values()) {
            BlockPos offsetPos = tracePos.relative(dir);
            Block block = world.getBlockState(offsetPos).getBlock();

            // Ignore block passed in exceptFor
            if (offsetPos.equals(exceptFor)) {
                continue;
            }

            if (dir.equals(Direction.DOWN)) {
                if (block instanceof RedstoneTorchBlock) {
                    traceTorch(traceSource, offsetPos);
                }
            } else {
                if (!mustBeStrong && block instanceof RedstoneWireBlock) {
                    if (dir.equals(Direction.UP) || world.getBlockState(offsetPos).getValue(RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(dir.getOpposite())).isConnected()) {
                        traceWire(traceSource, offsetPos);
                    }
                }

                if (tracePos.getY() == offsetPos.getY() && block instanceof CircuitBlock) {
                    traceIfHasCircuitFacing(traceSource, offsetPos, dir.getOpposite());
                }
            }
        }
    }

    /**
     * Traces all redstone components located at {@code offsetPos} placed at {@code offsetDir}
     * relatively to the original block that might power it.
     * <p>
     * Note that {@code offsetPos.equals(originalBlock.relative(offsetDir))}, which means that
     * the block placed at {@code offsetPos} must conduct power in the {@code offsetDir.getOpposite()}
     * direction.
     *
     * @param traceSource index of the graph node, from which this method was called
     * @param offsetPos   position to trace at
     * @param offsetDir   direction from original block to {@code offsetPos}
     */
    private void traceInwards(int traceSource, BlockPos offsetPos, Direction offsetDir) {
        if (boardManager.outsideOfBoardArea(offsetPos)) {
            return;
        }

        Block block = world.getBlockState(offsetPos).getBlock();

        if (block instanceof RedstoneWireBlock) {
            traceWire(traceSource, offsetPos);
        } else if (block instanceof RedstoneTorchBlock) {
            traceTorch(traceSource, offsetPos);
        } else if (block instanceof CircuitBlock) {
            traceIfHasCircuitFacing(traceSource, offsetPos, offsetDir.getOpposite());
        }
    }

    /**
     * Traces a circuit block (output) if there is a circuit located at {@code tracePos}
     * which has output on the {@code facingDir} side.
     *
     * @param traceSource index of the graph node, from which this method was called
     * @param tracePos    position of the circuit block
     * @param facingDir   the side which the circuit has to has an output on
     * @throws IllegalArgumentException if the block at {@code tracePos} is not a circuit
     */
    private void traceIfHasCircuitFacing(int traceSource, BlockPos tracePos, Direction facingDir) throws IllegalArgumentException {
        if (boardManager.outsideOfBoardArea(tracePos)) {
            return;
        }

        if (!(world.getBlockState(tracePos).getBlock() instanceof CircuitBlock)) {
            throw new IllegalArgumentException("Block is not a circuit.");
        }

        TileEntity tileEntity = world.getBlockEntity(tracePos);

        if (tileEntity instanceof CircuitBlockTileEntity) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;

            RelDir relDir = RelDir.getOffset(world.getBlockState(tracePos).getValue(FACING), facingDir);

            if (circuitEntity.hasOutputOnSide(relDir)) {
                traceCircuitOutput(traceSource, tracePos, circuitEntity, relDir);
            }
        }
    }

    /**
     * Returns output node associated with breadboard block below {@code position}.
     *
     * @param position position of the block above the input breadboard block
     * @return index of found node or null if no input exists at that place
     */
    @Nullable
    private Integer getInputNode(BlockPos position) {
        /*
        TODO: This gets run for every traced wire ensuring every input connection is found,
            it would be faster to discard some obviously illegal positions before iterating
            over all inputs (for instance based on position's coordinates).
         */
        for (BoardSocket socket : boardManager.getInputs()) {
            if (socket.containsBlock(position.below())) {
                return inputNodes.get(socket.getDirection());
            }
        }

        return null;
    }
}
