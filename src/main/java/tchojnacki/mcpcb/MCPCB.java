package tchojnacki.mcpcb;

import net.minecraft.item.ItemGroup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import tchojnacki.mcpcb.common.groups.CircuitGroup;
import tchojnacki.mcpcb.common.groups.MainGroup;
import tchojnacki.mcpcb.util.ClientRegistration;
import tchojnacki.mcpcb.util.Registration;

/**
 * Entry point of the mod.
 *
 * MCPCB is a shortcut of mod's initial name - MineCraft Printed Circuit Boards.
 *
 * @author tchojnacki
 * @version 1.0.1
 */
@SuppressWarnings("unused")
@Mod(MCPCB.MOD_ID)
public class MCPCB {
    // TODO: Update to 1.17

    public static final String MOD_ID = "mcpcb";

    public static final ItemGroup MAIN_GROUP = new MainGroup();
    public static final ItemGroup CIRCUIT_GROUP = new CircuitGroup();

    public MCPCB() {
        Registration.register();

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientRegistration::register);
    }
}
