package coffee.axle.coffeeclient.feature.combat;

import coffee.axle.coffeeclient.events.KnockbackEvent;
import coffee.axle.coffeeclient.events.LivingUpdateEvent;
import coffee.axle.coffeeclient.events.PacketEvent;
import coffee.axle.coffeeclient.events.UpdateEvent;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.property.properties.ModeProperty;
import coffee.axle.coffeeclient.util.MoveUtil;
import coffee.axle.coffeeclient.mixin.IAccessorEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Velocity extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean reversePending = false;
    private boolean bufferingVelocity = false;
    private S12PacketEntityVelocity delayedVelocity = null;
    private int delayTickCounter = 0;

    private boolean shouldJump = false;
    private int jumpCooldown = 0;

    public final ModeProperty mode = new ModeProperty("mode", 0,
            new String[] { "VANILLA", "JUMP", "DELAY", "REVERSE", "LEGITTest" });
    public final IntProperty delayTicksProperty = new IntProperty("delay-ticks", 3, 1, 20, this::showDelaySettings);
    public final IntProperty delayChance = new IntProperty("delay-chance", 100, 0, 100, this::showDelaySettings);
    public final IntProperty chance = new IntProperty("chance", 100, 0, 100);
    public final IntProperty horizontal = new IntProperty("horizontal", 0, 0, 100);
    public final IntProperty vertical = new IntProperty("vertical", 100, 0, 100);
    public final IntProperty explosionHorizontal = new IntProperty("explosions-horizontal", 100, 0, 100);
    public final IntProperty explosionVertical = new IntProperty("explosions-vertical", 100, 0, 100);
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true);
    public final BooleanProperty debugLog = new BooleanProperty("debug-log", false);

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava()
                || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean showDelaySettings() {
        return mode.getValue() == 2;
    }

    private void releaseDelayedVelocity() {
        this.bufferingVelocity = false;
        this.delayTickCounter = 0;
        S12PacketEntityVelocity packet = this.delayedVelocity;
        this.delayedVelocity = null;
        if (packet != null && mc.getNetHandler() != null) {
            try {
                packet.processPacket(mc.getNetHandler());
            } catch (ThreadQuickExitException ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onKnockback(KnockbackEvent event) {
        if (!isEnabled() || event.isCanceled()) {
            this.pendingExplosion = false;
            this.allowNext = true;
        } else if (!this.allowNext || !this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                if (this.explosionHorizontal.getValue() > 0) {
                    event.setX(event.getX() * (double) this.explosionHorizontal.getValue() / 100.0);
                    event.setZ(event.getZ() * (double) this.explosionHorizontal.getValue() / 100.0);
                } else {
                    event.setX(mc.thePlayer.motionX);
                    event.setZ(mc.thePlayer.motionZ);
                }
                if (this.explosionVertical.getValue() > 0) {
                    event.setY(event.getY() * (double) this.explosionVertical.getValue() / 100.0);
                } else {
                    event.setY(mc.thePlayer.motionY);
                }
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    this.jumpFlag = (this.mode.getValue() == 1 || this.mode.getValue() == 2) && event.getY() > 0.0;
                    this.reversePending = this.mode.getValue() == 3;
                    if (this.horizontal.getValue() > 0) {
                        event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                        event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
                    } else {
                        event.setX(mc.thePlayer.motionX);
                        event.setZ(mc.thePlayer.motionZ);
                    }
                    if (this.vertical.getValue() > 0) {
                        event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
                    } else {
                        event.setY(mc.thePlayer.motionY);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onUpdate(UpdateEvent event) {
        if (!event.isPost()) return;

        if (this.bufferingVelocity && (mc.thePlayer.onGround || this.isInLiquidOrWeb()
                || this.delayTickCounter >= this.delayTicksProperty.getValue())) {
            this.releaseDelayedVelocity();
        }
        if (this.reversePending) {
            MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
            this.reversePending = false;
        }

        if (this.mode.getValue() == 4) {
            int hurtTime = mc.thePlayer.hurtTime;
            if (hurtTime >= 8) {
                if (jumpCooldown <= 0) {
                    shouldJump = true;
                    jumpCooldown = 2;
                }
            } else if (hurtTime <= 1) {
                shouldJump = false;
                jumpCooldown = 0;
            }

            if (shouldJump && mc.thePlayer.onGround && jumpCooldown <= 0) {
                mc.thePlayer.jump();
                shouldJump = false;
            }

            if (jumpCooldown > 0) {
                jumpCooldown--;
            }
        }

        if (this.bufferingVelocity) {
            this.delayTickCounter++;
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump)
                    && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || !event.isReceive() || event.isCanceled())
            return;

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                if (this.bufferingVelocity) {
                    this.releaseDelayedVelocity();
                }
                if (this.mode.getValue() == 2
                        && !this.bufferingVelocity
                        && !mc.thePlayer.onGround
                        && !this.isInLiquidOrWeb()
                        && !this.pendingExplosion
                        && (!this.allowNext || !this.fakeCheck.getValue())) {
                    this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                    if (this.delayChanceCounter >= 100) {
                        event.setCanceled(true);
                        this.delayedVelocity = packet;
                        this.bufferingVelocity = true;
                        this.delayTickCounter = 0;
                        return;
                    }
                }
                if (this.debugLog.getValue()) {
                    System.out.println(String.format("Velocity (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                            mc.thePlayer.ticksExisted,
                            (double) packet.getMotionX() / 8000.0,
                            (double) packet.getMotionY() / 8000.0,
                            (double) packet.getMotionZ() / 8000.0));
                }
            }
        } else if (event.getPacket() instanceof S27PacketExplosion) {
            
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                    event.setCanceled(true);
                }
                if (this.debugLog.getValue()) {
                    System.out.println(String.format("Explosion (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                            mc.thePlayer.ticksExisted,
                            mc.thePlayer.motionX + (double) packet.func_149149_c(),
                            mc.thePlayer.motionY + (double) packet.func_149144_d(),
                            mc.thePlayer.motionZ + (double) packet.func_149147_e()));
                }
            }
        } else if (event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
            Entity entity = packet.getEntity(mc.theWorld);
            if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                this.allowNext = false;
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.bufferingVelocity) {
            this.releaseDelayedVelocity();
        }
        this.pendingExplosion = false;
        this.allowNext = true;
        this.shouldJump = false;
        this.jumpCooldown = 0;
        this.delayTickCounter = 0;
        this.reversePending = false;
        this.jumpFlag = false;
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString().toUpperCase();
        return new String[] { modeName };
    }
}
