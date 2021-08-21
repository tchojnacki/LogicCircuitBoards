package tchojnacki.mcpcb.common.tileentities;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.SideBoolMap;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Tile entity for the circuit block. Holds its custom name, the truth table as well as queued and current outputs.
 *
 * @see tchojnacki.mcpcb.common.block.CircuitBlock
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitBlockTileEntity extends TileEntity {
    public final static String ID = "circuit_tile_entity";

    private ITextComponent customName;

    private TruthTable truthTable = TruthTable.empty();

    /**
     * Map that determines power level that the block currently gives.
     */
    private SideBoolMap actualOutput = SideBoolMap.getEmpty();

    /**
     * Map that holds the output that will become {@link #actualOutput} after one tick.
     */
    private SideBoolMap queuedOutput = SideBoolMap.getEmpty();

    public CircuitBlockTileEntity() {
        super(Registration.CIRCUIT_BLOCK_TILE_ENTITY.get());
    }

    public SideBoolMap getActualOutput() {
        return actualOutput;
    }

    /**
     * Updates the output on tick - sets actual output to queued output.
     *
     * @see #actualOutput
     * @see #queuedOutput
     */
    public void updateOutput() {
        actualOutput = queuedOutput;
        setChanged();
    }

    /**
     * Whether the output is outdated.
     *
     * @return if actual output is different than queued output
     * @see #actualOutput
     * @see #queuedOutput
     */
    public boolean isOutputOutdated() {
        return !actualOutput.equals(queuedOutput);
    }

    /**
     * Sets queued output for a given input map.
     *
     * @param inputMap inputs for which to calculate output using the truth table
     * @return queued output after modification
     * @see #queuedOutput
     */
    public SideBoolMap setQueuedOutput(SideBoolMap inputMap) {
        queuedOutput = truthTable.getOutputsForInputs(inputMap);
        setChanged();
        return queuedOutput;
    }

    public boolean hasConnectionOnSide(RelDir side) {
        return truthTable.hasInputOrOutput(side);
    }

    public boolean hasOutputOnSide(RelDir side) {
        return truthTable.hasOutput(side);
    }

    public String getTexture() {
        return truthTable.getTexture();
    }

    public void setCustomName(ITextComponent name) {
        this.customName = name;
    }

    @Nullable
    public ITextComponent getCustomName() {
        return customName;
    }

    public TruthTable getTruthTable() {
        return truthTable;
    }

    /**
     * Save the tile entity data to NBT tag.
     *
     * @param parentTag tag to which we write the data
     * @return modified tag
     */
    @Override
    public CompoundNBT save(CompoundNBT parentTag) {
        super.save(parentTag);

        parentTag.put("TruthTable", truthTable.toNBT());

        if (customName != null) {
            parentTag.putString("CustomName", ITextComponent.Serializer.toJson(customName));
        }

        if (actualOutput.isntEmpty()) {
            parentTag.putByte("ActualOutput", actualOutput.toByte());
        }

        if (queuedOutput.isntEmpty()) {
            parentTag.putByte("QueuedOutput", queuedOutput.toByte());
        }

        return parentTag;
    }

    /**
     * Sent from server to client to update single tile entity.
     *
     * @return tile entity update packet
     */
    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT tag = new CompoundNBT();
        save(tag);
        return new SUpdateTileEntityPacket(worldPosition, 42, tag);
    }

    /**
     * Sent from server to client to update tile entities in batches.
     *
     * @return updated tag
     */
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT tag = new CompoundNBT();
        save(tag);
        return tag;
    }

    /**
     * Load the tile entity data from an NBT tag.
     *
     * @param blockState block's state
     * @param parentTag tag we are writing into
     */
    @Override
    public void load(BlockState blockState, CompoundNBT parentTag) {
        super.load(blockState, parentTag);

        setFromParentTag(parentTag);
    }

    /**
     * Helper method that saves tile entity data into a parent NBT tag.
     *
     * @param parentTag tag we are writing into
     */
    public void setFromParentTag(CompoundNBT parentTag) {
        if (parentTag.contains("TruthTable", Constants.NBT.TAG_COMPOUND)) {
            truthTable = TruthTable.fromNBT(parentTag.getCompound("TruthTable"));
        }

        if (parentTag.contains("CustomName", Constants.NBT.TAG_STRING)) {
            customName = ITextComponent.Serializer.fromJson(parentTag.getString("CustomName"));
        }

        if (parentTag.contains("ActualOutput", Constants.NBT.TAG_BYTE)) {
            actualOutput = SideBoolMap.fromByte(parentTag.getByte("ActualOutput"));
        }

        if (parentTag.contains("QueuedOutput", Constants.NBT.TAG_BYTE)) {
            queuedOutput = SideBoolMap.fromByte(parentTag.getByte("QueuedOutput"));
        }
    }

    /**
     * Loading data from a network packet when updating a single tile entity.
     *
     * @param net network manager
     * @param pkt tile entity update packet
     */
    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        if (level != null) {
            BlockState blockState = level.getBlockState(worldPosition);
            load(blockState, pkt.getTag());
        }
    }

    /**
     * Loading data during batch tile entity updates.
     *
     * @param blockState block's state
     * @param tag the tag we should load
     */
    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT tag) {
        load(blockState, tag);
    }
}
