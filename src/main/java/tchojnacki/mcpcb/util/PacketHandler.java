package tchojnacki.mcpcb.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import tchojnacki.mcpcb.MCPCB;

/**
 * Packet handler responsible for sending and receiving {@link MultimeterScreenRenamePacket}.
 * Messages registered in {@link Registration#register()}.
 *
 * @see <a href="https://mcforge.readthedocs.io/en/1.16.x/networking/simpleimpl/#simpleimpl">SimpleImpl - Forge Documentation</a>
 */
public final class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MCPCB.MOD_ID, "multimeter_screen_rename"), // channel used only for multimeter screen circuit rename
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private PacketHandler() {
    }
}
