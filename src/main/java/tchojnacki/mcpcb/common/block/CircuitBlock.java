package tchojnacki.mcpcb.common.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;
import tchojnacki.mcpcb.common.tileentities.CircuitBlockTileEntity;
import tchojnacki.mcpcb.logic.KnownTable;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.SideBoolMap;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.util.CircuitCreateTrigger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Circuit block.
 *
 * @see CircuitBlockTileEntity
 * @see tchojnacki.mcpcb.client.models.CircuitBlockModel
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitBlock extends HorizontalBlock { // extend HorizontalBlock for facing direction
    public final static String ID = "circuit";

    private final static int DELAY = 2;
    private final static VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    /**
     * Custom method called on circuit creation (using the Multimeter), used to grant criteria for achievements.
     *
     * @param itemStack    item stack that was crafted
     * @param playerEntity player which crafted the circuit
     * @see CircuitCreateTrigger
     */
    public static void onCrafted(ItemStack itemStack, PlayerEntity playerEntity) {
        if (playerEntity instanceof ServerPlayerEntity) {
            // Trigger the criteria
            CircuitCreateTrigger.TRIGGER.trigger((ServerPlayerEntity) playerEntity, itemStack);
        }
    }

    public CircuitBlock() {
        super(
                // properties based on vanilla's comparator and repeater
                Properties.of(Material.DECORATION)
                        .instabreak()
                        .sound(SoundType.WOOD)
        );

        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    /**
     * Checks if the block is being powered from a given side.
     *
     * @param world    block's world
     * @param blockPos block's position
     * @param relDir   the side
     * @return whether the circuit has power from the side
     */
    private boolean hasSignalFrom(World world, BlockPos blockPos, RelDir relDir) {
        BlockState blockState = world.getBlockState(blockPos);
        Direction direction = relDir.offsetFrom(blockState.getValue(FACING));

        return world.hasSignal(blockPos.relative(direction), direction);
    }

    /**
     * Adds {@link #FACING} to block state's definition.
     *
     * @param builder state container builder
     */
    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Orients the circuit in player's looking direction on placement.
     *
     * @param context item use context
     * @return block state with correct facing value
     */
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection());
    }

    /**
     * Use the tile entity to calculate circuit's outputs based on input signals.
     * Then schedule a tick to update circuit's neighbours after a delay.
     *
     * @param world    block's world
     * @param blockPos block's pos
     * @see CircuitBlockTileEntity
     */
    private void calculatePowerAndUpdateNeighbours(World world, BlockPos blockPos) {
        TileEntity tileEntity = world.getBlockEntity(blockPos);
        if (tileEntity instanceof CircuitBlockTileEntity) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;

            // Get power before and after update
            SideBoolMap currentPower = circuitEntity.getActualOutput();
            SideBoolMap newPower = circuitEntity.setQueuedOutput(
                    SideBoolMap.constructWith(dir -> hasSignalFrom(world, blockPos, dir))
            );

            // Schedule tick if new power is different
            if (!currentPower.equals(newPower)) {
                world.getBlockTicks().scheduleTick(blockPos, this, DELAY, TickPriority.VERY_HIGH);
            }
        }
    }

    /**
     * Called on tick scheduled in {@link #calculatePowerAndUpdateNeighbours(World, BlockPos)}.
     *
     * @param blockState  block's state
     * @param serverWorld block's world (server side)
     * @param blockPos    block's pos
     * @param _random     unused
     */
    @SuppressWarnings("deprecation")
    @Override
    public void tick(BlockState blockState, ServerWorld serverWorld, BlockPos blockPos, Random _random) {
        TileEntity tileEntity = serverWorld.getBlockEntity(blockPos);
        if (tileEntity instanceof CircuitBlockTileEntity) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;
            // Update only if the output will change
            if (circuitEntity.isOutputOutdated()) {
                circuitEntity.updateOutput();

                // Update immediate neighbouring blocks
                serverWorld.updateNeighborsAt(blockPos, this);

                /*
                If there is an output on given side update neighbours of neighbouring block too.
                In case there is a solid block at the output, the signal should propagate further
                (because circuit block provides strong signal), hence another set of blocks needs updating.

                For instance, consider a horizontal plane with a circuit with an output at each side, marked as '*' below:
                  2
                 212
                21*12
                 212
                  2
                The blocks marked as '1' would get updated by the command above, while blocks marked as '2' would
                get updated by the loop below.

                TODO: As a possible performance increase we might request block updates only if the direct neighbour
                    can conduct redstone power (it is solid). Meaning we only update blocks marked '2' from the example
                    above only if their according '1' blocks conduct power
                 */
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (circuitEntity.hasOutputOnSide(RelDir.getOffset(blockState.getValue(FACING), direction))) {
                        serverWorld.updateNeighborsAtExceptFromFacing(blockPos.relative(direction), this, direction.getOpposite());
                    }
                }
            }
        }
    }

    /**
     * Returns if the block has a tile entity, always true.
     *
     * @param _state unused
     * @return always true
     * @see CircuitBlockTileEntity
     */
    @Override
    public boolean hasTileEntity(BlockState _state) {
        return true;
    }

    /**
     * Creates and returns an appropriate tile enity.
     *
     * @param _state unused
     * @param _world unused
     * @return this block's {@link CircuitBlockTileEntity}
     * @see CircuitBlockTileEntity
     */
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState _state, IBlockReader _world) {
        return new CircuitBlockTileEntity();
    }

    /**
     * Returns if the block is a power source, always true.
     *
     * @param _blockState unused
     * @return always true
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(BlockState _blockState) {
        // TODO: It is worth investigating if returning false for blank circuits (or circuits containing only inputs) would give any performance benefit
        return true;
    }

    /**
     * Returns block's WEAK power (the one that can't go through blocks).
     *
     * @param blockState  block's state
     * @param blockReader block reader
     * @param blockPos    block's pos
     * @param direction   direction FROM NEIGHBOUR TO THIS BLOCK
     * @return WEAK power this block supplies to it's neighbour at {@code direction.getOpposite()}
     */
    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(BlockState blockState, IBlockReader blockReader, BlockPos blockPos, Direction direction) {
        TileEntity tileEntity = blockReader.getBlockEntity(blockPos);
        if (tileEntity instanceof CircuitBlockTileEntity) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;

            return circuitEntity
                    .getActualOutput()
                    .get(
                            RelDir.getOffset(
                                    blockState.getValue(FACING),
                                    direction.getOpposite() // getOpposite to get direction from this block to neighbour
                            )
                    ) ? 15 : 0;
        }

        return 0;
    }

    /**
     * Returns block's STRONG power (the one that can propagate through blocks).
     *
     * @param blockState  block's state
     * @param blockReader block reader
     * @param blockPos    block's pos
     * @param direction   direction FROM NEIGHBOUR TO THIS BLOCK
     * @return STRONG power this block supplies to it's neighbour at {@code direction.getOpposite()}
     */
    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(BlockState blockState, IBlockReader blockReader, BlockPos blockPos, Direction direction) {
        return blockState.getSignal(blockReader, blockPos, direction);
    }

    /**
     * Whether this block can connect redstone wire on a given side or not.
     *
     * @param state       block's state
     * @param blockReader block reader
     * @param blockPos    block's pos
     * @param side        side at which we check for connection
     * @return if the block connects redstone at in direction given by {@code side}
     */
    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader blockReader, BlockPos blockPos, @Nullable Direction side) {
        TileEntity tileEntity = blockReader.getBlockEntity(blockPos);
        if (tileEntity instanceof CircuitBlockTileEntity && Direction.Plane.HORIZONTAL.test(side)) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;
            BlockState blockState = blockReader.getBlockState(blockPos);
            RelDir relDir = RelDir.getOffset(blockState.getValue(FACING), side.getOpposite());

            return circuitEntity.hasConnectionOnSide(relDir);
        }

        return false;
    }

    /**
     * Returns block's collision shape.
     * It is a 16x16x2 pixel box (same as redstone repeater or comparator).
     *
     * @return circuit's collision shape
     * @see #SHAPE
     */
    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState _blockState, IBlockReader _blockReader, BlockPos _blockPos, ISelectionContext _context) {
        return SHAPE;
    }

    /**
     * Returns if the block can exist at a given position (checks if there is a supporting block below).
     *
     * @param _blockState unused
     * @param worldReader world reader
     * @param blockPos    block's position
     * @return if the block can survive at {@code blockPos}
     * @see #canSupportRigidBlock(IBlockReader, BlockPos)
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean canSurvive(BlockState _blockState, IWorldReader worldReader, BlockPos blockPos) {
        return canSupportRigidBlock(worldReader, blockPos.below());
    }

    /**
     * Called when block is placed.
     * Calculate initial power.
     *
     * @param _blockState        unused
     * @param world              block's world
     * @param blockPos           block's position
     * @param _blockStateUpdated unused
     * @param _flag              unused
     * @see #calculatePowerAndUpdateNeighbours(World, BlockPos)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onPlace(BlockState _blockState, World world, BlockPos blockPos, BlockState _blockStateUpdated, boolean _flag) {
        calculatePowerAndUpdateNeighbours(world, blockPos);
    }

    /**
     * Called when the circuit gets destroyed.
     * Update neighbours which might have been receiving power from circuit (and no longer do).
     *
     * @param blockState        block's state
     * @param world             block's world
     * @param blockPos          block's pos
     * @param updatedBlockState block state after removal, passed to super method
     * @param flag              passed to super method
     * @see #calculatePowerAndUpdateNeighbours(World, BlockPos)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState blockState, World world, BlockPos blockPos, BlockState updatedBlockState, boolean flag) {
        if (!flag && !blockState.is(updatedBlockState.getBlock())) {
            super.onRemove(blockState, world, blockPos, updatedBlockState, false);

            world.updateNeighborsAt(blockPos, this);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                // TODO: Update only neighbours separated by a conducting block, for more info see calculatePowerAndUpdateNeighbours()
                world.updateNeighborsAtExceptFromFacing(blockPos.relative(direction), this, direction.getOpposite());
            }
        }
    }

    /**
     * Called when a neighbour updates. Remove the circuit if the block below was destroyed, update signal otherwise.
     *
     * @param blockState block's state
     * @param world      block's world
     * @param blockPos   block's position
     * @param _block     unused
     * @param _updatePos unused
     * @param _flag      unused
     * @see #canSurvive(BlockState, IWorldReader, BlockPos)
     * @see #calculatePowerAndUpdateNeighbours(World, BlockPos)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState blockState, World world, BlockPos blockPos, Block _block, BlockPos _updatePos, boolean _flag) {
        if (!blockState.canSurvive(world, blockPos)) {
            world.removeBlock(blockPos, false);
            return;
        }

        calculatePowerAndUpdateNeighbours(world, blockPos);
    }

    /**
     * Returns the item stack a creative player obtains when middle-clicking a placed circuit block.
     *
     * @param blockReader block reader
     * @param blockPos    block's position
     * @param blockState  block's state
     * @return clone item stack
     */
    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(IBlockReader blockReader, BlockPos blockPos, BlockState blockState) {
        ItemStack itemStack = new ItemStack(this);

        TileEntity tileEntity = blockReader.getBlockEntity(blockPos);
        if (tileEntity instanceof CircuitBlockTileEntity) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;

            // Copy NBT
            itemStack.getOrCreateTagElement("BlockEntityTag").put("TruthTable", circuitEntity.getTruthTable().toNBT());

            // Copy custom name
            if (circuitEntity.getCustomName() != null) {
                itemStack.setHoverName(circuitEntity.getCustomName());
            }
        }

        return itemStack;
    }

    /**
     * Returns an item stack for a given truth table.
     *
     * @param table given truth table
     * @return an item stack for {@code table}
     */
    public ItemStack stackFromTable(TruthTable table) {
        ItemStack itemStack = new ItemStack(this);

        itemStack.getOrCreateTagElement("BlockEntityTag").put("TruthTable", table.toNBT());

        return itemStack;
    }

    /**
     * Method which allows to pass data from an item stack to a placed block.
     * It has a special use in a circuit block - it passes NBT data from item stack to block.
     * Normally it gets copied from item stack's BlockEntityTag tag onto the block, however that
     * happens after the block is rendered by {@link tchojnacki.mcpcb.client.models.CircuitBlockModel}.
     * To make sure it is copied before baking quads we pass the data here instead.
     *
     * @param world      block's world
     * @param blockPos   block's position
     * @param blockState block's state
     * @param entity     player which placed the circuit
     * @param itemStack  circuit's item stack before placement
     */
    @Override
    public void setPlacedBy(World world, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity entity, ItemStack itemStack) {
        super.setPlacedBy(world, blockPos, blockState, entity, itemStack);

        TileEntity tileEntity = world.getBlockEntity(blockPos);
        if (tileEntity instanceof CircuitBlockTileEntity) {
            CircuitBlockTileEntity circuitEntity = (CircuitBlockTileEntity) tileEntity;

            // Copy item name - the normal use for overriding this method
            if (itemStack.hasCustomHoverName()) {
                circuitEntity.setCustomName(itemStack.getHoverName());
            }

            // Fix for incorrect IBakedModel render on placement
            CompoundNBT tag = itemStack.getTagElement("BlockEntityTag");
            if (tag != null) {
                circuitEntity.setFromParentTag(tag);
            }
        }
    }

    /**
     * Helper method that returns a direction list in form of a {@link IFormattableTextComponent}.
     *
     * @param type "inputs" or "outputs"
     * @param directions list of directions to include in a list
     * @return formatted text with a header based on {@code type} and a list of comma separated directions
     */
    private IFormattableTextComponent dirListTextComponent(String type, List<RelDir> directions) {
        return new TranslationTextComponent("util.mcpcb.circuit_desc." + type) // list header
                .withStyle(TextFormatting.GRAY)
                .append(new StringTextComponent(
                        directions
                                .stream()
                                .map(RelDir::translationComponent) // return translation of each direction
                                .map(TranslationTextComponent::getString)
                                .collect(Collectors.joining(", "))
                ).withStyle(TextFormatting.GRAY));
    }

    /**
     * Appends additional text shown when hovering circuit's item stack.
     * It contains the recognized circuit's name (as long as it is different from its custom name)
     * and a list of input and output directions.
     *
     * @param itemStack item stack for which to add hover text
     * @param _blockReader unused
     * @param rows row list to which we append additional description lines
     * @param _flag unused
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack itemStack, @Nullable IBlockReader _blockReader, List<ITextComponent> rows, ITooltipFlag _flag) {
        CompoundNBT tag = itemStack.getTagElement("BlockEntityTag");
        if (tag != null) {
            if (tag.contains("TruthTable", Constants.NBT.TAG_COMPOUND)) {
                TruthTable table = TruthTable.fromNBT(tag.getCompound("TruthTable"));

                // Add a blue circuit name if it isn't mentioned in its custom name
                KnownTable knownTable = table.recognize();
                if (knownTable != null) {
                    TranslationTextComponent circuitName = knownTable.getTranslationKey();

                    if (!itemStack.hasCustomHoverName() || !itemStack.getHoverName().getString().equals(circuitName.getString())) {
                        rows.add(circuitName.plainCopy().withStyle(TextFormatting.BLUE));
                    }
                }

                // List inputs
                if (table.getInputs().size() > 0) {
                    rows.add(dirListTextComponent("inputs", table.getInputs()));
                }

                // List outputs
                if (table.getOutputs().size() > 0) {
                    rows.add(dirListTextComponent("outputs", table.getOutputs()));
                }
            }
        }
    }

    /**
     * Returns block's render type.
     *
     * @param _blockState unused
     * @return circuit's render type - model
     */
    @SuppressWarnings("deprecation")
    @Override
    public BlockRenderType getRenderShape(BlockState _blockState) {
        return BlockRenderType.MODEL;
    }
}
