/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.feature.combat;

import com.google.common.base.CaseFormat;
import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.events.LeftClickMouseEvent;
import coffee.axle.coffeeclient.events.MoveInputEvent;
import coffee.axle.coffeeclient.events.PacketEvent;
import coffee.axle.coffeeclient.events.UpdateEvent;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.feature.render.HUD;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.property.properties.ModeProperty;
import coffee.axle.coffeeclient.rotation.KillAuraRotation;
import coffee.axle.coffeeclient.rotation.RotationState;
import coffee.axle.coffeeclient.util.*;
import coffee.axle.coffeeclient.mixin.IAccessorC03PacketPlayer;
import coffee.axle.coffeeclient.mixin.IAccessorPlayerControllerMP;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

/**
 * KillAura combat feature.
 * <p>
 * Based on OpenMyau KillAura by 60124808866.
 */
public class KillAura extends Feature {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));

    private final TimerUtil targetSwitchTimer = new TimerUtil();
    private Target currentTarget;
    private boolean hasTargetInAutoBlockRange;
    private int targetIndex;
    private boolean attackedThisUpdate;

    private boolean serverBlocking;
    private boolean autoBlockActive;
    private boolean renderBlocking;
    private long attackCooldownMillis;
    private long blockHoldMillis;
    private int autoBlockPhase;
    private int lastDamageLogTick;

    private final RotationState rotState = RotationState.getInstance();
    private final KillAuraRotation rotController = new KillAuraRotation(rotState);

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[] { "SINGLE", "SWITCH" });
    public final ModeProperty sort = new ModeProperty("sort", 0,
            new String[] { "DISTANCE", "HEALTH", "HURT_TIME", "FOV" });
    public final ModeProperty autoBlock = new ModeProperty("auto-block", 1, new String[] { "NONE", "LEGIT" });
    public final BooleanProperty autoBlockRequirePress = new BooleanProperty("auto-block-require-press", true,
            this::isAutoBlockSettingsVisible);
    public final FloatProperty autoBlockMinCPS = new FloatProperty("auto-block-min-aps", 8.0f, 1.0f, 20.0f,
            this::isAutoBlockSettingsVisible);
    public final FloatProperty autoBlockMaxCPS = new FloatProperty("auto-block-max-aps", 10.0f, 1.0f, 20.0f,
            this::isAutoBlockSettingsVisible);
    public final FloatProperty autoBlockRange = new FloatProperty("auto-block-range", 4.0f, 3.0f, 8.0f,
            this::isAutoBlockSettingsVisible);
    public final FloatProperty swingRange = new FloatProperty("swing-range", 4.0f, 3.0f, 6.0f);
    public final FloatProperty attackRange = new FloatProperty("attack-range", 3.0f, 3.0f, 6.0f);
    public final IntProperty fov = new IntProperty("fov", 360, 30, 360);
    public final IntProperty minCPS = new IntProperty("min-aps", 14, 1, 20);
    public final IntProperty maxCPS = new IntProperty("max-aps", 14, 1, 20);
    public final IntProperty switchDelay = new IntProperty("switch-delay", 150, 0, 1000);
    public final ModeProperty rotations = new ModeProperty("rotations", 2,
            new String[] { "NONE", "LEGIT", "SILENT", "LOCK_VIEW" });
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[] { "NONE", "SILENT", "STRICT" });
    public final IntProperty smoothing = new IntProperty("smoothing", 0, 0, 100);
    public final IntProperty angleStep = new IntProperty("angle-step", 90, 30, 180);
    public final BooleanProperty throughWalls = new BooleanProperty("through-walls", true);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", false);
    public final BooleanProperty allowMining = new BooleanProperty("allow-mining", true);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, weaponsOnly::getValue);
    public final BooleanProperty inventoryCheck = new BooleanProperty("inventory-check", true);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty golems = new BooleanProperty("golems", false);
    public final BooleanProperty silverfish = new BooleanProperty("silverfish", false);
    public final BooleanProperty teams = new BooleanProperty("teams", true);
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[] { "NONE", "DEFAULT", "HUD" });
    public final ModeProperty debugLog = new ModeProperty("debug-log", 0, new String[] { "NONE", "HEALTH" });

    public KillAura() {
        super("KillAura", false);
    }

    // --- Public accessors ---

    public EntityLivingBase getTarget() {
        return currentTarget == null ? null : currentTarget.entity;
    }

    public boolean isCombatInputAllowed() {
        if (inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        }
        if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
            return false;
        }
        if (weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant()) {
            if (!allowTools.getValue() || !ItemUtil.isHoldingTool()) {
                return false;
            }
        }
        return !requirePress.getValue() || PlayerUtil.isAttacking();
    }

    public boolean shouldRenderBlocking() {
        return renderBlocking && ItemUtil.isHoldingSword();
    }

    public boolean isBlockingSword() {
        return (mc.thePlayer.isUsingItem() || serverBlocking) && ItemUtil.isHoldingSword();
    }

    public boolean isAutoBlockActive() {
        return autoBlockActive;
    }

    // --- Private helpers ---

    private long nextAttackDelayMillis() {
        return 1000L / RandomUtil.nextLong(minCPS.getValue(), maxCPS.getValue());
    }

    private long blockHoldDurationMillis() {
        return 1000L / RandomUtil.nextLong(
                autoBlockMinCPS.getValue().longValue(), autoBlockMaxCPS.getValue().longValue());
    }

    private boolean tryAttackTarget(float yaw, float pitch) {
        if (isBlockingSword()) {
            return false;
        }
        if (attackCooldownMillis > 0L) {
            return false;
        }
        attackCooldownMillis += nextAttackDelayMillis();
        mc.thePlayer.swingItem();

        boolean canAttack;
        if (rotations.getValue() == 0) {
            canAttack = isWithinAttackRange(currentTarget.distance);
        } else if (throughWalls.getValue()) {
            canAttack = RotationUtil.rayTrace(currentTarget.bounds, yaw, pitch, attackRange.getValue()) != null;
        } else {
            canAttack = !isRayObstructed(currentTarget.bounds, yaw, pitch, attackRange.getValue());
        }
        if (!canAttack) {
            return false;
        }

        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(currentTarget.entity, Action.ATTACK));
        if (mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
            PlayerUtil.attackEntity(currentTarget.entity);
        }
        attackedThisUpdate = true;
        return true;
    }

    private boolean isRayObstructed(AxisAlignedBB bounds, float yaw, float pitch, double distance) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        if (bounds.isVecInside(eyes)) {
            return false;
        }
        MovingObjectPosition boxHit = RotationUtil.rayTrace(bounds, yaw, pitch, distance);
        if (boxHit == null) {
            return true;
        }
        MovingObjectPosition blockHit = RotationUtil.rayTrace(yaw, pitch, distance, 1.0f);
        if (blockHit == null) {
            return false;
        }
        return eyes.distanceTo(boxHit.hitVec) > eyes.distanceTo(blockHit.hitVec);
    }

    private void startBlockingHeldItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        startBlockingWithStack(mc.thePlayer.getHeldItem());
    }

    private void startBlockingWithStack(ItemStack stack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack));
        mc.thePlayer.setItemInUse(stack, stack.getMaxItemUseDuration());
        serverBlocking = true;
    }

    private void releaseBlocking() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        serverBlocking = false;
    }

    private void interactWithTargetAndBlock(float yaw, float pitch) {
        if (currentTarget == null) {
            return;
        }
        MovingObjectPosition hit = RotationUtil.rayTrace(currentTarget.bounds, yaw, pitch, 8.0);
        if (hit == null) {
            return;
        }
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(currentTarget.entity, new Vec3(
                hit.hitVec.xCoord - currentTarget.x,
                hit.hitVec.yCoord - currentTarget.y,
                hit.hitVec.zCoord - currentTarget.z)));
        PacketUtil.sendPacket(new C02PacketUseEntity(currentTarget.entity, Action.INTERACT));
        startBlockingWithStack(mc.thePlayer.getHeldItem());
    }

    private boolean canRunCombat() {
        if (currentTarget == null) {
            return false;
        }
        if (inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        }
        if (weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant()) {
            if (!allowTools.getValue() || !ItemUtil.isHoldingTool()) {
                return false;
            }
        }
        if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) {
            return false;
        }
        boolean attackKeyDown = PlayerUtil.isAttacking();
        if (allowMining.getValue()
                && attackKeyDown
                && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
            return false;
        }
        return !requirePress.getValue() || attackKeyDown;
    }

    private boolean canAutoBlock() {
        if (!ItemUtil.isHoldingSword()) {
            return false;
        }
        if (autoBlock.getValue() == 0) {
            return true;
        }
        if (!autoBlockRequirePress.getValue() && hasTargetInAutoBlockRange) {
            return true;
        }
        return PlayerUtil.isUsingItem();
    }

    private boolean isValidTargetEntity(EntityLivingBase candidate) {
        if (candidate == mc.thePlayer
                || candidate == mc.thePlayer.ridingEntity
                || candidate == mc.getRenderViewEntity()
                || candidate == mc.getRenderViewEntity().ridingEntity) {
            return false;
        }
        return passesEntityCategoryFilters(candidate);
    }

    private boolean passesEntityCategoryFilters(EntityLivingBase candidate) {
        if (candidate.deathTime > 0) {
            return false;
        }
        if (candidate instanceof EntityOtherPlayerMP) {
            if (!players.getValue()) {
                return false;
            }
            EntityPlayer player = (EntityPlayer) candidate;
            if (TeamUtil.isFriend(player)) {
                return false;
            }
            if (teams.getValue() && TeamUtil.isSameTeam(player)) {
                return false;
            }
            return !botCheck.getValue() || !TeamUtil.isBot(player);
        }
        if (candidate instanceof EntityDragon || candidate instanceof EntityWither) {
            return bosses.getValue();
        }
        if (candidate instanceof EntityMob || candidate instanceof EntitySlime) {
            if (candidate instanceof EntitySilverfish) {
                if (!silverfish.getValue()) {
                    return false;
                }
                return !teams.getValue() || !TeamUtil.hasTeamColor(candidate);
            }
            return mobs.getValue();
        }
        if (candidate instanceof EntityAnimal
                || candidate instanceof EntityBat
                || candidate instanceof EntitySquid
                || candidate instanceof EntityVillager) {
            return animals.getValue();
        }
        if (candidate instanceof EntityIronGolem) {
            if (!golems.getValue()) {
                return false;
            }
            return !teams.getValue() || !TeamUtil.hasTeamColor(candidate);
        }
        return false;
    }

    private boolean isWithinAnyCombatRange(double distance) {
        return isWithinAutoBlockRange(distance) || isWithinSwingRange(distance) || isWithinAttackRange(distance);
    }

    private boolean isWithinAutoBlockRange(double distance) {
        return autoBlock.getValue() != 0 && distance <= autoBlockRange.getValue();
    }

    private boolean isWithinSwingRange(double distance) {
        return distance <= swingRange.getValue();
    }

    private boolean isWithinAttackRange(double distance) {
        return distance <= attackRange.getValue();
    }

    private boolean isEnemyPlayer(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && TeamUtil.isTarget((EntityPlayer) entity);
    }

    private boolean isAutoBlockSettingsVisible() {
        return autoBlock.getValue() != 0;
    }

    private float aimError(AxisAlignedBB bounds) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        if (bounds.isVecInside(eyes)) {
            return 0.0f;
        }
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        double trace = 30.0 + (bounds.maxX - bounds.minX) + (bounds.maxZ - bounds.minZ) + (bounds.maxY - bounds.minY);
        if (RotationUtil.rayTrace(bounds, yaw, pitch, trace) != null) {
            return 0.0f;
        }
        Vec3 center = new Vec3(
                (bounds.minX + bounds.maxX) / 2.0,
                (bounds.minY + bounds.maxY) / 2.0,
                (bounds.minZ + bounds.maxZ) / 2.0);
        double dx = center.xCoord - eyes.xCoord;
        double dy = center.yCoord - eyes.yCoord;
        double dz = center.zCoord - eyes.zCoord;
        float aimYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float aimPitch = (float) (-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
        float yawErr = Math.abs(MathHelper.wrapAngleTo180_float(aimYaw - yaw));
        float pitchErr = Math.abs(aimPitch - pitch);
        return Math.max(yawErr * 2.0f, pitchErr * 2.0f);
    }

    // --- Event handlers ---

    @SubscribeEvent
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (!event.isPre())
            return;

        rotState.register();
        rotState.captureClientAngles();

        boolean combatEligible = canRunCombat();
        boolean canAttackThisTick = combatEligible;
        boolean canBlockThisTick = combatEligible && canAutoBlock();

        if (attackCooldownMillis > 0L) {
            attackCooldownMillis -= 50L;
        }
        if (blockHoldMillis > 0L) {
            blockHoldMillis -= 50L;
        }

        if (!canBlockThisTick) {
            if (autoBlockActive && isBlockingSword()) {
                releaseBlocking();
                canAttackThisTick = false;
            }
            autoBlockActive = false;
            renderBlocking = false;
            blockHoldMillis = 0L;
            autoBlockPhase = 0;
        }

        if (!combatEligible) {
            rotController.setActive(false);
            return;
        }

        boolean shouldStartBlocking = false;
        if (canBlockThisTick) {
            switch (autoBlock.getValue()) {
                case 0: // NONE
                    autoBlockActive = false;
                    renderBlocking = false;
                    if (PlayerUtil.isUsingItem() && !isBlockingSword()) {
                        shouldStartBlocking = true;
                    }
                    break;

                case 1: // LEGIT
                    autoBlockActive = true;
                    renderBlocking = true;
                    switch (autoBlockPhase) {
                        case 0:
                            if (!isBlockingSword()) {
                                shouldStartBlocking = true;
                                blockHoldMillis += blockHoldDurationMillis();
                            }
                            autoBlockPhase = 1;
                            break;
                        case 1:
                            if (blockHoldMillis <= 0L) {
                                if (isBlockingSword()) {
                                    releaseBlocking();
                                    canAttackThisTick = false;
                                }
                                autoBlockPhase = 0;
                            }
                            break;
                        default:
                            autoBlockPhase = 0;
                    }
                    break;
            }
        }

        boolean attacked = false;
        float attackYaw = mc.thePlayer.rotationYaw;
        float attackPitch = mc.thePlayer.rotationPitch;

        if (currentTarget.hasUsableAim && isWithinSwingRange(currentTarget.distance)) {
            float[] rotation = computeRotation(rotations.getValue());
            attackYaw = rotation[0];
            attackPitch = rotation[1];
            if (canAttackThisTick) {
                attacked = tryAttackTarget(attackYaw, attackPitch);
            }
        } else {
            rotController.setActive(false);
        }

        if (shouldStartBlocking) {
            if (attacked) {
                interactWithTargetAndBlock(attackYaw, attackPitch);
            } else {
                startBlockingHeldItem();
            }
        }
    }

    private float[] computeRotation(int rotationMode) {
        float smooth = smoothing.getValue() / 100.0f;
        if (rotationMode == 2) {
            float[] rots = rotController.aimAtStepped(currentTarget.entity,
                    rotState.getServerYaw(), rotState.getServerPitch(), angleStep.getValue(), smooth);
            rotController.setActive(true);
            return rots;
        }
        if (rotationMode == 3) {
            float[] rots = rotController.aimAtStepped(currentTarget.entity,
                    mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, angleStep.getValue(), smooth);
            mc.thePlayer.rotationYaw = rots[0];
            mc.thePlayer.rotationPitch = rots[1];
            rotController.setActive(false);
            return rots;
        }
        rotController.setActive(false);
        return new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch };
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (event.phase == TickEvent.Phase.START) {
            selectTarget();
        }

        if (event.phase == TickEvent.Phase.END) {
            if (isBlockingSword() && !mc.thePlayer.isBlocking()) {
                ItemStack held = mc.thePlayer.getHeldItem();
                if (held != null) {
                    mc.thePlayer.setItemInUse(held, held.getMaxItemUseDuration());
                }
            }
        }
    }

    private void selectTarget() {
        ArrayList<Target> candidates = collectTargetCandidates();
        if (candidates.isEmpty()) {
            currentTarget = null;
            hasTargetInAutoBlockRange = false;
            return;
        }

        hasTargetInAutoBlockRange = candidates.stream().anyMatch(t -> isWithinAutoBlockRange(t.distance));
        if (candidates.stream().anyMatch(t -> t.hasUsableAim)) {
            candidates.removeIf(t -> !t.hasUsableAim);
        }
        if (candidates.stream().anyMatch(t -> isWithinSwingRange(t.distance))) {
            candidates.removeIf(t -> !isWithinSwingRange(t.distance));
        }
        if (candidates.stream().anyMatch(t -> isWithinAttackRange(t.distance))) {
            candidates.removeIf(t -> !isWithinAttackRange(t.distance));
        }
        if (candidates.stream().anyMatch(t -> isEnemyPlayer(t.entity))) {
            candidates.removeIf(t -> !isEnemyPlayer(t.entity));
        }
        if (candidates.isEmpty()) {
            currentTarget = null;
            return;
        }

        if (currentTarget != null && !targetSwitchTimer.hasTimeElapsed(switchDelay.getValue())) {
            for (Target candidate : candidates) {
                if (candidate.entity == currentTarget.entity) {
                    currentTarget = candidate;
                    return;
                }
            }
        }

        candidates.sort(this::compareTargets);
        if (mode.getValue() == 1 && attackedThisUpdate) {
            attackedThisUpdate = false;
            targetIndex++;
        }
        if (mode.getValue() == 0 || targetIndex >= candidates.size()) {
            targetIndex = 0;
        }
        currentTarget = candidates.get(targetIndex);
        targetSwitchTimer.reset();
    }

    private ArrayList<Target> collectTargetCandidates() {
        ArrayList<Target> candidates = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!isValidTargetEntity(living)) {
                continue;
            }

            double border = living.getCollisionBorderSize();
            AxisAlignedBB bounds = living.getEntityBoundingBox().expand(border, border, border);
            double distance = RotationUtil.distanceToBox(bounds);
            if (!isWithinAnyCombatRange(distance)) {
                continue;
            }

            float angularDistance = aimError(bounds);
            if (angularDistance > fov.getValue()) {
                continue;
            }

            boolean usableAim = true;
            if (!throughWalls.getValue()) {
                float[] aim = RotationUtil.getRotationsToBox(bounds,
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0f, 0.0f);
                if (isRayObstructed(bounds, aim[0], aim[1], 8.0)
                        && isRayObstructed(bounds, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 8.0)) {
                    usableAim = false;
                }
            }

            candidates.add(new Target(living, bounds, living.posX, living.posY, living.posZ,
                    distance, angularDistance, usableAim));
        }
        return candidates;
    }

    private int compareTargets(Target first, Target second) {
        int comparison;
        switch (sort.getValue()) {
            case 1: // HEALTH
                comparison = Float.compare(
                        TeamUtil.getHealthScore(first.entity), TeamUtil.getHealthScore(second.entity));
                break;
            case 2: // HURT_TIME
                comparison = Integer.compare(first.entity.hurtResistantTime, second.entity.hurtResistantTime);
                break;
            case 3: // FOV
                comparison = Float.compare(first.angularDistance, second.angularDistance);
                break;
            default: // DISTANCE
                comparison = Double.compare(first.distance, second.distance);
                break;
        }
        return comparison != 0 ? comparison : Double.compare(first.distance, second.distance);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.isCanceled() || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (event.isSend()) {
            if (event.getPacket() instanceof C03PacketPlayer
                    && rotController.isActive()
                    && rotations.getValue() == 2) {
                IAccessorC03PacketPlayer accessor = (IAccessorC03PacketPlayer) event.getPacket();
                accessor.setYaw(rotController.getTargetYaw());
                accessor.setPitch(rotController.getTargetPitch());
                accessor.setRotating(true);
            }

            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging digging = (C07PacketPlayerDigging) event.getPacket();
                if (digging.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    serverBlocking = false;
                }
            }
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                serverBlocking = false;
                if (autoBlockActive) {
                    mc.thePlayer.stopUsingItem();
                }
            }
        }

        if (event.isReceive() && debugLog.getValue() == 1) {
            if (event.getPacket() instanceof S06PacketUpdateHealth) {
                S06PacketUpdateHealth health = (S06PacketUpdateHealth) event.getPacket();
                logHealthDelta(health.getHealth() - mc.thePlayer.getHealth());
            } else if (event.getPacket() instanceof S1CPacketEntityMetadata) {
                S1CPacketEntityMetadata metadata = (S1CPacketEntityMetadata) event.getPacket();
                if (metadata.getEntityId() == mc.thePlayer.getEntityId()) {
                    for (WatchableObject watchable : metadata.func_149376_c()) {
                        if (watchable.getDataValueId() == 6) {
                            logHealthDelta((Float) watchable.getObject() - mc.thePlayer.getHealth());
                        }
                    }
                }
            }
        }
    }

    private void logHealthDelta(float delta) {
        if (delta == 0.0f || lastDamageLogTick == mc.thePlayer.ticksExisted) {
            return;
        }
        lastDamageLogTick = mc.thePlayer.ticksExisted;
        String color = delta > 0.0f ? "&a" : "&c";
        ChatUtil.sendFormatted(String.format(
                "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                CoffeeClient.CLIENT_NAME,
                color,
                df.format(delta),
                mc.thePlayer.ticksExisted));
    }

    @SubscribeEvent
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled() || mc.thePlayer == null)
            return;
        if (moveFix.getValue() == 0 || rotations.getValue() == 3)
            return;
        if (!rotController.isActive() || !MoveUtil.isForwardPressed())
            return;

        if (moveFix.getValue() == 1) {
            MoveUtil.fixStrafe(rotController.getTargetYaw());
        } else {
            float forward = mc.thePlayer.movementInput.moveForward;
            float strafe = mc.thePlayer.movementInput.moveStrafe;
            float delta = (float) Math.toRadians(mc.thePlayer.rotationYaw - rotController.getTargetYaw());
            float cos = MathHelper.cos(delta);
            float sin = MathHelper.sin(delta);
            mc.thePlayer.movementInput.moveForward = forward * cos + strafe * sin;
            mc.thePlayer.movementInput.moveStrafe = strafe * cos - forward * sin;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || currentTarget == null || mc.thePlayer == null)
            return;
        if (showTarget.getValue() == 0 || !TeamUtil.isEntityLoaded(currentTarget.entity) || !isCombatInputAllowed())
            return;

        Color color = Color.WHITE;
        if (showTarget.getValue() == 1) {
            color = currentTarget.entity.hurtTime > 0 ? new Color(16733525) : new Color(5635925);
        } else if (showTarget.getValue() == 2) {
            HUD hud = (HUD) CoffeeClient.featureManager.getFeature(HUD.class);
            if (hud != null) {
                color = hud.getColor(System.currentTimeMillis());
            }
        }
        RenderUtil.enableRenderState();
        RenderUtil.drawEntityBox(currentTarget.entity, color.getRed(), color.getGreen(), color.getBlue(),
                event.partialTicks);
        RenderUtil.disableRenderState();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (autoBlockActive) {
            event.setCanceled(true);
            return;
        }
        if (isEnabled() && canRunCombat() && currentTarget != null
                && isWithinSwingRange(currentTarget.distance)) {
            event.setCanceled(true);
        }
    }

    // --- Lifecycle ---

    @Override
    public void onEnabled() {
        currentTarget = null;
        hasTargetInAutoBlockRange = false;
        targetIndex = 0;
        attackedThisUpdate = false;
        attackCooldownMillis = 0L;
        blockHoldMillis = 0L;
        autoBlockPhase = 0;
        rotController.setActive(false);
    }

    @Override
    public void onDisabled() {
        if (serverBlocking) {
            releaseBlocking();
        }
        currentTarget = null;
        hasTargetInAutoBlockRange = false;
        serverBlocking = false;
        autoBlockActive = false;
        renderBlocking = false;
        autoBlockPhase = 0;
        blockHoldMillis = 0L;
        rotController.setActive(false);
    }

    @Override
    public void verifyValue(String value) {
        if (swingRange.getName().equals(value) && swingRange.getValue() < attackRange.getValue()) {
            attackRange.setValue(swingRange.getValue());
        } else if (attackRange.getName().equals(value) && attackRange.getValue() > swingRange.getValue()) {
            swingRange.setValue(attackRange.getValue());
        }

        if (minCPS.getValue() > maxCPS.getValue()) {
            if (minCPS.getName().equals(value)) {
                maxCPS.setValue(minCPS.getValue());
            } else {
                minCPS.setValue(maxCPS.getValue());
            }
        }

        if (autoBlockMinCPS.getValue() > autoBlockMaxCPS.getValue()) {
            if (autoBlockMinCPS.getName().equals(value)) {
                autoBlockMaxCPS.setValue(autoBlockMinCPS.getValue());
            } else {
                autoBlockMinCPS.setValue(autoBlockMaxCPS.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[] { CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, mode.getModeString()) };
    }

    public static class Target {
        public final EntityLivingBase entity;
        public final AxisAlignedBB bounds;
        public final double x;
        public final double y;
        public final double z;
        public final double distance;
        public final float angularDistance;
        public final boolean hasUsableAim;

        public Target(EntityLivingBase entity, AxisAlignedBB bounds, double x, double y, double z,
                double distance, float angularDistance, boolean hasUsableAim) {
            this.entity = entity;
            this.bounds = bounds;
            this.x = x;
            this.y = y;
            this.z = z;
            this.distance = distance;
            this.angularDistance = angularDistance;
            this.hasUsableAim = hasUsableAim;
        }
    }
}
