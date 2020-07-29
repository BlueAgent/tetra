package se.mickelus.tetra.items.modular.impl.toolbelt.gui;

import net.minecraft.util.ResourceLocation;
import se.mickelus.tetra.TetraMod;
import se.mickelus.mgui.gui.GuiAttachment;
import se.mickelus.mgui.gui.GuiElement;
import se.mickelus.mgui.gui.GuiRect;
import se.mickelus.mgui.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiColors;
import se.mickelus.tetra.items.modular.impl.toolbelt.SlotType;
import se.mickelus.tetra.module.ItemEffect;

import java.util.Collection;

public class GuiQuiverBackdrop extends GuiElement {
    private static final ResourceLocation texture = new ResourceLocation(TetraMod.MOD_ID, "textures/gui/toolbelt-inventory.png");
    public GuiQuiverBackdrop(int x, int y, int numSlots, Collection<Collection<ItemEffect>> inventoryEffects) {
        super(x, y, numSlots * 17 - 9, 28);

        setAttachmentPoint(GuiAttachment.topCenter);
        setAttachmentAnchor(GuiAttachment.topCenter);

        // background rects
        addChild(new GuiRect(0, 3, width, 22, 0xff000000));
        addChild(new GuiRect(0, 4, width, 20, GuiColors.muted));
        addChild(new GuiRect(0, 5, width, 18, 0xff000000));

        // left cap
        GuiTexture leftCap = new GuiTexture(0, 0, 16, 28, 96, 0, texture);
        leftCap.setAttachmentPoint(GuiAttachment.topRight);
        addChild(leftCap);

        GuiTexture rightCap = new GuiTexture(0, 0, 16, 28, 112, 0, texture);
        rightCap.setAttachmentPoint(GuiAttachment.topLeft);
        rightCap.setAttachmentAnchor(GuiAttachment.topRight);
        addChild(rightCap);

        GuiSlotEffect.getEffectsForInventory(SlotType.quiver, inventoryEffects).forEach(this::addChild);
    }
}
