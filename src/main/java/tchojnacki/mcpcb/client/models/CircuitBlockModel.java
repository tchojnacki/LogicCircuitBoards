package tchojnacki.mcpcb.client.models;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.common.block.entities.CircuitBlockEntity;
import tchojnacki.mcpcb.logic.KnownTable;
import tchojnacki.mcpcb.logic.RelDir;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Responsible for rendering the top face of a circuit block placed in the world.
 * All other faces are rendered by the base model from {@code circuit.json} inside resources/assets/mcpcb/models/block.
 *
 * @see IBakedModelParentOverride
 * @see <a href="https://github.com/TheGreyGhost/MinecraftByExample/tree/master/src/main/java/minecraftbyexample/mbe04_block_dynamic_block_models">MBE04_BLOCK_DYNAMIC_BLOCK_MODELS</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitBlockModel extends IBakedModelParentOverride {
    /**
     * Constructor for the model.
     *
     * @param baseModel base model defined in {@code circuit.json}
     */
    public CircuitBlockModel(BakedModel baseModel) {
        super(baseModel);
    }

    // Model properties
    private static final ModelProperty<Direction> FACING_PROP = new ModelProperty<>();
    private static final ModelProperty<String> CENTER_TEXTURE_PROP = new ModelProperty<>();

    // Model properties for side states (input/output/neither)
    private static final ImmutableMap<Direction, ModelProperty<Integer>> STATES = ImmutableMap.<Direction, ModelProperty<Integer>>builder()
            .put(Direction.NORTH, new ModelProperty<>())
            .put(Direction.EAST, new ModelProperty<>())
            .put(Direction.SOUTH, new ModelProperty<>())
            .put(Direction.WEST, new ModelProperty<>())
            .build();

    /**
     * Get initial model data.
     *
     * @return base {@link IModelData} of a circuit
     */
    private static ModelDataMap emptyModelData() {
        final var builder = new ModelDataMap.Builder();

        builder.withInitial(FACING_PROP, Direction.NORTH);
        builder.withInitial(CENTER_TEXTURE_PROP, KnownTable.DEFAULT_TEXTURE);

        // By default of faces are neither input nor output
        Direction.Plane.HORIZONTAL.forEach(
                d -> builder.withInitial(STATES.get(d), 0)
        );

        return builder.build();
    }

    /**
     * Fill model data of the model using information from associated {@link CircuitBlockEntity}.
     * It seems that normally, on placement, this is called after setPlacedBy, but before tile entity
     * loads NBT from BlockEntityTag, hence all needed tile entity data is set in setPlacedBy.
     *
     * @param blockGetter circuit's blockGetter
     * @param blockPos    position of the circuit
     * @param blockState  current block state at {@code blockPos}
     * @param _data       unused
     * @return final {@link IModelData} for the model
     * @see CircuitBlock#setPlacedBy(Level, BlockPos, BlockState, LivingEntity, ItemStack)
     */
    @NotNull
    @Override
    public IModelData getModelData(@NotNull BlockAndTintGetter blockGetter, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @NotNull IModelData _data) {
        IModelData data = emptyModelData();

        BlockEntity blockEntity = blockGetter.getBlockEntity(blockPos);
        if (blockEntity instanceof CircuitBlockEntity circuitEntity) {
            Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);

            data.setData(FACING_PROP, facing);
            data.setData(CENTER_TEXTURE_PROP, circuitEntity.getTexture());

            Direction.Plane.HORIZONTAL.forEach(
                    d -> data.setData(STATES.get(d), circuitEntity.getTruthTable().stateForSide(RelDir.getOffset(facing, d)))
            );
        }

        return data;
    }

    /**
     * Helper function to extract {@link ModelProperty} of {@link Integer}.
     *
     * @param data     parent of the int property
     * @param property {@link ModelProperty} to be extracted
     * @return value of {@code property}, or zero if it doesn't exist
     */
    private static int getIntData(IModelData data, ModelProperty<Integer> property) {
        if (data.hasProperty(property) && data.getData(property) != null) {
            return Objects.requireNonNull(data.getData(property));
        }
        return 0;
    }

    /**
     * Returns baked quads of the model for rendering.
     *
     * @param blockState block state of the circuit block
     * @param side       direction of requested quads
     * @param random     passed to super method
     * @param data       data generated by {@link #emptyModelData()} and {@link #getModelData(BlockAndTintGetter, BlockPos, BlockState, IModelData)}
     * @return baked quads for direction given by {@code side}
     */
    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, @NotNull Random random, @NotNull IModelData data) {
        // Only modify quads if side equals null
        if (side != null) {
            // Use quads from circuit.json for all of the other sides
            return baseModel.getQuads(blockState, side, random, data);
        }

        String centerTextureName = KnownTable.DEFAULT_TEXTURE;
        if (data.hasProperty(CENTER_TEXTURE_PROP) && data.getData(CENTER_TEXTURE_PROP) != null) {
            centerTextureName = Objects.requireNonNull(data.getData(CENTER_TEXTURE_PROP));
        }

        Direction facing = Direction.NORTH;
        if (data.hasProperty(FACING_PROP) && data.getData(FACING_PROP) != null) {
            facing = Objects.requireNonNull(data.getData(FACING_PROP));
        }

        // Return top side's quads based on model data
        return CircuitTopFaceBakery.generateQuads(
                new int[]{
                        getIntData(data, Objects.requireNonNull(STATES.get(Direction.NORTH))),
                        getIntData(data, Objects.requireNonNull(STATES.get(Direction.EAST))),
                        getIntData(data, Objects.requireNonNull(STATES.get(Direction.SOUTH))),
                        getIntData(data, Objects.requireNonNull(STATES.get(Direction.WEST)))
                },
                centerTextureName,
                facing
        );
    }

    /**
     * This signature should never be called, but is required by Minecraft's classes.
     * Forge's alternative, which allows to use {@link IModelData} is used instead.
     *
     * @throws AssertionError always thrown
     * @see #getQuads(BlockState, Direction, Random, IModelData)
     */
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState _blockState, @Nullable Direction _direction, Random _random) {
        throw new AssertionError("IBakedModel::getQuads should never be called.");
    }
}
