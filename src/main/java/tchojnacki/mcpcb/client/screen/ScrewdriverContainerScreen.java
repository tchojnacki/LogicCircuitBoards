package tchojnacki.mcpcb.client.screen;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.container.ScrewdriverContainer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

/**
 * Screen for breadboard configuration.
 * Used only on client side.
 *
 * @see ScrewdriverContainer
 */
@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ScrewdriverContainerScreen extends ContainerScreen<ScrewdriverContainer> {
    private final static ResourceLocation TEXTURE = new ResourceLocation(MCPCB.MOD_ID, "textures/gui/container/screwdriver.png");

    private final static ImmutableMap<Direction, Tuple<Vector2f, Vector2f>> BUTTON_AREAS = new ImmutableMap.Builder<Direction, Tuple<Vector2f, Vector2f>>()
            .put(Direction.NORTH, new Tuple<>(new Vector2f(64, 35), new Vector2f(111, 50)))
            .put(Direction.EAST, new Tuple<>(new Vector2f(120, 59), new Vector2f(135, 106)))
            .put(Direction.SOUTH, new Tuple<>(new Vector2f(64, 115), new Vector2f(111, 130)))
            .put(Direction.WEST, new Tuple<>(new Vector2f(40, 59), new Vector2f(55, 106)))
            .build();

    private final static int BLOCK_SIZE = 16;
    private final static int BORDER_PADDING = 2;

    public ScrewdriverContainerScreen(ScrewdriverContainer screwdriverContainer, PlayerInventory playerInv, ITextComponent title) {
        super(screwdriverContainer, playerInv, title);

        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int buttonId;

        if (containedInButtonArea(mouseX, mouseY, BUTTON_AREAS.get(Direction.NORTH))) {
            buttonId = 0;
        } else if (containedInButtonArea(mouseX, mouseY, BUTTON_AREAS.get(Direction.EAST))) {
            buttonId = 1;
        } else if (containedInButtonArea(mouseX, mouseY, BUTTON_AREAS.get(Direction.SOUTH))) {
            buttonId = 2;
        } else if (containedInButtonArea(mouseX, mouseY, BUTTON_AREAS.get(Direction.WEST))) {
            buttonId = 3;
        } else {
            buttonId = -1;
        }

        if (buttonId != -1 && mouseButton == 0 && this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null) {
            this.menu.clickMenuButton(this.minecraft.player, buttonId);
            this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, buttonId);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Check if mouse is hovering over a button.
     *
     * @param x    mouse x
     * @param y    mouse y
     * @param area tuple with first element containing top-left corner and second element containing bottom-right corner
     * @return whether or not mouse is within the {@code area}
     */
    private boolean containedInButtonArea(double x, double y, Tuple<Vector2f, Vector2f> area) {
        return leftPos + area.getA().x - BORDER_PADDING <= x && x <= leftPos + area.getB().x + BORDER_PADDING && topPos + area.getA().y - BORDER_PADDING <= y && y <= topPos + area.getB().y + BORDER_PADDING;
    }

    /**
     * Gets texture position for a button depending on it's position and state.
     *
     * @param direction button's direction
     * @param state     socket state at {@code direction} (0 - empty, 1 - input, 2 - output)
     * @param isHovered whether player is hovering over the button
     * @return vector containing u and v of button's texture
     */
    private Vector2f getButtonTexture(Direction direction, int state, boolean isHovered) {
        int dirIndex = Arrays.asList(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}).indexOf(direction);

        return new Vector2f(dirIndex * BLOCK_SIZE + (isHovered ? 4 * BLOCK_SIZE : 0), imageHeight + state * BLOCK_SIZE);
    }

    /**
     * Gets button border's texture for a given direction.
     *
     * @param direction button's direction
     * @param isHovered whether player is hovering over the button
     * @return vector containing u and v of border's texture
     */
    private Vector2f getBorderTexture(Direction direction, boolean isHovered) {
        if (direction.getAxis() == Direction.Axis.Z) {
            return new Vector2f(imageWidth, isHovered ? (BLOCK_SIZE + 2 * BORDER_PADDING) : 0);
        } else {
            return new Vector2f(imageWidth + (isHovered ? BLOCK_SIZE + 2 * BORDER_PADDING : 0), 2 * BLOCK_SIZE + 4 * BORDER_PADDING);
        }
    }

    /**
     * Returns border's dimensions depending on button's direction.
     *
     * @param direction button's direction
     * @return vector containing width and height of button's border
     */
    private Vector2f getBorderSize(Direction direction) {
        int longer = BORDER_PADDING + 3 * BLOCK_SIZE + BORDER_PADDING;
        int shorter = BORDER_PADDING + BLOCK_SIZE + BORDER_PADDING;

        if (direction.getAxis() == Direction.Axis.Z) {
            return new Vector2f(longer, shorter);
        } else {
            return new Vector2f(shorter, longer);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        if (this.minecraft != null) {
            //noinspection deprecation
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.minecraft.getTextureManager().bind(TEXTURE);
            this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int state = menu.getSocketStateNumber(direction);
                Tuple<Vector2f, Vector2f> btnArea = BUTTON_AREAS.get(direction);
                boolean isHovered = containedInButtonArea(mouseX, mouseY, btnArea);
                Vector2f buttonTextureLocation = getButtonTexture(direction, state, isHovered);
                Vector2f borderTextureLocation = getBorderTexture(direction, isHovered);
                Vector2f borderSize = getBorderSize(direction);

                this.blit(
                        matrixStack,
                        leftPos + (int) btnArea.getA().x - BORDER_PADDING,
                        topPos + (int) btnArea.getA().y - BORDER_PADDING,
                        (int) borderTextureLocation.x, (int) borderTextureLocation.y,
                        (int) borderSize.x, (int) borderSize.y
                );

                for (int x = (int) btnArea.getA().x + leftPos; x < (int) btnArea.getB().x + leftPos; x += BLOCK_SIZE) {
                    for (int y = (int) btnArea.getA().y + topPos; y < (int) btnArea.getB().y + topPos; y += BLOCK_SIZE) {
                        this.blit(matrixStack, x, y, (int) buttonTextureLocation.x, (int) buttonTextureLocation.y, BLOCK_SIZE, BLOCK_SIZE);
                    }
                }
            }
        }
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.font.draw(matrixStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
    }
}
