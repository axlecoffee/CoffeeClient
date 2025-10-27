package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RenderUtil;
import io.github.moulberry.notenoughupdates.mixins.AccessorRenderManager;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TestModule extends Module {

	private static final Minecraft mc = Minecraft.getMinecraft();

	public final BooleanProperty useMixin = new BooleanProperty("use-mixin", false);

	public TestModule() {
		super("Test", false);
	}

	@Override
	public void onEnabled() {
		ChatUtil.sendMessage("&aTest Module Enabled - Diamond Block ESP");
		ChatUtil.sendMessage("&7Use-Mixin: " + (useMixin.getValue() ? "&aON (Green)" : "&cOFF (Magenta)"));
	}

	@SubscribeEvent
	public void onRenderWorld(RenderWorldLastEvent event) {
		if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		RenderUtil.enableRenderState();

		try {
			int range = 50;
			BlockPos playerPos = mc.thePlayer.getPosition();

			for (int x = -range; x <= range; x++) {
				for (int y = -range; y <= range; y++) {
					for (int z = -range; z <= range; z++) {
						BlockPos pos = playerPos.add(x, y, z);
						if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.diamond_block) {
							if (useMixin.getValue()) {
								renderDiamondWithMixin(pos);
							} else {
								renderDiamondWithViewerPos(pos);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			ChatUtil.sendMessage("&cRendering Error: " + e.getMessage());
			e.printStackTrace();
		}

		RenderUtil.disableRenderState();
	}

	private void renderDiamondWithMixin(BlockPos pos) {
		AccessorRenderManager renderManager = (AccessorRenderManager) mc.getRenderManager();

		AxisAlignedBB box = new AxisAlignedBB(
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				pos.getX() + 1.0,
				pos.getY() + 1.0,
				pos.getZ() + 1.0).offset(
						-renderManager.getRenderPosX(),
						-renderManager.getRenderPosY(),
						-renderManager.getRenderPosZ());

		RenderUtil.drawFilledBox(box, 0, 255, 0, 63);
		RenderUtil.drawBoundingBox(box, 0, 255, 0, 255, 2.0F);
	}

	private void renderDiamondWithViewerPos(BlockPos pos) {
		double renderPosX = mc.getRenderManager().viewerPosX;
		double renderPosY = mc.getRenderManager().viewerPosY;
		double renderPosZ = mc.getRenderManager().viewerPosZ;

		AxisAlignedBB box = new AxisAlignedBB(
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				pos.getX() + 1.0,
				pos.getY() + 1.0,
				pos.getZ() + 1.0).offset(
						-renderPosX,
						-renderPosY,
						-renderPosZ);

		RenderUtil.drawFilledBox(box, 255, 0, 255, 63);
		RenderUtil.drawBoundingBox(box, 255, 0, 255, 255, 2.0F);
	}
}
