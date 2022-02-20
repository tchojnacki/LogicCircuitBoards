package tchojnacki.mcpcb.logic;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tchojnacki.mcpcb.common.block.BreadboardBlock;
import tchojnacki.mcpcb.logic.graphs.CGBuilder;
import tchojnacki.mcpcb.logic.graphs.ReducedCircuitGraph;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper class for managing a breadboard (a collection of 8 by 8 breadboard blocks).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BoardManager {
    public final static int BOARD_SIZE = 8;

    private final Level level;
    private final BlockPos nwCorner;
    private final ImmutableMap<Direction, BoardSocket> sockets;

    /**
     * Change the state (input/output/neither) of a side of the breadboard.
     *
     * @param direction side to change
     * @param state     new state to set (0, 1 or 2)
     * @throws IllegalArgumentException if the direction or state is incorrect
     */
    public void setSideState(Direction direction, BoardSocket.State state) throws IllegalArgumentException {
        if (!sockets.containsKey(direction)) {
            throw new IllegalArgumentException("Illegal direction.");
        }

        Objects.requireNonNull(sockets.get(direction)).setState(state, level);
    }

    public BoardSocket getSocket(Direction direction) {
        return Objects.requireNonNull(sockets.get(direction));
    }

    /**
     * Get all sockets with a given state.
     *
     * @param state enum value of the required state
     * @return list of sockets with {@code state}
     */
    private ArrayList<BoardSocket> getSocketsWithState(BoardSocket.State state) {
        return sockets
                .values()
                .stream()
                .filter(socket -> socket.getState() == state)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<BoardSocket> getInputs() {
        return getSocketsWithState(BoardSocket.State.Input);
    }

    public ArrayList<BoardSocket> getOutputs() {
        return getSocketsWithState(BoardSocket.State.Output);
    }

    /**
     * Create a truth table from the breadboard.
     *
     * @param level breadboard's level
     * @return truth table representing the circuit
     * @see CGBuilder
     */
    @Nullable
    public TruthTable generateTruthTable(Level level) {
        ReducedCircuitGraph reducedGraph = CGBuilder
                .create(level, this)
                .reduce();

        return reducedGraph.isAcyclic() ? reducedGraph.getTruthTable() : null;
    }

    /**
     * Checks wheter a block position is outside of the board area or not.
     * Board area is defined as all of the breadboard blocks that the breadboard
     * consists of as well as all of the blocks placed above (any bigger Y coordinate) them.
     *
     * @param pos block position to check
     * @return if {@code pos} is contained in board area
     */
    public boolean outsideOfBoardArea(BlockPos pos) {
        return pos.getY() <= nwCorner.getY() ||
                (nwCorner.getX() > pos.getX() || pos.getX() >= nwCorner.getX() + 8) ||
                (nwCorner.getZ() > pos.getZ() || pos.getZ() >= nwCorner.getZ() + 8);
    }

    /**
     * Creates a breadboard manager.
     *
     * Throws a {@link BoardManagerException} if:
     * - passed {@code blockPos} is not a breadboard block
     * - the 8 by 8 area has some breadboard blocks adjacent to it
     * - the area has wrong dimensions
     * - block states of the breadboard blocks are in any way malformed
     *
     * @param level breadboard's world
     * @param blockPos any of the blocks contained in the breadboard
     * @throws BoardManagerException see method desc
     */
    public BoardManager(Level level, BlockPos blockPos) throws BoardManagerException {
        this.level = level;

        if (!(level.getBlockState(blockPos).getBlock() instanceof BreadboardBlock)) {
            throw new BoardManagerException("target_isnt_breadboard");
        }

        // North then west
        BlockPos curNwCorner = blockPos;

        while (level.getBlockState(curNwCorner.north()).getBlock() instanceof BreadboardBlock) {
            curNwCorner = curNwCorner.north();
        }

        while (level.getBlockState(curNwCorner.west()).getBlock() instanceof BreadboardBlock) {
            curNwCorner = curNwCorner.west();
        }

        nwCorner = curNwCorner;

        // West then north
        curNwCorner = blockPos;

        while (level.getBlockState(curNwCorner.west()).getBlock() instanceof BreadboardBlock) {
            curNwCorner = curNwCorner.west();
        }

        while (level.getBlockState(curNwCorner.north()).getBlock() instanceof BreadboardBlock) {
            curNwCorner = curNwCorner.north();
        }

        if (!nwCorner.equals(curNwCorner)) {
            throw new BoardManagerException("board_not_isolated");
        }

        final var socketBlockMap = new HashMap<Direction, ArrayList<BlockPos>>();
        Direction.Plane.HORIZONTAL.forEach(dir -> socketBlockMap.put(dir, new ArrayList<>()));

        final var lastSocketKinds = new HashMap<Direction, BreadboardKindEnum>();

        for (int z = 0; z < BOARD_SIZE; z++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                BlockPos currentPos = nwCorner.offset(x, 0, z);

                BlockState currentBlockState = level.getBlockState(currentPos);

                if (!(currentBlockState.getBlock() instanceof BreadboardBlock)) {
                    throw new BoardManagerException("grid_dimensions_incorrect");
                }

                BreadboardKindEnum currentKind = currentBlockState.getValue(BreadboardBlock.KIND);

                Direction socketDir = null;

                if (z == 0) {
                    if (x != 0 && x != BOARD_SIZE - 1) {
                        socketDir = Direction.NORTH;
                    }

                    if (level.getBlockState(currentPos.relative(Direction.NORTH)).getBlock() instanceof BreadboardBlock) {
                        throw new BoardManagerException("board_not_isolated");
                    }
                }

                if (x == BOARD_SIZE - 1) {
                    if (z != 0 && z != BOARD_SIZE - 1) {
                        socketDir = Direction.EAST;
                    }

                    if (level.getBlockState(currentPos.relative(Direction.EAST)).getBlock() instanceof BreadboardBlock) {
                        throw new BoardManagerException("board_not_isolated");
                    }
                }

                if (z == BOARD_SIZE - 1) {
                    if (x != 0 && x != BOARD_SIZE - 1) {
                        socketDir = Direction.SOUTH;
                    }

                    if (level.getBlockState(currentPos.relative(Direction.SOUTH)).getBlock() instanceof BreadboardBlock) {
                        throw new BoardManagerException("board_not_isolated");
                    }
                }

                if (x == 0) {
                    if (z != 0 && z != BOARD_SIZE - 1) {
                        socketDir = Direction.WEST;
                    }

                    if (level.getBlockState(currentPos.relative(Direction.WEST)).getBlock() instanceof BreadboardBlock) {
                        throw new BoardManagerException("board_not_isolated");
                    }
                }

                if (socketDir != null) {
                    socketBlockMap.get(socketDir).add(currentPos);

                    if (lastSocketKinds.containsKey(socketDir)) {
                        if (currentKind != lastSocketKinds.get(socketDir)) {
                            throw new BoardManagerException("board_states_broken");
                        }
                    } else {
                        lastSocketKinds.put(socketDir, currentKind);
                    }
                } else {
                    if (currentKind != BreadboardKindEnum.NORMAL) {
                        throw new BoardManagerException("board_states_broken");
                    }
                }
            }
        }

        final var builder = new ImmutableMap.Builder<Direction, BoardSocket>();
        Direction.Plane.HORIZONTAL.forEach(dir ->
                builder.put(dir, new BoardSocket(dir, socketBlockMap.get(dir), lastSocketKinds.get(dir).getState()))
        );
        sockets = builder.build();
    }
}
