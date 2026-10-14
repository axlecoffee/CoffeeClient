package coffee.axle.coffeeclient.feature.combat;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.property.properties.ModeProperty;
import coffee.axle.coffeeclient.rotation.AimAssistRotation;
import coffee.axle.coffeeclient.rotation.RotationMath;
import coffee.axle.coffeeclient.rotation.RotationState;
import coffee.axle.coffeeclient.util.ItemUtil;
import coffee.axle.coffeeclient.util.KeyBindUtil;
import coffee.axle.coffeeclient.util.RotationUtil;
import coffee.axle.coffeeclient.util.TeamUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long ATTACK_COOLDOWN_MILLIS = 350L;

    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 10.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 10.0F);
    public final IntProperty smoothing = new IntProperty("smoothing", 50, 0, 100);
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);
    public final BooleanProperty allowMining = new BooleanProperty("allow-mining", true);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, weaponsOnly::getValue);
    public final BooleanProperty playersOnly = new BooleanProperty("players-only", true);
    public final BooleanProperty targetInvisible = new BooleanProperty("target-invisible", false);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);
    public final ModeProperty aimMode = new ModeProperty("aim-mode", 0, AimAssistRotation.MODE_NAMES);
    public final FloatProperty hitboxBounds = new FloatProperty("hitbox-bounds", 0.95F, 0.05F, 1.5F);
    public final BooleanProperty extrapolation = new BooleanProperty("extrapolation", false);
    public final BooleanProperty pingComp = new BooleanProperty("ping-comp", false);
    public final FloatProperty contractionMin = new FloatProperty("contraction-min", 0.03F, 0.01F, 0.15F);
    public final FloatProperty contractionMax = new FloatProperty("contraction-max", 0.22F, 0.10F, 0.30F);

    private final RotationState rotationState = RotationState.getInstance();
    private AimAssistRotation rotation;
    private EntityLivingBase currentTarget;
    private long lastAttackAt;

    public AimAssist() {
        super("AimAssist", false);
    }

    public EntityLivingBase getCurrentTarget() {
        return currentTarget;
    }

    @Override
    public void onEnabled() {
        rotationState.register();
        if (mc.thePlayer != null) {
            rotationState.setServerAngles(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        rotation = new AimAssistRotation(rotationState);
    }

    @Override
    public void onDisabled() {
        rotation = null;
        currentTarget = null;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    /**
     * Reads the physical input state (Mouse.isButtonDown / Keyboard.isKeyDown)
     * via KeyBindUtil, which is independent of AutoClicker's keybind manipulation.
     */
    private boolean isAttackKeyDown() {
        return KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    private void syncRotationSettings() {
        rotation.setMode(aimMode.getValue());
        rotation.setHitboxBounds(0.05f, hitboxBounds.getValue());
        rotation.setExtrapolationEnabled(extrapolation.getValue());
        rotation.setPingCompensation(pingComp.getValue());
        rotation.setContractionRange(contractionMin.getValue(), contractionMax.getValue());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            currentTarget = null;
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        boolean attackPressed = isAttackKeyDown();
        AutoClicker autoClicker = (AutoClicker) CoffeeClient.featureManager.getFeature(AutoClicker.class);
        if (attackPressed && !autoClicker.isEnabled()) {
            lastAttackAt = System.currentTimeMillis();
        }

        currentTarget = findTarget();

        if (weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant()) {
            if (!allowTools.getValue() || !ItemUtil.isHoldingTool()) {
                return;
            }
        }

        if (allowMining.getValue() && attackPressed && isLookingAtBlock()) {
            return;
        }

        if (requirePress.getValue() && !attackPressed
                && System.currentTimeMillis() - lastAttackAt >= ATTACK_COOLDOWN_MILLIS) {
            return;
        }

        if (currentTarget == null || RotationUtil.distanceToEntity(currentTarget) <= 0.0) {
            return;
        }

        rotationState.captureClientAngles();
        rotationState.updatePing();
        syncRotationSettings();

        applyRotation(currentTarget);
    }

    private EntityLivingBase findTarget() {
        List<EntityLivingBase> inRange = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase) entity)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                .collect(Collectors.toList());

        if (inRange.isEmpty()) {
            return null;
        }

        if (inRange.stream().anyMatch(this::isInReach)) {
            inRange.removeIf(e -> !isInReach(e));
        }

        return inRange.get(0);
    }

    private void applyRotation(EntityLivingBase target) {
        float smooth = smoothing.getValue() / 100.0f;
        float hSpd = hSpeed.getValue();
        float vSpd = vSpeed.getValue();

        AimAssistRotation.AimMode currentMode = AimAssistRotation.AimMode.values()[Math.min(aimMode.getValue(),
                AimAssistRotation.AimMode.values().length - 1)];

        if (currentMode == AimAssistRotation.AimMode.DEFAULT) {
            
            applyDefaultRotation(target, smooth, hSpd, vSpd);
        } else {
            
            float[] result = rotation.aimAtEntity(target, smooth, hSpd, vSpd, true);
            if (result != null) {
                mc.thePlayer.rotationYaw = result[0];
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(result[1], -90.0f, 90.0f);
            }
        }
    }

    private void applyDefaultRotation(EntityLivingBase target, float smooth,
            float hSpd, float vSpd) {
        AxisAlignedBB bb = target.getEntityBoundingBox();
        double border = target.getCollisionBorderSize();
        AxisAlignedBB expandedBox = bb.expand(border, border, border);

        float verticalMultipoint = RotationMath.computeVerticalMultipoint(target.posY);

        float[] rotations = RotationUtil.getRotationsToBoxDynamic(
                expandedBox,
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch,
                180.0f,
                smooth,
                verticalMultipoint);

        float yaw = Math.min(Math.abs(hSpd), 10.0f);
        float pitch = Math.min(Math.abs(vSpd), 10.0f);

        mc.thePlayer.rotationYaw += (rotations[0] - mc.thePlayer.rotationYaw) * 0.1f * yaw;
        mc.thePlayer.rotationPitch += (rotations[1] - mc.thePlayer.rotationPitch) * 0.1f * pitch;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0f, 90.0f);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof EntityLivingBase))
            return false;
        EntityLivingBase living = (EntityLivingBase) entity;

        if (living == mc.thePlayer || living == mc.thePlayer.ridingEntity)
            return false;
        if (living == mc.getRenderViewEntity() || living == mc.getRenderViewEntity().ridingEntity)
            return false;
        if (living.deathTime > 0)
            return false;

        if (playersOnly.getValue() && !(living instanceof EntityPlayer))
            return false;
        if (!targetInvisible.getValue() && living.isInvisible())
            return false;

        if (RotationUtil.distanceToEntity(living) > range.getValue())
            return false;
        if (RotationUtil.angleToEntity(living) > (float) fov.getValue())
            return false;
        if (RotationUtil.rayTrace(living) != null)
            return false;

        if (living instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) living;
            if (TeamUtil.isFriend(player))
                return false;
            if (TeamUtil.isEnemy(player))
                return true;
            if (team.getValue() && TeamUtil.isSameTeam(player))
                return false;
            return !botCheck.getValue() || !TeamUtil.isBot(player);
        }

        return true;
    }

    private boolean isInReach(EntityLivingBase entity) {
        return RotationUtil.distanceToEntity(entity) <= 3.0;
    }
}
