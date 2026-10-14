package coffee.axle.coffeeclient.feature.combat;

import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.property.properties.ModeProperty;
import coffee.axle.coffeeclient.util.ItemUtil;
import coffee.axle.coffeeclient.util.MoveUtil;
import coffee.axle.coffeeclient.util.PacketUtil;
import coffee.axle.coffeeclient.util.PlayerUtil;
import coffee.axle.coffeeclient.util.RenderUtil;
import coffee.axle.coffeeclient.util.RotationUtil;
import coffee.axle.coffeeclient.util.TeamUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AntiFireball extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ArrayList<EntityFireball> farList = new ArrayList<>();
    private final ArrayList<EntityFireball> nearList = new ArrayList<>();
    private EntityFireball target = null;

    public final FloatProperty range = new FloatProperty("range", 5.0F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 360, 1, 360);
    public final BooleanProperty rotations = new BooleanProperty("rotations", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});

    public AntiFireball() {
        super("AntiFireball", false);
    }

    private boolean isValidTarget(EntityFireball entityFireball) {
        return !entityFireball.getEntityBoundingBox().hasNaN()
                && RotationUtil.distanceToEntity(entityFireball) <= (double) this.range.getValue() + 3.0
                && RotationUtil.angleToEntity(entityFireball) <= (float) this.fov.getValue();
    }

    private void doAttackAnimation() {
        if (this.swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            PacketUtil.sendPacket(new C0APacketAnimation());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        List<EntityFireball> fireballs = mc.theWorld
                .loadedEntityList
                .stream()
                .filter(entity -> entity instanceof EntityFireball)
                .map(entity -> (EntityFireball) entity)
                .collect(Collectors.toList());

        this.farList.removeIf(entityFireball -> !fireballs.contains(entityFireball));
        this.nearList.removeIf(entityFireball -> !fireballs.contains(entityFireball));

        for (EntityFireball fireball : fireballs) {
            if (!this.farList.contains(fireball) && !this.nearList.contains(fireball)) {
                if (RotationUtil.distanceToEntity(fireball) > 3.0) {
                    this.farList.add(fireball);
                } else {
                    this.nearList.add(fireball);
                }
            }
        }

        if (mc.thePlayer.capabilities.allowFlying) {
            this.target = null;
        } else {
            this.target = this.farList.stream()
                    .filter(this::isValidTarget)
                    .min(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                    .orElse(null);
        }
    }

    @SubscribeEvent
    public void onClientTickPost(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityFireball fireball = this.target;
        if (TeamUtil.isEntityLoaded(fireball)) {
            float[] rots = RotationUtil.getRotationsToBox(
                    fireball.getEntityBoundingBox(),
                    mc.thePlayer.rotationYaw,
                    mc.thePlayer.rotationPitch,
                    180.0F,
                    0.0F);

            if (this.rotations.getValue()
                    && !ItemUtil.isHoldingNonEmpty()
                    && !ItemUtil.isUsingBow()
                    && !ItemUtil.hasHoldItem()) {
                mc.thePlayer.rotationYaw = rots[0];
                mc.thePlayer.rotationPitch = rots[1];
            }

            if (!PlayerUtil.isAttacking() && !PlayerUtil.isUsingItem()) {
                this.doAttackAnimation();
                if (RotationUtil.distanceToEntity(this.target) <= (double) this.range.getValue()) {
                    PacketUtil.sendPacket(new C02PacketUseEntity(this.target, Action.ATTACK));
                    PlayerUtil.attackEntity(this.target);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (this.showTarget.getValue() != 0 && TeamUtil.isEntityLoaded(this.target)) {
            Color color = new Color(-1);
            switch (this.showTarget.getValue()) {
                case 1:
                    double dist = (this.target.posX - this.target.lastTickPosX) * (mc.thePlayer.posX - this.target.posX)
                            + (this.target.posY - this.target.lastTickPosY)
                            * (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - this.target.posY - (double) this.target.height / 2.0)
                            + (this.target.posZ - this.target.lastTickPosZ) * (mc.thePlayer.posZ - this.target.posZ);
                    if (dist < 0.0) {
                        color = new Color(16733525);
                    } else {
                        color = new Color(5635925);
                    }
                    break;
                case 2:
                    color = new Color(0x55FFFF);
                    break;
            }

            RenderUtil.enableRenderState();
            float partialTicks = event.partialTicks;
            double x = RenderUtil.interpolate(this.target.posX, this.target.lastTickPosX, partialTicks);
            double y = RenderUtil.interpolate(this.target.posY, this.target.lastTickPosY, partialTicks);
            double z = RenderUtil.interpolate(this.target.posZ, this.target.lastTickPosZ, partialTicks);
            AxisAlignedBB box = this.target.getEntityBoundingBox()
                    .offset(x - this.target.posX, y - this.target.posY, z - this.target.posZ)
                    .offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtil.drawFilledBox(box, color.getRed(), color.getGreen(), color.getBlue());
            RenderUtil.drawBoundingBox(box, color.getRed(), color.getGreen(), color.getBlue(), 255, 2.0F);
            RenderUtil.disableRenderState();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.farList.clear();
        this.nearList.clear();
    }
}
