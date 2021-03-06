package tchojnacki.mcpcb.client.models;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

/**
 * Helper class extended by most custom baked models.
 * By default {@link BakedModel} requires a lot of method overriding.
 * All of the models contained in the mod delegate some of the rendering
 * to a base model (defined in a JSON file inside resources/assets/mcpcb/models).
 * This class overrides all methods required by {@link BakedModel} such that
 * they all use the base model's functionality. These methods can be overrided
 * further to provide custom behaviour.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class IBakedModelParentOverride implements BakedModel {
    // Model, which methods we use
    protected final BakedModel baseModel;

    /**
     * Constructor.
     *
     * @param baseModel model that the overrides will use by default
     */
    public IBakedModelParentOverride(BakedModel baseModel) {
        this.baseModel = baseModel;
    }

    /**
     * Minecraft's method for getting quads to render.
     * It is used for item models only (block models use Forge's alternative).
     * It is abstract because it should return quad list for items and throw for blocks.
     *
     * @param blockState block's state
     * @param side       direction of requested quads
     * @param random     randomness supplier
     * @return baked quad list for items
     * @throws AssertionError for blocks
     */
    @Override
    public abstract List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, Random random);

    /**
     * Forge's method for getting quads to render.
     * It is used for block models only (item models use Minecraft's alternative).
     * It is abstract because it should return quad list for blocks and throw for items.
     *
     * @param blockState block's state
     * @param side       direction of requested quads
     * @param random     randomness supplier
     * @param data       additional data supplied by {@link #getModelData(BlockAndTintGetter, BlockPos, BlockState, IModelData)}
     * @return baked quad list for blocks
     * @throws AssertionError for items
     */
    @NotNull
    @Override
    public abstract List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, @NotNull Random random, @NotNull IModelData data);

    /**
     * Forge's method to pass additional data to the method rendering quads.
     * Should never get called in case of item models.
     *
     * @param blockGetter block's blockGetter
     * @param blockPos    block's position
     * @param blockState  block's state
     * @param data        additional data
     * @return additional model data for blocks used by {@link #getQuads(BlockState, Direction, Random, IModelData)}
     * @throws AssertionError for items
     */
    @NotNull
    @Override
    public abstract IModelData getModelData(@NotNull BlockAndTintGetter blockGetter, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @NotNull IModelData data);

    /**
     * This method is unused and replaced by Forge's alternative which supplies {@link IModelData}.
     *
     * @throws AssertionError always throws
     * @see #getParticleIcon(IModelData)
     */
    @Override
    public final TextureAtlasSprite getParticleIcon() {
        throw new AssertionError("IBakedModel::getParticleIcon should never be called.");
    }

    /**
     * @param data additional data from {@link #getModelData(BlockAndTintGetter, BlockPos, BlockState, IModelData)}
     * @return particle texture
     */
    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull IModelData data) {
        return baseModel.getParticleIcon(data);
    }

    // All of the following methods simply use base model's implementation

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return baseModel.isCustomRenderer();
    }

    @Override
    public ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }
}
