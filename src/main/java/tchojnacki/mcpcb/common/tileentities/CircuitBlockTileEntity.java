package tchojnacki.mcpcb.common.tileentities;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.SideBoolMap;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Tile entity for the circuit block. Holds its custom name, the truth table as well as queued and current outputs.
 *
 * @see tchojnacki.mcpcb.common.block.CircuitBlock
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitBlockTileEntity extends BlockEntity {
    public final static String ID = "circuit_tile_entity";

    private TextComponent customName;

    private TruthTable truthTable = TruthTable.empty();

    /**
     * Map that determines power level that the block currently gives.
     */
    private SideBoolMap actualOutput = SideBoolMap.getEmpty();

    /**
     * Map that holds the output that will become {@link #actualOutput} after one tick.
     */
    private SideBoolMap queuedOutput = SideBoolMap.getEmpty();

    public CircuitBlockTileEntity(BlockPos blockPos, BlockState blockState) {
        super(Registration.CIRCUIT_BLOCK_TILE_ENTITY.get(), blockPos, blockState);
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

    public void setCustomName(TextComponent name) {
        this.customName = name;
    }

    @Nullable
    public TextComponent getCustomName() {
        return customName;
    }

    public TruthTable getTruthTable() {
        return truthTable;
    }

    /**
     * Save the tile entity data to NBT tag.
     *
     * @param parentTag tag to which we write the data
     */
    @Override
    public void saveAdditional(CompoundTag parentTag) {
        super.saveAdditional(parentTag);

        parentTag.put("TruthTable", truthTable.toNBT());

        if (customName != null) {
            parentTag.putString("CustomName", TextComponent.Serializer.toJson(customName));
        }

        if (actualOutput.isntEmpty()) {
            parentTag.putByte("ActualOutput", actualOutput.toByte());
        }

        if (queuedOutput.isntEmpty()) {
            parentTag.putByte("QueuedOutput", queuedOutput.toByte());
        }
    }

    /**
     * Sent from server to client to update single tile entity.
     *
     * @return tile entity update packet
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Sent from server to client to update tile entities in batches.
     *
     * @return updated tag
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    /**
     * Load the tile entity data from an NBT tag.
     *
     * @param parentTag tag we are writing into
     */
    @Override
    public void load(CompoundTag parentTag) {
        super.load(parentTag);

        setFromParentTag(parentTag);
    }

    /**
     * Helper method that saves tile entity data into a parent NBT tag.
     *
     * @param parentTag tag we are writing into
     */
    public void setFromParentTag(CompoundTag parentTag) {
        if (parentTag.contains("TruthTable", CompoundTag.TAG_COMPOUND)) {
            truthTable = TruthTable.fromNBT(parentTag.getCompound("TruthTable"));
        }

        if (parentTag.contains("CustomName", CompoundTag.TAG_STRING)) {
            customName = (TextComponent) Component.Serializer.fromJson(parentTag.getString("CustomName"));
        }

        if (parentTag.contains("ActualOutput", CompoundTag.TAG_BYTE)) {
            actualOutput = SideBoolMap.fromByte(parentTag.getByte("ActualOutput"));
        }

        if (parentTag.contains("QueuedOutput", CompoundTag.TAG_BYTE)) {
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
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (level != null) {
            load(Objects.requireNonNull(pkt.getTag()));
        }
    }

    /**
     * Loading data during batch tile entity updates.
     *
     * @param tag the tag we should load
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
