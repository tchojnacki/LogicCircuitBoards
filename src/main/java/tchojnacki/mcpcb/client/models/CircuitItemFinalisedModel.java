package tchojnacki.mcpcb.client.models;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import tchojnacki.mcpcb.logic.KnownTable;
import tchojnacki.mcpcb.logic.RelDir;
import tchojnacki.mcpcb.logic.TruthTable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

/**
 * The finalised circuit item model that renders model's top face using {@link TruthTable}
 * extracted from item stack by {@link CircuitItemOverrideList#resolve(IBakedModel, ItemStack, ClientWorld, LivingEntity)}.
 *
 * @see IBakedModelParentOverride
 * @see CircuitItemBaseModel
 * @see CircuitItemOverrideList
 * @see <a href="https://github.com/TheGreyGhost/MinecraftByExample/tree/master/src/main/java/minecraftbyexample/mbe15_item_dynamic_item_model">MBE15_ITEM_DYNAMIC_ITEM_MODEL</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitItemFinalisedModel extends IBakedModelParentOverride {
    private final int[] states;
    private final String centerTextureName;

    /**
     * Constructor used in {@link CircuitItemOverrideList#resolve(IBakedModel, ItemStack, ClientWorld, LivingEntity)}.
     *
     * @param baseModel base model
     * @param table     truth table contained in item's NBT tag
     */
    public CircuitItemFinalisedModel(IBakedModel baseModel, TruthTable table) {
        super(baseModel);

        KnownTable recognized = table.recognize();
        centerTextureName = recognized != null ? recognized.getTexture() : KnownTable.DEFAULT_TEXTURE;

        states = CircuitTopFaceBakery.DIRECTIONS.stream().mapToInt(
                dir -> table.stateForSide(
                        RelDir.getOffset(Direction.NORTH, dir) // pretend the circuit is facing north
                )
        ).toArray();
    }

    /**
     * Generates the quads used to render the circuit item's model.
     *
     * @param blockState passed to super method
     * @param side       direction of requested baked quads
     * @param random     passed to super method
     * @return baked quads for the given direction
     */
    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, @NotNull Random random) {
        // We only replace quads with null side, generate others using base model
        if (side != null) {
            //noinspection deprecation
            return baseModel.getQuads(blockState, side, random);
        }

        return CircuitTopFaceBakery.generateQuads(
                states,
                centerTextureName,
                Direction.NORTH // pretend the circuit is facing north
        );
    }

    /**
     * Always throws because this is already an override.
     *
     * @throws UnsupportedOperationException always thrown
     * @see CircuitItemBaseModel#getOverrides()
     */
    @NotNull
    @Override
    public ItemOverrideList getOverrides() {
        throw new UnsupportedOperationException("The finalised model does not have an override list.");
    }

    /**
     * Unused for item models.
     *
     * @throws AssertionError always thrown
     */
    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState _blockState, @Nullable Direction _side, @NotNull Random _random, @NotNull IModelData _data) {
        throw new AssertionError("IForgeBakedModel::getQuads should never be called.");
    }

    /**
     * Unused for item models.
     *
     * @throws AssertionError always thrown
     */
    @NotNull
    @Override
    public IModelData getModelData(@NotNull IBlockDisplayReader _world, @NotNull BlockPos _blockPos, @NotNull BlockState _blockState, @NotNull IModelData _data) {
        throw new AssertionError("IForgeBakedModel::getModelData should never be called.");
    }
}
