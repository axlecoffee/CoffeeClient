/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.feature.combat;

import com.google.common.base.CaseFormat;
import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.events.LeftClickMouseEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.MoveInputEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.PacketEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.UpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.FloatProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.ModeProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.rotation.KillAuraRotation;
import io.github.moulberry.notenoughupdates.coffeeclient.rotation.RotationState;
import io.github.moulberry.notenoughupdates.coffeeclient.util.*;
import io.github.moulberry.notenoughupdates.mixins.IAccessorC03PacketPlayer;
import io.github.moulberry.notenoughupdates.mixins.IAccessorPlayerControllerMP;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
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
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
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
import java.util.Random;

/**
 * KillAura combat feature.
 * <p>
 * Based on OpenMyau KillAura by 60124808866.
 */
public class KillAura extends Feature {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));

    private final TimerUtil timer = new TimerUtil();
    private AttackData target = null;
    private int switchTick = 0;
    private boolean hitRegistered = false;
    private boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private long attackDelayMS = 0L;
    private int blockTick = 0;
    private int lastTickProcessed = 0;

    private final RotationState rotState = RotationState.getInstance();
    private final KillAuraRotation rotController = new KillAuraRotation(rotState);

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[] { "SINGLE", "SWITCH" });
    public final ModeProperty sort = new ModeProperty("sort", 0,
            new String[] { "DISTANCE", "HEALTH", "HURT_TIME", "FOV" });
    public final ModeProperty autoBlock = new ModeProperty(
            "auto-block", 0, new String[] { "NONE", "VANILLA", "SPOOF", "HYPIXEL", "LEGIT", "FAKE" });
    public final BooleanProperty autoBlockRequirePress = new BooleanProperty("auto-block-require-press", false);
    public final FloatProperty autoBlockMinCPS = new FloatProperty("auto-block-min-aps", 8.0f, 1.0f, 20.0f);
    public final FloatProperty autoBlockMaxCPS = new FloatProperty("auto-block-max-aps", 10.0f, 1.0f, 20.0f);
    public final FloatProperty autoBlockRange = new FloatProperty("auto-block-range", 6.0f, 3.0f, 8.0f);
    public final FloatProperty swingRange = new FloatProperty("swing-range", 3.5f, 3.0f, 6.0f);
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
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[] { "NONE", "DEFAULT" });
    public final ModeProperty debugLog = new ModeProperty("debug-log", 0, new String[] { "NONE", "HEALTH" });

    public KillAura() {
        super("KillAura", false);
    }

    // --- Public accessors ---

    public EntityLivingBase getTarget() {
        return target != null ? target.getEntity() : null;
    }

    public boolean isBlocking() {
        return fakeBlockState && ItemUtil.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || blockingState) && ItemUtil.isHoldingSword();
    }

    public boolean isAttackAllowed() {
        if (!weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || (allowTools.getValue() && ItemUtil.isHoldingTool())) {
            return !requirePress.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
        }
        return false;
    }

    public boolean shouldAutoBlock() {
        if (isPlayerBlocking() && isBlocking) {
            return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()
                    && (autoBlock.getValue() == 3 || autoBlock.getValue() == 4);
        }
        return false;
    }

    // --- Private helpers ---

    private long getAttackDelay() {
        if (isBlocking) {
            return (long) (1000.0f / RandomUtil.nextLong(
                    autoBlockMinCPS.getValue().longValue(),
                    autoBlockMaxCPS.getValue().longValue()));
        }
        return 1000L / RandomUtil.nextLong(minCPS.getValue(), maxCPS.getValue());
    }

    private boolean performAttack(float yaw, float pitch) {
        IAccessorPlayerControllerMP controller = (IAccessorPlayerControllerMP) mc.playerController;
        if (controller.getIsHittingBlock()) {
            return false;
        }
        if (isPlayerBlocking() && autoBlock.getValue() != 1) {
            return false;
        }
        if (attackDelayMS > 0L) {
            return false;
        }
        attackDelayMS += getAttackDelay();
        mc.thePlayer.swingItem();

        if ((rotations.getValue() != 0 || !isBoxInAttackRange(target.getBox()))
                && RotationUtil.rayTrace(target.getBox(), yaw, pitch, attackRange.getValue()) == null) {
            return false;
        }

        controller.callSyncCurrentPlayItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.ATTACK));
        if (mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
            PlayerUtil.attackEntity(target.getEntity());
        }
        hitRegistered = true;
        return true;
    }

    private void sendUseItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        startBlock(mc.thePlayer.getHeldItem());
    }

    private void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.thePlayer.setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        blockingState = true;
    }

    private void stopBlock() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        blockingState = false;
    }

    private void interactAttack(float yaw, float pitch) {
        if (target == null)
            return;
        MovingObjectPosition mop = RotationUtil.rayTrace(target.getBox(), yaw, pitch, 8.0);
        if (mop != null) {
            IAccessorPlayerControllerMP controller = (IAccessorPlayerControllerMP) mc.playerController;
            controller.callSyncCurrentPlayItem();
            PacketUtil.sendPacket(new C02PacketUseEntity(
                    target.getEntity(),
                    new Vec3(
                            mop.hitVec.xCoord - target.getX(),
                            mop.hitVec.yCoord - target.getY(),
                            mop.hitVec.zCoord - target.getZ())));
            PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.INTERACT));
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
            blockingState = true;
        }
    }

    private boolean canAttack() {
        if (inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        }
        if (weaponsOnly.getValue()
                && !ItemUtil.hasRawUnbreakingEnchant()
                && !(allowTools.getValue() && ItemUtil.isHoldingTool())) {
            return false;
        }
        if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
            return false;
        }
        if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) {
            return false;
        }
        if (requirePress.getValue()) {
            return PlayerUtil.isAttacking();
        }
        if (allowMining.getValue()
                && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
                && PlayerUtil.isAttacking()) {
            return false;
        }
        return true;
    }

    private boolean canAutoBlock() {
        if (!ItemUtil.isHoldingSword()) {
            return false;
        }
        return !autoBlockRequirePress.getValue() || PlayerUtil.isUsingItem();
    }

    private boolean hasValidTarget() {
        return mc.theWorld.loadedEntityList.stream()
                .anyMatch(entity -> entity instanceof EntityLivingBase
                        && isValidTarget((EntityLivingBase) entity)
                        && isInBlockRange((EntityLivingBase) entity));
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (!mc.theWorld.loadedEntityList.contains(entity)) {
            return false;
        }
        if (entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity) {
            return false;
        }
        if (entity == mc.getRenderViewEntity() || entity == mc.getRenderViewEntity().ridingEntity) {
            return false;
        }
        if (entity.deathTime > 0) {
            return false;
        }
        if (RotationUtil.angleToEntity(entity) > fov.getValue().floatValue()) {
            return false;
        }
        if (!throughWalls.getValue() && RotationUtil.rayTrace(entity) != null) {
            return false;
        }
        if (entity instanceof EntityOtherPlayerMP) {
            if (!players.getValue()) {
                return false;
            }
            if (TeamUtil.isFriend((EntityPlayer) entity)) {
                return false;
            }
            if (teams.getValue() && TeamUtil.isSameTeam((EntityPlayer) entity)) {
                return false;
            }
            return !botCheck.getValue() || !TeamUtil.isBot((EntityPlayer) entity);
        }
        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return bosses.getValue();
        }
        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntitySilverfish) {
                return silverfish.getValue() && (!teams.getValue() || !TeamUtil.hasTeamColor(entity));
            }
            return this.mobs.getValue();
        }
        if (entity instanceof EntityAnimal
                || entity instanceof EntityBat
                || entity instanceof EntitySquid
                || entity instanceof EntityVillager) {
            return this.animals.getValue();
        }
        if (entity instanceof EntityIronGolem) {
            return this.golems.getValue() && (!teams.getValue() || !TeamUtil.hasTeamColor(entity));
        }
        return false;
    }

    private boolean isInRange(EntityLivingBase entity) {
        return isInBlockRange(entity) || isInSwingRange(entity) || isInAttackRange(entity);
    }

    private boolean isInBlockRange(EntityLivingBase entity) {
        return RotationUtil.distanceToEntity(entity) <= (double) autoBlockRange.getValue();
    }

    private boolean isInSwingRange(EntityLivingBase entity) {
        return RotationUtil.distanceToEntity(entity) <= (double) swingRange.getValue();
    }

    private boolean isBoxInSwingRange(AxisAlignedBB box) {
        return RotationUtil.distanceToBox(box) <= (double) swingRange.getValue();
    }

    private boolean isInAttackRange(EntityLivingBase entity) {
        return RotationUtil.distanceToEntity(entity) <= (double) attackRange.getValue();
    }

    private boolean isBoxInAttackRange(AxisAlignedBB box) {
        return RotationUtil.distanceToBox(box) <= (double) attackRange.getValue();
    }

    private boolean isPlayerTarget(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && TeamUtil.isTarget((EntityPlayer) entity);
    }

    private int findEmptySlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && !stack.hasDisplayName()) {
                    return i;
                }
            }
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    private int findSwordSlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
                if (item != null && item.getItem() instanceof ItemSword) {
                    return i;
                }
            }
        }
        return -1;
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

        if (attackDelayMS > 0L) {
            attackDelayMS -= 50L;
        }

        boolean attack = target != null && canAttack();
        boolean block = attack && canAutoBlock();

        if (!block) {
            isBlocking = false;
            fakeBlockState = false;
            blockTick = 0;
        }

        if (!attack)
            return;

        boolean swap = false;
        IAccessorPlayerControllerMP controller = (IAccessorPlayerControllerMP) mc.playerController;

        if (block) {
            switch (autoBlock.getValue()) {
                case 0: // NONE
                    if (PlayerUtil.isUsingItem()) {
                        isBlocking = true;
                        if (!isPlayerBlocking() && !controller.getIsHittingBlock()) {
                            swap = true;
                        }
                    } else {
                        isBlocking = false;
                        if (isPlayerBlocking() && !controller.getIsHittingBlock()) {
                            stopBlock();
                        }
                    }
                    fakeBlockState = false;
                    break;

                case 1: // VANILLA
                    if (hasValidTarget()) {
                        if (!isPlayerBlocking() && !controller.getIsHittingBlock()) {
                            swap = true;
                        }
                        isBlocking = true;
                        fakeBlockState = false;
                    } else {
                        isBlocking = false;
                        fakeBlockState = false;
                    }
                    break;

                case 2: // SPOOF
                    if (hasValidTarget()) {
                        int item = controller.getCurrentPlayerItem();
                        if (!controller.getIsHittingBlock()
                                && mc.thePlayer.inventory.currentItem == item
                                && !(isPlayerBlocking() && blockTick != 0)
                                && !(attackDelayMS > 0L && attackDelayMS <= 50L)) {
                            int slot = findEmptySlot(item);
                            PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
                            PacketUtil.sendPacket(new C09PacketHeldItemChange(item));
                            swap = true;
                            blockTick = 1;
                        } else {
                            blockTick = 0;
                        }
                        isBlocking = true;
                        fakeBlockState = false;
                    } else {
                        isBlocking = false;
                        fakeBlockState = false;
                    }
                    break;

                case 3: // HYPIXEL
                    if (hasValidTarget()) {
                        if (!controller.getIsHittingBlock()) {
                            switch (blockTick) {
                                case 0:
                                    if (!isPlayerBlocking()) {
                                        swap = true;
                                    }
                                    blockTick = 1;
                                    break;
                                case 1:
                                    if (isPlayerBlocking()) {
                                        int randomSlot = new Random().nextInt(9);
                                        while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                            randomSlot = new Random().nextInt(9);
                                        }
                                        PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                        PacketUtil.sendPacket(new C09PacketHeldItemChange(
                                                mc.thePlayer.inventory.currentItem));
                                        stopBlock();
                                        attack = false;
                                    }
                                    if (attackDelayMS <= 50L) {
                                        blockTick = 0;
                                    }
                                    break;
                                default:
                                    blockTick = 0;
                            }
                        }
                        isBlocking = true;
                        fakeBlockState = true;
                    } else {
                        isBlocking = false;
                        fakeBlockState = false;
                    }
                    break;

                case 4: // LEGIT
                    if (hasValidTarget()) {
                        if (!controller.getIsHittingBlock()) {
                            switch (blockTick) {
                                case 0:
                                    if (!isPlayerBlocking()) {
                                        swap = true;
                                    }
                                    blockTick = 1;
                                    break;
                                case 1:
                                    if (isPlayerBlocking()) {
                                        stopBlock();
                                        attack = false;
                                    }
                                    if (attackDelayMS <= 50L) {
                                        blockTick = 0;
                                    }
                                    break;
                                default:
                                    blockTick = 0;
                            }
                        }
                        isBlocking = false;
                        fakeBlockState = false;
                    } else {
                        isBlocking = false;
                        fakeBlockState = false;
                    }
                    break;

                case 5: // FAKE
                    isBlocking = false;
                    fakeBlockState = hasValidTarget();
                    if (PlayerUtil.isUsingItem()
                            && !isPlayerBlocking()
                            && !controller.getIsHittingBlock()) {
                        swap = true;
                    }
                    break;
            }
        }

        // Apply rotations & attack
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        float attackYaw = currentYaw;
        float attackPitch = currentPitch;
        float stepJitter = RandomUtil.nextFloat(-5.0f, 5.0f);
        float smooth = (float) smoothing.getValue() / 100.0f;

        boolean attacked = false;
        if (isBoxInSwingRange(target.getBox())) {
            if (rotations.getValue() == 2) {
                // SILENT — use KillAuraRotation VSPLIT + GCD pipeline.
                // Target angles are stored in rotController and applied
                // to outgoing C03 packets. The player's visual rotation
                // is NEVER modified.
                float hSpeed = 10.0f - smooth * 8.0f;
                float vSpeed = 10.0f - smooth * 8.0f;
                float[] rots = rotController.aimAt(
                        target.getEntity(), smooth, hSpeed, vSpeed);
                rotController.setActive(true);
                attackYaw = rots[0];
                attackPitch = rots[1];

            } else if (rotations.getValue() == 3) {
                // LOCK_VIEW — VSPLIT aim, applied directly to the player's view
                float[] rots = rotController.aimAtStepped(
                        target.getEntity(), currentYaw, currentPitch,
                        (float) angleStep.getValue() + stepJitter, smooth);
                mc.thePlayer.rotationYaw = rots[0];
                mc.thePlayer.rotationPitch = rots[1];
                attackYaw = rots[0];
                attackPitch = rots[1];
                rotController.setActive(false);

            } else if (rotations.getValue() == 1) {
                // LEGIT — smooth rotation applied to the actual view
                float[] rots = rotController.aimAtStepped(
                        target.getEntity(), currentYaw, currentPitch,
                        (float) angleStep.getValue() + stepJitter, smooth);
                mc.thePlayer.rotationYaw = rots[0];
                mc.thePlayer.rotationPitch = rots[1];
                attackYaw = rots[0];
                attackPitch = rots[1];
                rotController.setActive(false);

            } else {
                // NONE — no rotation changes
                rotController.setActive(false);
            }

            if (attack) {
                attacked = performAttack(attackYaw, attackPitch);
            }
        } else {
            rotController.setActive(false);
        }

        if (swap) {
            if (attacked) {
                interactAttack(attackYaw, attackPitch);
            } else {
                sendUseItem();
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (event.phase == TickEvent.Phase.START) {
            // Target selection (PRE tick)
            if (target == null
                    || !isValidTarget(target.getEntity())
                    || !isBoxInAttackRange(target.getBox())
                    || !isBoxInSwingRange(target.getBox())
                    || timer.hasTimeElapsed(switchDelay.getValue().longValue())) {

                timer.reset();
                ArrayList<EntityLivingBase> targets = new ArrayList<>();
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (entity instanceof EntityLivingBase
                            && isValidTarget((EntityLivingBase) entity)
                            && isInRange((EntityLivingBase) entity)) {
                        targets.add((EntityLivingBase) entity);
                    }
                }

                if (targets.isEmpty()) {
                    target = null;
                } else {
                    if (targets.stream().anyMatch(this::isInSwingRange)) {
                        targets.removeIf(e -> !isInSwingRange(e));
                    }
                    if (targets.stream().anyMatch(this::isInAttackRange)) {
                        targets.removeIf(e -> !isInAttackRange(e));
                    }
                    if (targets.stream().anyMatch(this::isPlayerTarget)) {
                        targets.removeIf(e -> !isPlayerTarget(e));
                    }

                    targets.sort((a, b) -> {
                        int sortBase = 0;
                        switch (sort.getValue()) {
                            case 1: // HEALTH
                                sortBase = Float.compare(
                                        TeamUtil.getHealthScore(a),
                                        TeamUtil.getHealthScore(b));
                                break;
                            case 2: // HURT_TIME
                                sortBase = Integer.compare(a.hurtResistantTime, b.hurtResistantTime);
                                break;
                            case 3: // FOV
                                sortBase = Float.compare(
                                        RotationUtil.angleToEntity(a),
                                        RotationUtil.angleToEntity(b));
                                break;
                        }
                        return sortBase != 0
                                ? sortBase
                                : Double.compare(
                                        RotationUtil.distanceToEntity(a),
                                        RotationUtil.distanceToEntity(b));
                    });

                    if (mode.getValue() == 1 && hitRegistered) {
                        hitRegistered = false;
                        switchTick++;
                    }
                    if (mode.getValue() == 0 || switchTick >= targets.size()) {
                        switchTick = 0;
                    }

                    target = new AttackData(targets.get(switchTick));
                }
            }

            if (target != null) {
                target = new AttackData(target.getEntity());
            }
        }

        if (event.phase == TickEvent.Phase.END) {
            // POST tick — fix visual blocking state
            if (isPlayerBlocking() && !mc.thePlayer.isBlocking()) {
                ItemStack held = mc.thePlayer.getHeldItem();
                if (held != null) {
                    mc.thePlayer.setItemInUse(held, held.getMaxItemUseDuration());
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.isCanceled() || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (event.isSend()) {
            // ── Silent rotation: spoof yaw/pitch on outgoing C03 packets ──
            // This is the correct way to do silent aim — the player's visual
            // rotation (rotationYaw/Pitch) is never changed, so the camera
            // stays perfectly still. Only the packet the server receives
            // contains the spoofed angles.
            if (event.getPacket() instanceof C03PacketPlayer
                    && rotController.isActive()
                    && rotations.getValue() == 2) {
                IAccessorC03PacketPlayer accessor = (IAccessorC03PacketPlayer) event.getPacket();
                accessor.setYaw(rotController.getTargetYaw());
                accessor.setPitch(rotController.getTargetPitch());
                accessor.setRotating(true);
            }

            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
                if (packet.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    blockingState = false;
                }
            }
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                blockingState = false;
                if (isBlocking) {
                    mc.thePlayer.stopUsingItem();
                }
            }
        }

        // Debug health logging
        if (debugLog.getValue() == 1 && isAttackAllowed() && event.isReceive()) {
            if (event.getPacket() instanceof S06PacketUpdateHealth) {
                float diff = ((S06PacketUpdateHealth) event.getPacket()).getHealth() - mc.thePlayer.getHealth();
                if (diff != 0.0f && lastTickProcessed != mc.thePlayer.ticksExisted) {
                    lastTickProcessed = mc.thePlayer.ticksExisted;
                    ChatUtil.sendFormatted(String.format(
                            "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                            CoffeeClient.CLIENT_NAME,
                            diff > 0.0f ? "&a" : "&c",
                            df.format(diff),
                            mc.thePlayer.ticksExisted));
                }
            }
        }
    }

    @SubscribeEvent
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled() || mc.thePlayer == null)
            return;

        // Move fix: compensate WASD inputs for the yaw difference between
        // the player's visual rotation and the spoofed server-side rotation.
        if (moveFix.getValue() != 0
                && rotations.getValue() == 2
                && rotController.isActive()
                && MoveUtil.isForwardPressed()) {
            if (moveFix.getValue() == 1) {
                // SILENT move fix — remap to 8 directions
                MoveUtil.fixStrafe(rotController.getTargetYaw());
            } else {
                // STRICT move fix — continuous rotation compensation
                float forward = mc.thePlayer.movementInput.moveForward;
                float strafe = mc.thePlayer.movementInput.moveStrafe;
                float delta = (float) Math.toRadians(
                        mc.thePlayer.rotationYaw - rotController.getTargetYaw());
                float cos = net.minecraft.util.MathHelper.cos(delta);
                float sin = net.minecraft.util.MathHelper.sin(delta);
                mc.thePlayer.movementInput.moveForward = forward * cos + strafe * sin;
                mc.thePlayer.movementInput.moveStrafe = strafe * cos - forward * sin;
            }
        }
        if (shouldAutoBlock()) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || target == null || mc.thePlayer == null)
            return;

        if (showTarget.getValue() != 0
                && TeamUtil.isEntityLoaded(target.getEntity())
                && isAttackAllowed()) {
            Color color;
            if (target.getEntity().hurtTime > 0) {
                color = new Color(16733525); // red-ish
            } else {
                color = new Color(5635925); // green-ish
            }
            RenderUtil.enableRenderState();
            RenderUtil.drawEntityBox(target.getEntity(), color.getRed(), color.getGreen(), color.getBlue(),
                    event.partialTicks);
            RenderUtil.disableRenderState();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (!isEnabled())
            return;
        if (isBlocking) {
            event.setCanceled(true);
        } else if (target != null && canAttack()) {
            event.setCanceled(true);
        }
    }

    // --- Lifecycle ---

    @Override
    public void onEnabled() {
        target = null;
        switchTick = 0;
        hitRegistered = false;
        attackDelayMS = 0L;
        blockTick = 0;
        rotController.setActive(false);
    }

    @Override
    public void onDisabled() {
        blockingState = false;
        isBlocking = false;
        fakeBlockState = false;
        rotController.setActive(false);
    }

    @Override
    public void verifyValue(String value) {
        boolean badCps = autoBlock.getValue() == 2
                || autoBlock.getValue() == 3
                || autoBlock.getValue() == 4;

        if (autoBlock.getName().equals(value)) {
            if (badCps && (autoBlockMinCPS.getValue() > 10.0f || autoBlockMaxCPS.getValue() > 10.0f)) {
                autoBlockMinCPS.setValue(8.0f);
                autoBlockMaxCPS.setValue(10.0f);
            }
            return;
        }
        if (swingRange.getName().equals(value)) {
            if (swingRange.getValue() < attackRange.getValue()) {
                attackRange.setValue(swingRange.getValue());
            }
        } else if (attackRange.getName().equals(value)) {
            if (swingRange.getValue() < attackRange.getValue()) {
                swingRange.setValue(attackRange.getValue());
            }
        } else if (minCPS.getName().equals(value)) {
            if (minCPS.getValue() > maxCPS.getValue()) {
                maxCPS.setValue(minCPS.getValue());
            }
        } else if (autoBlockMinCPS.getName().equals(value)) {
            if (autoBlockMinCPS.getValue() > autoBlockMaxCPS.getValue()) {
                autoBlockMaxCPS.setValue(autoBlockMinCPS.getValue());
            }
            if (autoBlockMinCPS.getValue() > 10.0f && badCps) {
                autoBlockMinCPS.setValue(10.0f);
            }
        } else if (autoBlockMaxCPS.getName().equals(value)) {
            if (autoBlockMinCPS.getValue() > autoBlockMaxCPS.getValue()) {
                autoBlockMinCPS.setValue(autoBlockMaxCPS.getValue());
            }
            if (autoBlockMaxCPS.getValue() > 10.0f && badCps) {
                autoBlockMaxCPS.setValue(10.0f);
            }
        } else if (maxCPS.getName().equals(value)) {
            if (minCPS.getValue() > maxCPS.getValue()) {
                minCPS.setValue(maxCPS.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[] { CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, mode.getModeString()) };
    }

    /**
     * Snapshot of an entity's position and expanded bounding box at a specific
     * moment.
     */
    public static class AttackData {
        private final EntityLivingBase entity;
        private final AxisAlignedBB box;
        private final double x;
        private final double y;
        private final double z;

        public AttackData(EntityLivingBase entity) {
            this.entity = entity;
            double collisionBorderSize = entity.getCollisionBorderSize();
            this.box = entity.getEntityBoundingBox()
                    .expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
            this.x = entity.posX;
            this.y = entity.posY;
            this.z = entity.posZ;
        }

        public EntityLivingBase getEntity() {
            return entity;
        }

        public AxisAlignedBB getBox() {
            return box;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }
}
