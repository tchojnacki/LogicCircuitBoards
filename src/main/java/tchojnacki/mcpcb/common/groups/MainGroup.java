package tchojnacki.mcpcb.common.groups;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Main item group (creative tab).
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MainGroup extends ItemGroup {
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
