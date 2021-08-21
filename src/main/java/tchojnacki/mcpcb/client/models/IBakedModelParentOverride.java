package tchojnacki.mcpcb.client.models;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

/**
 * Helper class extended by most custom baked models.
 * By default {@link IBakedModel} requires a lot of method overriding.
 * All of the models contained in the mod delegate some of the rendering
 * to a base model (defined in a JSON file inside resources/assets/mcpcb/models).
 * This class overrides all methods required by {@link IBakedModel} such that
 * they all use the base model's functionality. These methods can be overrided
 * further to provide custom behaviour.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class IBakedModelParentOverride implements IBakedModel {
    // Model, which methods we use
    protected final IBakedModel baseModel;

    /**
     * Constructor.
     *
     * @param baseModel model that the overrides will use by default
     */
    public IBakedModelParentOverride(IBakedModel baseModel) {
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
     * @param data       additional data supplied by {@link #getModelData(IBlockDisplayReader, BlockPos, BlockState, IModelData)}
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
     * @param world      block's world
     * @param blockPos   block's position
     * @param blockState block's state
     * @param data       additional data
     * @return additional model data for blocks used by {@link #getQuads(BlockState, Direction, Random, IModelData)}
     * @throws AssertionError for items
     */
    @NotNull
    @Override
    public abstract IModelData getModelData(@NotNull IBlockDisplayReader world, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @NotNull IModelData data);

    /**
     * This method is unused and replaced by Forge's alternative which supplies {@link IModelData}.
     *
     * @throws AssertionError always throws
     * @see #getParticleTexture(IModelData)
     */
    @Override
    public final TextureAtlasSprite getParticleIcon() {
        throw new AssertionError("IBakedModel::getParticleIcon should never be called.");
    }

    /**
     * Forge's alternative to {@link #getParticleIcon()}.
     *
     * @param data additional data from {@link #getModelData(IBlockDisplayReader, BlockPos, BlockState, IModelData)}
     * @return particle texture
     */
    @Override
    public TextureAtlasSprite getParticleTexture(@NotNull IModelData data) {
        return baseModel.getParticleTexture(data);
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
    public ItemOverrideList getOverrides() {
        return baseModel.getOverrides();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemCameraTransforms getTransforms() {
        return baseModel.getTransforms();
    }
}
