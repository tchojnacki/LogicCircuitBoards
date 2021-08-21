package tchojnacki.mcpcb.common.container;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tchojnacki.mcpcb.MCPCB;
import tchojnacki.mcpcb.common.block.CircuitBlock;
import tchojnacki.mcpcb.logic.TruthTable;
import tchojnacki.mcpcb.util.Registration;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Container used for circuit creation, opened by multimeter.
 *
 * @see tchojnacki.mcpcb.common.item.MultimeterItem
 * @see tchojnacki.mcpcb.client.screen.MultimeterContainerScreen
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultimeterContainer extends Container {
    public final static String ID = "multimeter_container";
    public final static ITextComponent TITLE = new TranslationTextComponent(String.format("container.%s.%s.title", MCPCB.MOD_ID, MultimeterContainer.ID));

    public final static int MAX_NAME_CHARS = 15;

    // Items that can go into each slot
    private final static ImmutableList<ImmutableSet<Item>> SLOT_ALLOWED_ITEMS = new ImmutableList.Builder<ImmutableSet<Item>>()
            .add(
                    new ImmutableSet.Builder<Item>()
                            .add(Items.TERRACOTTA)
                            .add(Items.WHITE_TERRACOTTA)
                            .add(Items.ORANGE_TERRACOTTA)
                            .add(Items.MAGENTA_TERRACOTTA)
                            .add(Items.LIGHT_BLUE_TERRACOTTA)
                            .add(Items.YELLOW_TERRACOTTA)
                            .add(Items.LIME_TERRACOTTA)
                            .add(Items.PINK_TERRACOTTA)
                            .add(Items.GRAY_TERRACOTTA)
                            .add(Items.LIGHT_GRAY_TERRACOTTA)
                            .add(Items.CYAN_TERRACOTTA)
                            .add(Items.PURPLE_TERRACOTTA)
                            .add(Items.BLUE_TERRACOTTA)
                            .add(Items.BROWN_TERRACOTTA)
                            .add(Items.GREEN_TERRACOTTA)
                            .add(Items.RED_TERRACOTTA)
                            .add(Items.BLACK_TERRACOTTA)
                            .build()
            )
            .add(ImmutableSet.of(Items.REDSTONE))
            .add(ImmutableSet.of(Items.REDSTONE_TORCH))
            .build();

    /**
     * {@link IntStream} over slot indices.
     *
     * @return item stream from 0 to 3 (exclusive)
     */
    public static IntStream inputRange() {
        return IntStream.range(0, 3);
    }

    /**
     * Create container for server side.
     *
     * @param windowId  window's id passed to superclass
     * @param playerInv player's inventory
     * @param access    world access
     * @param table     truth table for the circuit
     * @return server side container
     * @see tchojnacki.mcpcb.common.item.MultimeterItem#useOn(ItemUseContext)
     */
    public static MultimeterContainer createContainerServerSide(int windowId, PlayerInventory playerInv, IWorldPosCallable access, TruthTable table) {
        return new MultimeterContainer(windowId, playerInv, access, table);
    }

    /**
     * Create container for client side.
     *
     * @param windowId  window's id passed to superclass
     * @param playerInv player's inventory
     * @param extraData extra data - in this case containing the truth table
     * @return client side container
     * @see tchojnacki.mcpcb.common.item.MultimeterItem#useOn(ItemUseContext)
     */
    public static MultimeterContainer createContainerClientSide(int windowId, PlayerInventory playerInv, PacketBuffer extraData) {
        // Read truth table data from the packet
        CompoundNBT tableTag = extraData.readNbt();
        TruthTable truthTable = tableTag != null ? TruthTable.fromNBT(tableTag) : TruthTable.empty();

        return new MultimeterContainer(windowId, playerInv, IWorldPosCallable.NULL, truthTable);
    }

    private final IWorldPosCallable access;
    private final TruthTable truthTable;
    private final CircuitBlock circuitBlock;

    private final CraftResultInventory resultInventory = new CraftResultInventory();
    private final IInventory inputInventory = new Inventory(3) {
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    private final ImmutableList<Slot> inputSlotList;
    private final ImmutableList<IntReferenceHolder> costList;

    private String name = "";

    /**
     * Container's constructor, use appropriate static factory method instead.
     *
     * @see #createContainerServerSide(int, PlayerInventory, IWorldPosCallable, TruthTable)
     * @see #createContainerClientSide(int, PlayerInventory, PacketBuffer)
     */
    private MultimeterContainer(int id, PlayerInventory playerInv, IWorldPosCallable access, TruthTable table) {
        super(Registration.MULTIMETER_CONTAINER.get(), id);

        this.access = access;
        this.truthTable = table;
        this.circuitBlock = (CircuitBlock) Registration.CIRCUIT_BLOCK.get();

        // Add hotbar slots
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
        }

        // Add inventory slots
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Add container slots
        ImmutableList.Builder<Slot> builder = new ImmutableList.Builder<>();

        // Input slots
        inputRange().forEach(i -> builder.add(this.addSlot(new Slot(this.inputInventory, i, 68 + 20 * i, 58) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return SLOT_ALLOWED_ITEMS.get(i).contains(itemStack.getItem());
            }
        })));

        inputSlotList = builder.build();

        // Output slot
        this.addSlot(new Slot(this.resultInventory, 3, 152, 58) {
            @Override
            public boolean mayPlace(ItemStack _itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(PlayerEntity playerEntity) {
                return canCraft();
            }

            @Override
            public ItemStack onTake(PlayerEntity playerEntity, ItemStack itemStack) {
                itemStack.onCraftedBy(playerEntity.level, playerEntity, itemStack.getCount());

                inputRange().forEach(i -> inputSlotList.get(i).remove(costList.get(i).get()));
                createResult();

                return super.onTake(playerEntity, itemStack);
            }
        });

        // Cost list synchronizing them between server and client side
        costList = new ImmutableList.Builder<IntReferenceHolder>()
                .add(IntReferenceHolder.standalone()) // terracotta
                .add(IntReferenceHolder.standalone()) // dust
                .add(IntReferenceHolder.standalone()) // torch
                .build();

        TruthTable.CircuitCosts circuitCosts = table.calculateCost();

        if (playerInv.player.abilities.instabuild) {
            inputRange().forEach(i -> costList.get(i).set(0));
        } else {
            costList.get(0).set(circuitCosts.terracotta);
            costList.get(1).set(circuitCosts.dust);
            costList.get(2).set(circuitCosts.torches);
        }

        for (IntReferenceHolder holder : costList) {
            this.addDataSlot(holder);
        }

        createResult();
    }

    /**
     * Whether the circuit item is craftable using current inputs.
     * Note that all costs are equal zero in creative mode.
     *
     * @return if we can craft a circuit item
     */
    private boolean canCraft() {
        return inputRange()
                .map(i -> {
                    int available = inputSlotList.get(i).getItem().getCount();
                    int cost = costList.get(i).get();
                    return cost > 0
                            ? Math.floorDiv(available, cost)
                            : 64; // max stack size
                })
                .min()
                .orElse(0) > 0; // craftable count is bigger than zero
    }

    /**
     * Refresh the output slot.
     * Put a circuit if it is currently craftable and an empty stack otherwise.
     * Called on when input slots are modified or name is changed.
     *
     * @see CircuitBlock#stackFromTable(TruthTable)
     */
    public void createResult() {
        if (canCraft()) {
            ItemStack result = circuitBlock.stackFromTable(truthTable);

            // Empty string is used to mark default name
            if (!name.isEmpty()) {
                result.setHoverName(new StringTextComponent(name));
            }

            this.resultInventory.setItem(0, result);
        } else {
            this.resultInventory.setItem(0, ItemStack.EMPTY);
        }
    }

    public void setName(String newName) {
        this.name = newName;
        createResult();
    }

    /**
     * If container is still valid - true.
     *
     * @param _player unused
     * @return always true
     */
    @Override
    public boolean stillValid(PlayerEntity _player) {
        // TODO: Possibly check distance from container and others like in vanilla's containers
        return true;
    }

    /**
     * Called when container is destroyed, return the items to the player.
     *
     * @param player player using the container
     */
    @Override
    public void removed(PlayerEntity player) {
        super.removed(player);
        this.access.execute((world, blockPos) -> this.clearContainer(player, world, this.inputInventory));
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        super.slotsChanged(inventory);

        // Refresh output on input change
        if (inventory == this.inputInventory) {
            this.createResult();
        }
    }

    /**
     * Handle quick moving (shift-clicking) of stacks.
     * Note that shift-clicking the output stack in creative mode will fill the entire inventory.
     * It is fully consistent with vanilla's way of doing things - take as many item as possible
     * (because here you can take infinitely many).
     *
     * @param playerEntity  player moving the stack
     * @param sourceSlotIdx index of the shift-clicked slot
     * @return {@link ItemStack#EMPTY} on failure or copy of original stack on success
     */
    @Override
    public ItemStack quickMoveStack(PlayerEntity playerEntity, int sourceSlotIdx) {
        // Based on: https://github.com/TheGreyGhost/MinecraftByExample/blob/e9862e606f6306463fccde5e3ebe576ea88f0745/src/main/java/minecraftbyexample/mbe30_inventory_basic/ContainerBasic.java#L139

        Slot sourceSlot = slots.get(sourceSlotIdx);

        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (sourceSlotIdx < 36) { // Inventory
            boolean lastMoveResult = false;
            for (int i = 0; i < 3; i++) {
                if (SLOT_ALLOWED_ITEMS.get(i).contains(sourceStack.getItem())) {
                    lastMoveResult = moveItemStackTo(sourceStack, 36 + i, 36 + i + 1, false);
                    if (lastMoveResult) {
                        break;
                    }
                }
            }

            if (!lastMoveResult) {
                return ItemStack.EMPTY;
            }
        } else if (sourceSlotIdx < 40) { // Input & output
            // Try moving to inventory
            if (!moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else { // Invalid - should never reach this
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerEntity, copyOfSourceStack);
        return copyOfSourceStack;
    }

    /**
     * Pass input slot list to the screen.
     * Client side only.
     *
     * @return {@link #inputSlotList}
     */
    @OnlyIn(Dist.CLIENT)
    public ImmutableList<Slot> getInputSlotList() {
        return inputSlotList;
    }

    /**
     * Pass a copy of cost list to the screen.
     * Client side only.
     *
     * @return copy of {@link #costList}
     */
    @OnlyIn(Dist.CLIENT)
    public ImmutableList<Integer> getCostArray() {
        return ImmutableList.copyOf(costList.stream().map(IntReferenceHolder::get).collect(Collectors.toList()));
    }

    /**
     * Make it possible to get truth table's item stack from screen.
     * Client side only.
     *
     * @return item stack of circuit craftable in this container
     * @see CircuitBlock#stackFromTable(TruthTable)
     */
    @OnlyIn(Dist.CLIENT)
    public ItemStack getCircuitStack() {
        return circuitBlock.stackFromTable(truthTable);
    }
}
