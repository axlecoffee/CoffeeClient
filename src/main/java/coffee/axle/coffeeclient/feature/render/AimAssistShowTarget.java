package coffee.axle.coffeeclient.feature.render;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.feature.combat.AimAssist;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.property.properties.ModeProperty;
import coffee.axle.coffeeclient.util.ItemUtil;
import coffee.axle.coffeeclient.util.RenderUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class AimAssistShowTarget extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final Color hurtColor = new Color(16733525);
    private final Color normalColor = new Color(5635925);

    public final ModeProperty showTarget = new ModeProperty("show-target", 1,
            new String[] { "NONE", "DEFAULT", "HUD", "BOX" });
    public final FloatProperty boxOutlineWidth = new FloatProperty("target-box-width", 2.0f, 1.0f, 5.0f);

    public AimAssistShowTarget() {
        super("AimAssistShowTarget", false);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) {
            return;
        }

        AimAssist aimAssist = (AimAssist) CoffeeClient.featureManager.getFeature(AimAssist.class);
        if (aimAssist == null || !aimAssist.isEnabled()) {
            return;
        }

        if (!checkWeaponRequirement(aimAssist)) {
            return;
        }

        int mode = showTarget.getValue();
        if (mode == 0) {
            return;
        }

        EntityLivingBase target = aimAssist.getCurrentTarget();
        if (target == null || target.isDead || target.getHealth() <= 0.0f) {
            return;
        }

        Color color;
        if (mode == 1 || mode == 3) {
            color = target.hurtTime > 0 ? hurtColor : normalColor;
        } else {
            HUD hud = (HUD) CoffeeClient.featureManager.getFeature(HUD.class);
            color = hud != null ? hud.getColor(System.currentTimeMillis()) : normalColor;
        }

        double x = RenderUtil.interpolate(target.posX, target.lastTickPosX, event.partialTicks);
        double y = RenderUtil.interpolate(target.posY, target.lastTickPosY, event.partialTicks);
        double z = RenderUtil.interpolate(target.posZ, target.lastTickPosZ, event.partialTicks);
        AxisAlignedBB box = target.getEntityBoundingBox()
                .expand(0.1f, 0.1f, 0.1f)
                .offset(x - target.posX, y - target.posY, z - target.posZ)
                .offset(-mc.getRenderManager().viewerPosX,
                        -mc.getRenderManager().viewerPosY,
                        -mc.getRenderManager().viewerPosZ);

        RenderUtil.enableRenderState();
        if (mode == 3) {
            RenderUtil.drawBoundingBox(box, color, boxOutlineWidth.getValue());
        } else {
            RenderUtil.drawFilledBox(box, color.getRed(), color.getGreen(), color.getBlue(), 38);
        }
        RenderUtil.disableRenderState();
    }

    private boolean checkWeaponRequirement(AimAssist aimAssist) {
        return !aimAssist.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || (aimAssist.allowTools.getValue() && ItemUtil.isHoldingTool());
    }
}
