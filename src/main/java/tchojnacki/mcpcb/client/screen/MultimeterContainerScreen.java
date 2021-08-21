package tchojnacki.mcpcb.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.container.MultimeterContainer;
import tchojnacki.mcpcb.util.MultimeterScreenRenamePacket;
import tchojnacki.mcpcb.util.PacketHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for circuit creation and crafting.
 * Used only on client side.
 *
 * @see MultimeterContainer
 */
@OnlyIn(Dist.CLIENT)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultimeterContainerScreen extends ContainerScreen<MultimeterContainer> {
    private final static ResourceLocation TEXTURE = new ResourceLocation(MCPCB.MOD_ID, "textures/gui/container/multimeter.png");
    private final String initialName;
    private TextFieldWidget name;

    public MultimeterContainerScreen(MultimeterContainer multimeterContainer, PlayerInventory playerInv, ITextComponent title) {
        super(multimeterContainer, playerInv, title);

        // Set default circuit name to item's name (localized in player's language)
        this.initialName = I18n.get("block.mcpcb.circuit");

        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        }

        this.name = new TextFieldWidget(this.font, this.leftPos + 70, this.topPos + 28, 95, 12, new TranslationTextComponent("container.mcpcb.multimeter_container.title"));
        this.name.setValue(initialName);
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setBordered(false);
        this.name.setMaxLength(MultimeterContainer.MAX_NAME_CHARS);
        this.name.setResponder(this::onNameChanged);
        this.children.add(this.name);
        this.setInitialFocus(this.name);
    }

    /**
     * Event handler called when player edits the text field.
     *
     * @param newName name after change
     * @see MultimeterContainer#setName(String)
     * @see MultimeterScreenRenamePacket
     * @see PacketHandler
     */
    private void onNameChanged(String newName) {
        // Pass empty string if text field is empty or has default value
        String formattedName = newName.equals(initialName) ? "" : newName;
        this.menu.setName(formattedName);

        // We need to use packets to send the new name to server side
        PacketHandler.INSTANCE.sendToServer(new MultimeterScreenRenamePacket(formattedName));
    }

    @Override
    public void tick() {
        super.tick();
        this.name.tick();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // Set value again after resizing, based on vanilla's AnvilScreen
        String nameStr = this.name.getValue();
        this.init(minecraft, width, height);
        this.name.setValue(nameStr);
    }

    @Override
    public void removed() {
        super.removed();

        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
        }
    }

    @Override
    public boolean keyPressed(int key, int p_231046_2_, int p_231046_3_) {
        // Close container on ESC press
        if (key == 256 && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }

        boolean keyPressResult = this.name.keyPressed(key, p_231046_2_, p_231046_3_);

        return keyPressResult || this.name.canConsumeInput() || super.keyPressed(key, p_231046_2_, p_231046_3_);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.name.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        if (this.minecraft != null) {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.minecraft.getTextureManager().bind(TEXTURE);

            // Base background texture
            this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

            ImmutableList<Slot> slotList = this.menu.getInputSlotList();

            // Render input slots backgrounds
            for (int i = 0; i < slotList.size(); i++) {
                Slot slot = slotList.get(i);
                if (!slot.hasItem()) {
                    this.blit(matrixStack, this.leftPos + slot.x, this.topPos + slot.y, this.imageWidth + i * 16, 0, 16, 16);
                }
            }

            // Render text field's background
            this.blit(matrixStack, this.leftPos + 67, this.topPos + 24, 0, this.imageHeight, 102, 16);

            // Render big circuit on left side
            RenderSystem.pushMatrix();

            float scale = 3f;

            RenderSystem.scalef(scale, scale, scale);

            itemRenderer.renderGuiItem(
                    this.menu.getCircuitStack(),
                    (int) ((this.leftPos + 12) / scale), (int) ((this.topPos + 20) / scale)
            );

            RenderSystem.popMatrix();
        }
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        // Container title and inventory label
        super.renderLabels(matrixStack, mouseX, mouseY);

        // Render cost labels
        ImmutableList<Integer> costs = this.menu.getCostArray();
        for (int i = 0; i < costs.size(); i++) {
            String numberString = String.format("x%d", costs.get(i));
            int padding = (int) Math.ceil((18 - this.font.width(numberString)) / 2f); // Text centering

            // Text's color
            int color = this.menu.getInputSlotList().get(i).getItem().getCount() >= costs.get(i) ? 0x206010 : 0xFC5F5F;

            this.font.draw(matrixStack, numberString, 67 + 20 * i + padding, 48, color);
        }
    }

    @Override
    protected void renderTooltip(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderTooltip(matrixStack, mouseX, mouseY);

        // Tooltips for the areas
        ImmutableList<String> itemKeys = ImmutableList.of("block.minecraft.terracotta", "item.minecraft.redstone", "block.minecraft.redstone_torch");

        List<ITextComponent> list = new ArrayList<>();

        // If mouse is vertically within tooltip areas
        if (mouseY >= this.topPos + 43 && mouseY < this.topPos + 57) {
            // Check all areas
            MultimeterContainer
                    .inputRange()
                    .forEach(i -> {
                        int xStart = this.leftPos + 67 + 20 * i;
                        if (mouseX >= xStart && mouseX < xStart + 18) {
                            list.add(new TranslationTextComponent(itemKeys.get(i)).append(String.format(" x%d", this.menu.getCostArray().get(i))));
                        }
                    });
        }

        this.renderComponentTooltip(matrixStack, list, mouseX, mouseY);
    }
}
