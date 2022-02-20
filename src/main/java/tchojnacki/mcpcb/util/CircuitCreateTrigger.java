package tchojnacki.mcpcb.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.logic.KnownTable;
import tchojnacki.mcpcb.logic.TruthTable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * A trigger for advancement critetia, usable inside advancement JSON files as "mcpcb:circuit_create".
 * Certain advancements require crafting specific types of logic gates, which is nearly impossible to track
 * with vanilla's triggers (we would have to check for dozens of NBTs in "minecraft:inventory_changed" trigger).
 * <p>
 * This trigger has an optional predicate "ids" which checks if created circuit block is any of the passed known circuits.
 * If it is not supplied, then the trigger fires on any circuit block creation. Examples:
 * <pre>
 * "criteria": {
 *   "obtain_circuit": {
 *     "trigger": "mcpcb:circuit_create"
 *   }
 * }
 * </pre>
 * Criterion "obtain_circuit" gets fulfilled when any circuit block is created.
 * <pre>
 * "obtain_nand_nor": {
 *   "trigger": "mcpcb:circuit_create",
 *   "conditions": {
 *     "ids": ["nand_2", "nand_3", "nor_2", "nor_3"]
 *   }
 * }
 * </pre>
 * Criterion "obtain_nand_nor" gets fulfilled when a circuit block which contains a truth table recognized
 * as EITHER "nand_2', "nand_3", "nor_2" or "nor_3" gets crafted.
 * For more examples check resources/data/mcpcb/advancements/main.
 * <p>
 * This trigger is fired from {@link CircuitBlock#onCrafted(ItemStack, Player)}.
 * <p>
 * This trigger is registered in {@link Registration#register()}.
 *
 * @see <a href="https://minecraft.fandom.com/wiki/Advancement">Minecraft Wiki - Advancement</a>
 * @see <a href="https://minecraft.fandom.com/wiki/Advancement/JSON_format">Minecraft Wiki - Advancement/JSON format</a>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CircuitCreateTrigger extends SimpleCriterionTrigger<CircuitCreateTrigger.Instance> {
    public static final CircuitCreateTrigger TRIGGER = new CircuitCreateTrigger();

    // Criterion id, this needs to be put to the JSON file exactly as here
    public static final ResourceLocation ID = new ResourceLocation(String.format("%s:circuit_create", MCPCB.MOD_ID));

    private static String[] extractStringArray(@Nullable JsonElement element) {
        try {
            if (element != null) {
                JsonArray jsonArray = GsonHelper.convertToJsonArray(element, "ids");
                String[] ids = new String[jsonArray.size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = GsonHelper.convertToString(jsonArray.get(i), "id");
                }
                return ids;
            }
        } catch (JsonSyntaxException exception) {
            return new String[0];
        }

        return new String[0];
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject jsonObject, EntityPredicate.Composite entityPredicate, DeserializationContext _arrayParser) {
        return new Instance(entityPredicate, extractStringArray(jsonObject.get("ids")));
    }

    /**
     * This method should be used to grant the criteria.
     * This is an overload of {@link SimpleCriterionTrigger#trigger(ServerPlayer, Predicate)}.
     *
     * @param serverPlayer player who crafted the circuit
     * @param itemStack    crafted circuit item
     */
    public void trigger(ServerPlayer serverPlayer, ItemStack itemStack) {
        // Call the build-in trigger method
        this.trigger(serverPlayer, instance -> instance.matches(itemStack));
    }

    /**
     * Single criterion instance.
     * Contains optional array of strings containing known circuit ids to check.
     */
    public static class Instance extends AbstractCriterionTriggerInstance {
        private final String[] ids;

        public Instance(EntityPredicate.Composite p_i231597_1_, String[] ids) {
            super(CircuitCreateTrigger.ID, p_i231597_1_);
            this.ids = ids;
        }

        /**
         * Check if we should grant the criteria - if crafted circuit has any of the mentioned ids.
         *
         * @param itemStack circuit item stack
         * @return whether we should grant the criteria for a given {@code itemStack} or not
         */
        public boolean matches(ItemStack itemStack) {
            if (ids.length == 0) {
                return true;
            }

            CompoundTag tag = itemStack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("TruthTable", CompoundTag.TAG_COMPOUND)) {
                    TruthTable table = TruthTable.fromNBT(tag.getCompound("TruthTable"));
                    KnownTable recognized = table.recognize();

                    if (recognized != null) {
                        return Arrays.stream(ids).anyMatch(id -> recognized.getId().equals(id));
                    }
                }
            }


            return false;
        }
    }
}
