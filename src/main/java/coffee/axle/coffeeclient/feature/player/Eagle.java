package coffee.axle.coffeeclient.feature.player;

import coffee.axle.coffeeclient.events.MoveInputEvent;
import coffee.axle.coffeeclient.events.RightClickMouseEvent;
import coffee.axle.coffeeclient.events.UpdateEvent;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.util.ItemUtil;
import coffee.axle.coffeeclient.util.MoveUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.RandomUtils;

import java.util.Objects;

public class Eagle extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int sneakDelay = 0;

    public final IntProperty minDelay = new IntProperty("min-delay", 2, 0, 10);
    public final IntProperty maxDelay = new IntProperty("max-delay", 3, 0, 10);
    public final IntProperty offset = new IntProperty("offset", 100, 0, 100);
    public final BooleanProperty align = new BooleanProperty("align", true);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);

    public Eagle() {
        super("Eagle", false);
    }

    private boolean isNearEdge() {
        double[] movement = MoveUtil.predictMovement();
        float widthFactor = this.offset.getValue() / 100.0f;
        double horizontalInset = mc.thePlayer.width / -2.0f * (1.0f - widthFactor);
        AxisAlignedBB bounds = mc.thePlayer.getEntityBoundingBox()
                .offset(mc.thePlayer.motionX + movement[0], -1.0, mc.thePlayer.motionZ + movement[1])
                .expand(horizontalInset, 0.0, horizontalInset);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bounds).isEmpty();
    }

    private boolean shouldSneak() {
        if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.pitchCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else {
            return (!this.blocksOnly.getValue() || ItemUtil.isHoldingBlock()) && mc.thePlayer.onGround;
        }
    }

    @SubscribeEvent
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled())
            return;

        if (event.isPre()) {
            if (this.sneakDelay > 0) {
                this.sneakDelay--;
            }
            if (this.sneakDelay != 0) {
                return;
            }
            if (this.isNearEdge()) {
                this.sneakDelay = RandomUtils.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
        }
    }

    @SubscribeEvent
    public void onRightClick(RightClickMouseEvent event) {
        if (!isEnabled()
                || mc.currentScreen != null
                || !this.align.getValue()
                || mc.gameSettings.keyBindSneak.isKeyDown()
                || !this.shouldSneak()) {
            return;
        }

        if (this.sneakDelay <= 0 && !this.isNearEdge()) {
            return;
        }

        float movementYaw = (float) Math.toRadians(
                MoveUtil.adjustYaw(mc.thePlayer.rotationYaw, MoveUtil.getForwardValue(), MoveUtil.getLeftValue())
                        - 180.0f);
        float xDirection = -MathHelper.sin(movementYaw);
        float zDirection = MathHelper.cos(movementYaw);

        if (Math.abs(xDirection) < 0.1f || Math.abs(zDirection) < 0.1f) {
            return;
        }

        AxisAlignedBB bounds = mc.thePlayer.getEntityBoundingBox();
        double xEdge = xDirection >= 0.0f ? bounds.maxX : bounds.minX;
        double zEdge = zDirection >= 0.0f ? bounds.maxZ : bounds.minZ;
        double xAlignment = Math.abs(xEdge - Math.rint(xEdge));
        double zAlignment = Math.abs(zEdge - Math.rint(zEdge));

        if (Math.abs(xAlignment - zAlignment) > 0.125) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled() || mc.currentScreen != null || mc.thePlayer.movementInput.sneak
                || !this.shouldSneak()) {
            return;
        }

        if (this.sneakDelay <= 0 && !this.isNearEdge()) {
            return;
        }

        mc.thePlayer.movementInput.sneak = true;
        mc.thePlayer.movementInput.moveStrafe *= 0.3F;
        mc.thePlayer.movementInput.moveForward *= 0.3F;
    }

    @Override
    public void onDisabled() {
        this.sneakDelay = 0;
    }

    @Override
    public void verifyValue(String name) {
        switch (name) {
            case "min-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.maxDelay.setValue(this.minDelay.getValue());
                }
                break;
            case "max-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.minDelay.setValue(this.maxDelay.getValue());
                }
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())
                ? new String[] { this.minDelay.getValue().toString() }
                : new String[] { String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue()) };
    }
}
