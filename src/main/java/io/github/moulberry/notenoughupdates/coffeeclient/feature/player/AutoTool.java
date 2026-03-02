package io.github.moulberry.notenoughupdates.coffeeclient.feature.player;

import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ItemUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoTool extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int currentToolSlot = -1;
    private int previousSlot = -1;
    private int tickDelayCounter = 0;

    public final IntProperty switchDelay = new IntProperty("delay", 0, 0, 5);
    public final BooleanProperty switchBack = new BooleanProperty("switch-back", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneak-only", true);

    public AutoTool() {
        super("AutoTool", false);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            if (currentToolSlot != -1 && currentToolSlot != mc.thePlayer.inventory.currentItem) {
                currentToolSlot = -1;
                previousSlot = -1;
            }
            if (mc.objectMouseOver != null
                    && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
                    && mc.gameSettings.keyBindAttack.isKeyDown()
                    && !mc.thePlayer.isUsingItem()) {
                if (tickDelayCounter >= switchDelay.getValue()
                        && (!sneakOnly.getValue() || mc.gameSettings.keyBindSneak.isKeyDown())) {
                    int slot = ItemUtil.findInventorySlot(
                            mc.thePlayer.inventory.currentItem,
                            mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock());
                    if (mc.thePlayer.inventory.currentItem != slot) {
                        if (previousSlot == -1) {
                            previousSlot = mc.thePlayer.inventory.currentItem;
                        }
                        mc.thePlayer.inventory.currentItem = currentToolSlot = slot;
                    }
                }
                tickDelayCounter++;
            } else {
                if (switchBack.getValue() && previousSlot != -1) {
                    mc.thePlayer.inventory.currentItem = previousSlot;
                }
                currentToolSlot = -1;
                previousSlot = -1;
                tickDelayCounter = 0;
            }
        }
    }

    @Override
    public void onDisabled() {
        currentToolSlot = -1;
        previousSlot = -1;
        tickDelayCounter = 0;
    }
}
