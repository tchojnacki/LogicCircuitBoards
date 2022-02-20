package tchojnacki.mcpcb.client.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.math.Vector3f;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.IModelData;
import tchojnacki.mcpcb.logic.KnownTable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Utility class that bakes quads for circuit's models (both item and block).
 * Cannot be instantiated.
 *
 * @see CircuitBlockModel#getQuads(BlockState, Direction, Random, IModelData)
 * @see CircuitItemFinalisedModel#getQuads(BlockState, Direction, Random)
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CircuitTopFaceBakery {
    /**
     * All of the methods are static, disallow instantiation.
     *
     * @throws UnsupportedOperationException always throws
     */
    private CircuitTopFaceBakery() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot be instantiated.");
    }

    private final static FaceBakery FACE_BAKERY = new FaceBakery();

    // Order of directions in the state array
    public final static ImmutableList<Direction> DIRECTIONS = ImmutableList.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    public final static ResourceLocation CORNER_TEXTURE = new ResourceLocation("mcpcb:block/circuit_corner");
    public final static ResourceLocation SOCKET_TEXTURE = new ResourceLocation("mcpcb:block/circuit_sockets");

    public final static ImmutableMap<String, ResourceLocation> CENTER_TEXTURE_MAP;

    static {
        // Static block filling the CENTER_TEXTURE_MAP with required ResourceLocations

        final var builder = new ImmutableMap.Builder<String, ResourceLocation>();

        final Consumer<String> addTexture = (String textureName) -> builder.put(textureName, new ResourceLocation("mcpcb:block/circuits/" + textureName));

        addTexture.accept(KnownTable.DEFAULT_TEXTURE);
        addTexture.accept("false");
        addTexture.accept("true");
        addTexture.accept("buffer");
        addTexture.accept("not");
        addTexture.accept("or");
        addTexture.accept("and");
        addTexture.accept("nor");
        addTexture.accept("nand");
        addTexture.accept("xor");
        addTexture.accept("xnor");
        addTexture.accept("impl");
        addTexture.accept("not_impl");
        addTexture.accept("half_adder");
        addTexture.accept("half_subtractor");
        addTexture.accept("mux");
        addTexture.accept("aoi");

        CENTER_TEXTURE_MAP = builder.build();
    }

    /**
     * Returns {@link BakedQuad} list based on side states (input/output/neither), center texture's name and facing direction.
     *
     * @param states            array of four integers (0 - empty, 1 - input or 2 - output) in order given by {@link #DIRECTIONS}
     * @param centerTextureName name of the texture used for circuit's center (logic gate symbol)
     * @param facing            direction the circuit is facing
     * @return list of baked quads used for rendering of the model
     */
    public static ImmutableList<BakedQuad> generateQuads(int[] states, String centerTextureName, Direction facing) {
        // If states are incorrect silently replace them with a blank circuit's state
        if (states.length != 4 || Arrays.stream(states).anyMatch(s -> s < 0 || s > 2)) {
            states = new int[]{0, 0, 0, 0};
        }

        final var builder = new ImmutableList.Builder<BakedQuad>();

        // Render corners
        for (int i = 0; i < 4; i++) {
            // List of x offsets of board corners
            final var offsets = Arrays.asList(0, 14, 14, 0);

            int x = offsets.get(i); // 0 14 14 0
            int y = offsets.get((i + 3) % 4); // 0 0 14 14

            // Render a corner
            builder.add(bakeQuadTop(
                    x, y, x + 2, y + 2,
                    0, 0, 2, 2,
                    i * 90,
                    CORNER_TEXTURE
            ));
        }

        // Render board sockets
        for (int i = 0; i < 4; i++) {
            // List of x offsets of board sockets
            final var offsets = Arrays.asList(2, 14, 2, 0);

            int x = offsets.get(i);
            int y = offsets.get((i + 3) % 4);

            // Width and height alternate
            int w = i % 2 == 0 ? 12 : 2;
            int h = i % 2 == 0 ? 2 : 12;

            // Render a socket
            builder.add(bakeQuadTop(
                    x, y, x + w, y + h,
                    0, getSocketV(states, i, facing),
                    12, getSocketV(states, i, facing) + 2,
                    i * 90,
                    SOCKET_TEXTURE
            ));
        }

        // Render the center texture
        builder.add(bakeQuadTop(
                2, 2, 14, 14,
                2, 2, 14, 14,
                DIRECTIONS.contains(facing) ? DIRECTIONS.indexOf(facing) * 90 : 0, // Rotate texture to match facing direction
                Objects.requireNonNull(CENTER_TEXTURE_MAP.getOrDefault(centerTextureName, CENTER_TEXTURE_MAP.get(KnownTable.DEFAULT_TEXTURE)))
        ));

        return builder.build();
    }

    /**
     * Bake a single quad on circuit's top face.
     *
     * @param x1               x coordinate of the top-left corner of the quad area
     * @param y1               y coordinate of the top-left corner of the quad area
     * @param x2               x coordinate of the bottom-right corner of the quad area (x1 + quad width)
     * @param y2               y coordinate of the bottom-right corner of the quad area (y1 + quad height)
     * @param u1               u coordinate of the top-left corner of the texture
     * @param v1               v coordinate of the top-left corner of the texture
     * @param u2               u coordinate of the bottom-right corner of the texture (u1 + texture width)
     * @param v2               v coordinate of the bottom-right corner of the texture (v1 + texture height)
     * @param uvRot            rotation of the texture
     * @param resourceLocation texture name
     * @return generated baked quad
     */
    private static BakedQuad bakeQuadTop(int x1, int y1, int x2, int y2, int u1, int v1, int u2, int v2, int uvRot, ResourceLocation resourceLocation) {
        // Based on: https://github.com/TheGreyGhost/MinecraftByExample/blob/e9862e606f6306463fccde5e3ebe576ea88f0745/src/main/java/minecraftbyexample/mbe04_block_dynamic_block_models/AltimeterBakedModel.java#L169
        return FACE_BAKERY.bakeQuad(
                new Vector3f(x1, 2, y1), // Circuit's top face is at height 2
                new Vector3f(x2, 2, y2), // Circuit's top face is at height 2
                new BlockElementFace(
                        null, // Face culling - none for this case because circuit's top can't touch any blocks
                        -1, // Disable tinting
                        "", // Dummy texture name, unused
                        new BlockFaceUV(
                                new float[]{u1, v1, u2, v2}, uvRot // Texture's u, v and rotation
                        )
                ),
                Objects.requireNonNull(ForgeModelBakery.instance()).getSpriteMap().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(
                        resourceLocation // Get texture from Minecraft's block atlas (it was put there in ClientRegistration)
                ),
                Direction.UP, // Face's direction
                SimpleModelState.IDENTITY, // No transformation
                null, // No face rotation
                true, // Shading
                resourceLocation // Dummy texture for error messages
        );
    }

    /**
     * Returns v texture coordinate of socket based on states array, index and facing direction.
     *
     * @param states state array passed to {@link #generateQuads(int[], String, Direction)}
     * @param i      direction index
     * @param facing facing direction
     * @return v coordinate of socket texture
     */
    private static int getSocketV(int[] states, int i, Direction facing) {
        return states[i] * 2 + (DIRECTIONS.get(i).equals(facing) ? 8 : 0);
    }
}
