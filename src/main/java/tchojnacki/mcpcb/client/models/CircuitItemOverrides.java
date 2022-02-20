package tchojnacki.mcpcb.client.models;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tchojnacki.mcpcb.logic.TruthTable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Passes a correct {@link TruthTable} to use for circuit item model rendering.
 *
 * @see CircuitItemBaseModel
 * @see CircuitItemFinalisedModel
 * @see <a href="https://github.com/TheGreyGhost/MinecraftByExample/tree/master/src/main/java/minecraftbyexample/mbe15_item_dynamic_item_model">MBE15_ITEM_DYNAMIC_ITEM_MODEL</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitItemOverrides extends ItemOverrides {
    public CircuitItemOverrides() {
        super();
    }

    /**
     * Creates the appropriate {@link CircuitItemFinalisedModel} for a given {@link ItemStack}.
     *
     * @param baseModel base model passed to {@link CircuitItemFinalisedModel}
     * @param itemStack stack used to extract {@link TruthTable}
     * @param _world    unused
     * @param _player   unused
     * @return appropriate {@link CircuitItemFinalisedModel} for {@code itemStack}
     */
    @Nullable
    @Override
    public BakedModel resolve(@NotNull BakedModel baseModel, @NotNull ItemStack itemStack, @Nullable ClientLevel _world, @Nullable LivingEntity _player, int _flag) {
        // By default use an empty table (blank circuit)
        TruthTable table = TruthTable.empty();

        // Extract BlockEntityTag->TruthTable from item's NBT
        CompoundTag tag = itemStack.getTagElement("BlockEntityTag");
        if (tag != null) {
            if (tag.contains("TruthTable", CompoundTag.TAG_COMPOUND)) {
                table = TruthTable.fromNBT(tag.getCompound("TruthTable"));
            }
        }

        return new CircuitItemFinalisedModel(baseModel, table);
    }
}
