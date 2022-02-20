package tchojnacki.mcpcb.common.groups;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Main item group (creative tab).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MainGroup extends CreativeModeTab {
    public static final String ID = "mcpcb_main_tab";

    /**
     * Default constructor.
     */
    public MainGroup() {
        super(MainGroup.ID);
    }

    /**
     * Generates the icon of the tab (screwdriver item).
     *
     * @return item stack used as tab icon
     */
    @Override
    public ItemStack makeIcon() {
        return Registration.SCREWDRIVER_ITEM.get().getDefaultInstance();
    }
}
