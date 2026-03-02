package io.github.moulberry.notenoughupdates.coffeeclient.feature.player;

import io.github.moulberry.notenoughupdates.mixins.IAccessorMinecraft;
import io.github.moulberry.notenoughupdates.coffeeclient.events.UpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.FloatProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RotationUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FastPlace extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private long delayMS = 0L;

    public final FloatProperty delay = new FloatProperty("delay", 1.0F, 1.0F, 3.0F);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty placeFix = new BooleanProperty("place-fix", true);

    public FastPlace() {
        super("FastPlace", false);
    }

    private boolean canPlace() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack != null) {
            Item item = stack.getItem();
            if (item instanceof ItemFishingRod) {
                return false;
            }
            if (item instanceof ItemBlock) {
                boolean isMoving = mc.thePlayer.motionX != 0 || mc.thePlayer.motionY != 0 || mc.thePlayer.motionZ != 0;
                if (isMoving || !placeFix.getValue()) {
                    return true;
                }
                MovingObjectPosition mop = RotationUtil.rayTrace(
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
                        mc.playerController.getBlockReachDistance(), 1.0F);
                return mop != null
                        && mop.typeOfHit == MovingObjectType.BLOCK
                        && ((ItemBlock) item).canPlaceBlockOnSide(mc.theWorld, mop.getBlockPos(), mop.sideHit,
                                mc.thePlayer, stack);
            }
        }
        return !blocksOnly.getValue();
    }

    @SubscribeEvent
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.isPre()) {
            int rightClickDelayTimer = ((IAccessorMinecraft) mc).getRightClickDelayTimer();

            if (rightClickDelayTimer == 4) {
                delayMS = delayMS + (long) (50.0F * delay.getValue());
            }
            if (delayMS > 0L) {
                delayMS = delayMS - 50;
            }

            if (delayMS <= 0L && rightClickDelayTimer > 1 && canPlace()) {
                ((IAccessorMinecraft) mc).setRightClickDelayTimer(0);
            }
        }
    }

    @Override
    public void onDisabled() {
        delayMS = 0L;
    }

    @Override
    public String[] getSuffix() {
        return new String[] { df.format(delay.getValue()) };
    }
}
