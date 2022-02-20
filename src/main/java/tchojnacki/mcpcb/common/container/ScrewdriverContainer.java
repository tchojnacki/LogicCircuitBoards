package tchojnacki.mcpcb.common.container;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.item.ScrewdriverItem;
import tchojnacki.mcpcb.logic.BoardManager;
import tchojnacki.mcpcb.logic.BoardSocket;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Objects;

/**
 * Container used for board configuration, opened by screwdriver.
 *
 * @see ScrewdriverItem
 * @see tchojnacki.mcpcb.client.screen.ScrewdriverContainerScreen
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ScrewdriverContainer extends AbstractContainerMenu {
    public final static String ID = "screwdriver_container";
    public final static Component TITLE = new TranslatableComponent(String.format("container.%s.%s.title", MCPCB.MOD_ID, ScrewdriverContainer.ID));

    private final @Nullable
    BoardManager boardManager;

    private final ImmutableMap<Direction, DataSlot> STATE_NUMBERS;

    /**
     * Create container for server side.
     *
     * @param windowId     window's id passed to superclass
     * @param boardManager board manager for a given breadboard
     * @return server side container
     * @see ScrewdriverItem#useOn(UseOnContext)
     */
    public static ScrewdriverContainer createContainerServerSide(int windowId, BoardManager boardManager) {
        return new ScrewdriverContainer(windowId, boardManager);
    }

    /**
     * Create container for client side.
     *
     * @param windowId   window's id passed to superclass
     * @param _playerInv unused
     * @param _extraData unused
     * @return client side container
     * @see ScrewdriverItem#useOn(UseOnContext)
     */
    @SuppressWarnings("unused")
    // function's signature should look like that for consistency, yet two parameters are unused
    public static ScrewdriverContainer createContainerClientSide(int windowId, Inventory _playerInv, FriendlyByteBuf _extraData) {
        return new ScrewdriverContainer(windowId, null);
    }

    /**
     * Container's constructor, use appropriate static factory method instead.
     *
     * @see #createContainerServerSide(int, BoardManager)
     * @see #createContainerClientSide(int, Inventory, FriendlyByteBuf)
     */
    private ScrewdriverContainer(int id, @Nullable BoardManager boardManager) {
        super(Registration.SCREWDRIVER_CONTAINER.get(), id);

        this.boardManager = boardManager;

        final HashMap<Direction, DataSlot> stateNumbers = new HashMap<>();

        Direction.Plane.HORIZONTAL.forEach(direction -> {
            stateNumbers.put(direction, DataSlot.standalone());
            if (boardManager != null) {
                stateNumbers.get(direction).set(boardManager.getSocket(direction).getState().getNumber());
            }
            this.addDataSlot(stateNumbers.get(direction));
        });

        this.STATE_NUMBERS = ImmutableMap.copyOf(stateNumbers);
    }

    /**
     * Pass socket state to the screen.
     * Client side only.
     *
     * @param direction direction for which to get the state
     * @return state of socket at {@code direction}
     */
    @OnlyIn(Dist.CLIENT)
    public int getSocketStateNumber(Direction direction) {
        return Objects.requireNonNull(STATE_NUMBERS.get(direction)).get();
    }

    /**
     * Return the direction of a button with given id.
     *
     * @param buttonId button's id - 0, 1, 2 or 3
     * @return direction of button specified with {@code buttonId}
     * @throws IllegalArgumentException when incorrect {@code buttonId} is passed
     */
    private Direction buttonToDirection(int buttonId) throws IllegalArgumentException {
        return switch (buttonId) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> throw new IllegalArgumentException("Incorrect button id.");
        };
    }

    /**
     * Called from screen when player clicks a button.
     *
     * @param player   player that clicked the button
     * @param buttonId button's id (0, 1, 2 or 3)
     * @return if the event was handled - true
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (boardManager != null) {
            // Cycle state to next
            Direction direction = buttonToDirection(buttonId);
            Objects.requireNonNull(STATE_NUMBERS.get(direction)).set((Objects.requireNonNull(STATE_NUMBERS.get(direction)).get() + 1) % 3);
            boardManager.setSideState(direction, BoardSocket.State.fromNumber(Objects.requireNonNull(STATE_NUMBERS.get(direction)).get()));
        }

        return true;
    }

    /**
     * If container is still valid - true.
     *
     * @param _player unused
     * @return always true
     */
    @Override
    public boolean stillValid(Player _player) {
        // TODO: Possibly check distance from container and others like in vanilla's containers
        return true;
    }
}
