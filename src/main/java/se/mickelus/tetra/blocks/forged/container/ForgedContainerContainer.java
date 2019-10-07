package se.mickelus.tetra.blocks.forged.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import se.mickelus.tetra.gui.ToggleableSlot;
import se.mickelus.tetra.network.PacketHandler;

import java.util.Arrays;


public class ForgedContainerContainer extends Container {
    private TileEntityForgedContainer tileEntity;
    private IInventory playerInventory;

    private ToggleableSlot[][] compartmentSlots;
    private int currentCompartment = 0;

    public ForgedContainerContainer(int windowId,TileEntityForgedContainer tileEntity, IInventory playerInventory, PlayerEntity player) {
        super(BlockForgedContainer.containerType, windowId);
        this.tileEntity = tileEntity;
        tileEntity.openInventory(player);

        this.playerInventory = playerInventory;

        compartmentSlots = new ToggleableSlot[TileEntityForgedContainer.compartmentCount][];
        for (int i = 0; i < compartmentSlots.length; i++) {
            compartmentSlots[i] = new ToggleableSlot[TileEntityForgedContainer.compartmentSize];
            int offset = i * TileEntityForgedContainer.compartmentSize;
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 9; k++) {
                    int index = j * 9 + k;
                    compartmentSlots[i][index] = new ToggleableSlot(tileEntity, index + offset, k * 17 + 12, j * 17);
                    compartmentSlots[i][index].toggle(i == 0);
                    this.addSlotToContainer(compartmentSlots[i][index]);
                }
            }
        }

        // player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(playerInventory, i * 9 + j + 9, j * 17 + 12, i * 17 + 116));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(playerInventory, i, i * 17 + 12, 171));
        }
    }

    public void changeCompartment(int compartmentIndex) {
        currentCompartment = compartmentIndex;
        for (int i = 0; i < compartmentSlots.length; i++) {
            boolean enabled = i == compartmentIndex;
            Arrays.stream(compartmentSlots[i]).forEach(slot -> slot.toggle(enabled));
        }

        if (tileEntity.getWorld().isRemote) {
            PacketHandler.sendToServer(new ChangeCompartmentPacket(compartmentIndex));
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return tileEntity.isUsableByPlayer(playerIn);
    }

    /**
     * Take a stack from the specified inventory slot.
     */
    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index) {
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemStack = slot.getStack();

            if (index < tileEntity.getSizeInventory()) {
                if (!mergeItemStack(itemStack,  tileEntity.getSizeInventory(), inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(itemStack, currentCompartment * TileEntityForgedContainer.compartmentSize,
                    ( currentCompartment + 1) * TileEntityForgedContainer.compartmentSize, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            return itemStack.copy();
        }

        return ItemStack.EMPTY;
    }

    /**
     * Called when the container is closed.
     */
    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);

        tileEntity.closeInventory(playerIn);
    }

    public TileEntityForgedContainer getTileEntity() {
        return tileEntity;
    }
}