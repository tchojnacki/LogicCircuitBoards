package tchojnacki.mcpcb.client.screen;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.container.ScrewdriverContainer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Objects;

record Vector(int x, int y) {}

/**
 * Screen for breadboard configuration.
 * Used only on client side.
 *
 * @see ScrewdriverContainer
 */
@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ScrewdriverContainerScreen extends AbstractContainerScreen<ScrewdriverContainer> {
    private final static ResourceLocation TEXTURE = new ResourceLocation(MCPCB.MOD_ID, "textures/gui/container/screwdriver.png");

    private final static ImmutableMap<Direction, Tuple<Vector, Vector>> BUTTON_AREAS = new ImmutableMap.Builder<Direction, Tuple<Vector, Vector>>()
            .put(Direction.NORTH, new Tuple<>(new Vector(64, 35), new Vector(111, 50)))
            .put(Direction.EAST, new Tuple<>(new Vector(120, 59), new Vector(135, 106)))
            .put(Direction.SOUTH, new Tuple<>(new Vector(64, 115), new Vector(111, 130)))
            .put(Direction.WEST, new Tuple<>(new Vector(40, 59), new Vector(55, 106)))
            .build();

    private final static int BLOCK_SIZE = 16;
    private final static int BORDER_PADDING = 2;

    public ScrewdriverContainerScreen(ScrewdriverContainer screwdriverContainer, Inventory playerInv, Component title) {
        super(screwdriverContainer, playerInv, title);

        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int buttonId;

        if (containedInButtonArea(mouseX, mouseY, Objects.requireNonNull(BUTTON_AREAS.get(Direction.NORTH)))) {
            buttonId = 0;
        } else if (containedInButtonArea(mouseX, mouseY, Objects.requireNonNull(BUTTON_AREAS.get(Direction.EAST)))) {
            buttonId = 1;
        } else if (containedInButtonArea(mouseX, mouseY, Objects.requireNonNull(BUTTON_AREAS.get(Direction.SOUTH)))) {
            buttonId = 2;
        } else if (containedInButtonArea(mouseX, mouseY, Objects.requireNonNull(BUTTON_AREAS.get(Direction.WEST)))) {
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
    private boolean containedInButtonArea(double x, double y, Tuple<Vector, Vector> area) {
        return leftPos + area.getA().x() - BORDER_PADDING <= x && x <= leftPos + area.getB().x() + BORDER_PADDING && topPos + area.getA().y() - BORDER_PADDING <= y && y <= topPos + area.getB().y() + BORDER_PADDING;
    }

    /**
     * Gets texture position for a button depending on it's position and state.
     *
     * @param direction button's direction
     * @param state     socket state at {@code direction} (0 - empty, 1 - input, 2 - output)
     * @param isHovered whether player is hovering over the button
     * @return vector containing u and v of button's texture
     */
    private Vector getButtonTexture(Direction direction, int state, boolean isHovered) {
        int dirIndex = Arrays.asList(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}).indexOf(direction);

        return new Vector(dirIndex * BLOCK_SIZE + (isHovered ? 4 * BLOCK_SIZE : 0), imageHeight + state * BLOCK_SIZE);
    }

    /**
     * Gets button border's texture for a given direction.
     *
     * @param direction button's direction
     * @param isHovered whether player is hovering over the button
     * @return vector containing u and v of border's texture
     */
    private Vector getBorderTexture(Direction direction, boolean isHovered) {
        if (direction.getAxis() == Direction.Axis.Z) {
            return new Vector(imageWidth, isHovered ? (BLOCK_SIZE + 2 * BORDER_PADDING) : 0);
        } else {
            return new Vector(imageWidth + (isHovered ? BLOCK_SIZE + 2 * BORDER_PADDING : 0), 2 * BLOCK_SIZE + 4 * BORDER_PADDING);
        }
    }

    /**
     * Returns border's dimensions depending on button's direction.
     *
     * @param direction button's direction
     * @return vector containing width and height of button's border
     */
    private Vector getBorderSize(Direction direction) {
        int longer = BORDER_PADDING + 3 * BLOCK_SIZE + BORDER_PADDING;
        int shorter = BORDER_PADDING + BLOCK_SIZE + BORDER_PADDING;

        if (direction.getAxis() == Direction.Axis.Z) {
            return new Vector(longer, shorter);
        } else {
            return new Vector(shorter, longer);
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        if (this.minecraft != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, TEXTURE);
            this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int state = menu.getSocketStateNumber(direction);
                Tuple<Vector, Vector> btnArea = BUTTON_AREAS.get(direction);
                boolean isHovered = containedInButtonArea(mouseX, mouseY, Objects.requireNonNull(btnArea));
                Vector buttonTextureLocation = getButtonTexture(direction, state, isHovered);
                Vector borderTextureLocation = getBorderTexture(direction, isHovered);
                Vector borderSize = getBorderSize(direction);

                this.blit(
                        matrixStack,
                        leftPos + btnArea.getA().x() - BORDER_PADDING,
                        topPos + btnArea.getA().y() - BORDER_PADDING,
                        borderTextureLocation.x(), borderTextureLocation.y(),
                        borderSize.x(), borderSize.y()
                );

                for (int x = btnArea.getA().x() + leftPos; x < btnArea.getB().x() + leftPos; x += BLOCK_SIZE) {
                    for (int y = btnArea.getA().y() + topPos; y < btnArea.getB().y() + topPos; y += BLOCK_SIZE) {
                        this.blit(matrixStack, x, y, buttonTextureLocation.x(), buttonTextureLocation.y(), BLOCK_SIZE, BLOCK_SIZE);
                    }
                }
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        this.font.draw(matrixStack, this.title, this.titleLabelX, this.titleLabelY, 0x404040);
    }
}
