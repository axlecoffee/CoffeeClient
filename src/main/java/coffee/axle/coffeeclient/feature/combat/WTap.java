package coffee.axle.coffeeclient.feature.combat;

import coffee.axle.coffeeclient.events.MoveInputEvent;
import coffee.axle.coffeeclient.events.PacketEvent;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.util.TimerUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WTap extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private int chanceAccumulator = 0;
    private boolean active = false;
    private boolean stopForward = false;
    private long delayTicks = 0L;
    private long durationTicks = 0L;

    public final FloatProperty delay = new FloatProperty("delay", 5.5f, 0.0f, 10.0f);
    public final FloatProperty duration = new FloatProperty("duration", 1.5f, 1.0f, 5.0f);
    public final IntProperty chance = new IntProperty("chance", 100, 0, 100);

    public WTap() {
        super("WTap", false);
    }

    private boolean canTrigger() {
        if (mc.thePlayer.movementInput.moveForward < 0.8f || mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        if ((float) mc.thePlayer.getFoodStats().getFoodLevel() <= 6.0f
                && !mc.thePlayer.capabilities.allowFlying) {
            return false;
        }
        if (mc.thePlayer.isSneaking()) {
            return true;
        }
        if (mc.thePlayer.isUsingItem() || mc.thePlayer.isPotionActive(Potion.blindness)) {
            return false;
        }
        return mc.gameSettings.keyBindSprint.isKeyDown();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (active) {
            if (!stopForward && !canTrigger()) {
                active = false;
                while (delayTicks > 0L) {
                    delayTicks -= 50L;
                }
                while (durationTicks > 0L) {
                    durationTicks -= 50L;
                }
            } else if (delayTicks > 0L) {
                delayTicks -= 50L;
            } else {
                if (durationTicks > 0L) {
                    durationTicks -= 50L;
                    stopForward = true;
                    mc.thePlayer.movementInput.moveForward = 0.0f;
                }
                if (durationTicks <= 0L) {
                    active = false;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.isCanceled()) {
            return;
        }

        if (event.isSend() && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == Action.ATTACK
                    && !active
                    && timer.hasTimeElapsed(500L)
                    && mc.thePlayer.isSprinting()) {
                timer.reset();
                this.chanceAccumulator = this.chanceAccumulator % 100 + this.chance.getValue();
                if (this.chanceAccumulator < 100) {
                    return;
                }
                active = true;
                stopForward = false;
                delayTicks = delayTicks + (long) (50.0f * delay.getValue());
                durationTicks = durationTicks + (long) (50.0f * duration.getValue());
            }
        }
    }
}
