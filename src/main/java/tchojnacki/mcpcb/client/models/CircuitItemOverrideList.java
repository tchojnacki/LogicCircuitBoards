package tchojnacki.mcpcb.client.models;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;
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
public class CircuitItemOverrideList extends ItemOverrideList {
    public CircuitItemOverrideList() {
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
    public IBakedModel resolve(@NotNull IBakedModel baseModel, @NotNull ItemStack itemStack, @Nullable ClientWorld _world, @Nullable LivingEntity _player) {
        // By default use an empty table (blank circuit)
        TruthTable table = TruthTable.empty();

        // Extract BlockEntityTag->TruthTable from item's NBT
        CompoundNBT tag = itemStack.getTagElement("BlockEntityTag");
        if (tag != null) {
            if (tag.contains("TruthTable", Constants.NBT.TAG_COMPOUND)) {
                table = TruthTable.fromNBT(tag.getCompound("TruthTable"));
            }
        }

        return new CircuitItemFinalisedModel(baseModel, table);
    }
}
