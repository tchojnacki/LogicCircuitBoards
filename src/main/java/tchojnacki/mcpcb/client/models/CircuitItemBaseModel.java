package tchojnacki.mcpcb.client.models;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.block.CircuitBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

/**
 * Replaces the JSON circuit item model from resources such that the top face is rendered.
 * Selects an appropriate model override using {@link CircuitItemOverrideList} and then delegates
 * its rendering to {@link CircuitItemFinalisedModel}. All of the other faces are rendered by the
 * base model defined in {@code circuit.json} inside resources/assets/mcpcb/models/item.
 *
 * @see IBakedModelParentOverride
 * @see CircuitItemOverrideList
 * @see CircuitItemFinalisedModel
 * @see <a href="https://github.com/TheGreyGhost/MinecraftByExample/tree/master/src/main/java/minecraftbyexample/mbe15_item_dynamic_item_model">MBE15_ITEM_DYNAMIC_ITEM_MODEL</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitItemBaseModel extends IBakedModelParentOverride {
    private final ItemOverrideList circuitItemOverrideList = new CircuitItemOverrideList();

    /**
     * Constructor for the model.
     *
     * @param baseModel base model defined in {@code circuit.json}
     */
    public CircuitItemBaseModel(IBakedModel baseModel) {
        super(baseModel);
    }

    // Resource for model, "inventory" is passed to specify an item (the default is a block)
    public static final ModelResourceLocation MODEL_RESOURCE_LOCATION = new ModelResourceLocation(String.format("%s:%s", MCPCB.MOD_ID, CircuitBlock.ID), "inventory");

    /**
     * Use base model (the one from {@code circuit.json}) for rendering.
     * This is used for items not included in {@link CircuitItemOverrideList}.
     * The item is normally rendered by {@link CircuitItemFinalisedModel}.
     *
     * @param blockState passed to super method
     * @param side       passed to super method
     * @param random     passed to super method
     * @return quads to render
     * @see CircuitItemFinalisedModel#getQuads(BlockState, Direction, Random)
     */
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, Random random) {
        //noinspection deprecation
        return baseModel.getQuads(blockState, side, random);
    }

    /**
     * Returns the override list, which specifies what models to use for which item kinds (based on {@link net.minecraft.item.ItemStack}).
     *
     * @return item override list
     * @see CircuitItemOverrideList#resolve(IBakedModel, ItemStack, ClientWorld, LivingEntity)
     */
    @Override
    public ItemOverrideList getOverrides() {
        return circuitItemOverrideList;
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
