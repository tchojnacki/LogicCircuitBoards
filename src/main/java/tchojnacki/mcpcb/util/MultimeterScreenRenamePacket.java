package tchojnacki.mcpcb.util;

import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import tchojnacki.mcpcb.common.container.MultimeterContainer;

import java.util.function.Supplier;

/**
 * Circuit name gets customized on client-side which means it needs to get sent to server-side
 * using network packets (even if we are in a singleplayer world).
 */
public record MultimeterScreenRenamePacket(String newName) {
    public static MultimeterScreenRenamePacket decode(FriendlyByteBuf buffer) {
        return new MultimeterScreenRenamePacket(
                // Don't use the argumentless overload, it is defined only for client
                buffer.readUtf(32767)
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.newName);
    }

    /**
     * Handle packet received on server-side - update the name in the container.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();

            if (sender != null && sender.containerMenu instanceof MultimeterContainer multimeterContainer) {
                // Don't ever trust the client, check the new name
                String filteredName = SharedConstants.filterText(this.newName);
                if (filteredName.length() <= MultimeterContainer.MAX_NAME_CHARS) {
                    multimeterContainer.setName(filteredName);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
