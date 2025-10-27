package io.github.moulberry.notenoughupdates.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderUtil {

	private static final Minecraft mc = Minecraft.getMinecraft();
	private static final Frustum cameraFrustum = new Frustum();

	public static double interpolate(double current, double last, float partialTicks) {
		return last + (current - last) * partialTicks;
	}

	public static void drawBoundingBox(AxisAlignedBB box, Color color, float lineWidth) {
		drawBoundingBox(box, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), lineWidth);
	}

	public static void drawBoundingBox(AxisAlignedBB box, int red, int green, int blue, int alpha, float lineWidth) {
		GL11.glLineWidth(lineWidth);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		RenderGlobal.drawOutlinedBoundingBox(box, red, green, blue, alpha);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2.0F);
	}

	public static void drawBlockBoundingBox(AxisAlignedBB box, Color color, float lineWidth) {
		drawBoundingBox(box, color, lineWidth);
	}

	public static void drawFilledBox(AxisAlignedBB box, Color color) {
		drawFilledBox(box, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	public static void drawFilledBox(AxisAlignedBB box, int red, int green, int blue) {
		drawFilledBox(box, red, green, blue, 63);
	}

	public static void drawFilledBox(AxisAlignedBB box, int red, int green, int blue, int alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();

		worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

		worldrenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();

		tessellator.draw();
	}

	public static void drawFramebuffer(Framebuffer framebuffer) {
		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		GlStateManager.bindTexture(framebuffer.framebufferTexture);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2d(0.0, 1.0);
		GL11.glVertex2d(0.0, 0.0);
		GL11.glTexCoord2d(0.0, 0.0);
		GL11.glVertex2d(0.0, scaledResolution.getScaledHeight());
		GL11.glTexCoord2d(1.0, 0.0);
		GL11.glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
		GL11.glTexCoord2d(1.0, 1.0);
		GL11.glVertex2d(scaledResolution.getScaledWidth(), 0.0);
		GL11.glEnd();
	}

	public static void enableRenderState() {
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableTexture2D();
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		GlStateManager.disableDepth();
	}

	public static void disableRenderState() {
		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.enableCull();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	public static void drawOutlinedString(String text, float x, float y) {
		String stripped = text.replaceAll("(?i)§[\\da-f]", "");
		mc.fontRendererObj.drawString(stripped, x + 1.0f, y, 0, false);
		mc.fontRendererObj.drawString(stripped, x - 1.0f, y, 0, false);
		mc.fontRendererObj.drawString(stripped, x, y + 1.0f, 0, false);
		mc.fontRendererObj.drawString(stripped, x, y - 1.0f, 0, false);
		mc.fontRendererObj.drawString(text, x, y, -1, false);
	}

	public static void drawBlockBox(BlockPos blockPos, double height, int red, int green, int blue) {
		AxisAlignedBB box = new AxisAlignedBB(
				blockPos.getX(),
				blockPos.getY(),
				blockPos.getZ(),
				(double) blockPos.getX() + 1.0,
				(double) blockPos.getY() + height,
				(double) blockPos.getZ() + 1.0).offset(
						-mc.getRenderManager().viewerPosX,
						-mc.getRenderManager().viewerPosY,
						-mc.getRenderManager().viewerPosZ);
		drawFilledBox(box, new Color(red, green, blue, 63));
	}

	public static void drawBlockBoundingBox(BlockPos blockPos, double height, int red, int green, int blue, int alpha,
			float lineWidth) {
		AxisAlignedBB box = new AxisAlignedBB(
				blockPos.getX(),
				blockPos.getY(),
				blockPos.getZ(),
				(double) blockPos.getX() + 1.0,
				(double) blockPos.getY() + height,
				(double) blockPos.getZ() + 1.0).offset(
						-mc.getRenderManager().viewerPosX,
						-mc.getRenderManager().viewerPosY,
						-mc.getRenderManager().viewerPosZ);

		GL11.glLineWidth(lineWidth);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		RenderGlobal.drawOutlinedBoundingBox(box, red, green, blue, alpha);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2.0f);
	}

	public static boolean isInViewFrustum(AxisAlignedBB box, double expand) {
		cameraFrustum.setPosition(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY,
				mc.getRenderViewEntity().posZ);
		return cameraFrustum.isBoundingBoxInFrustum(box.expand(expand, expand, expand));
	}

	public static double lerpDouble(double current, double previous, double partialTicks) {
		return previous + (current - previous) * partialTicks;
	}

	public static void setColor(int argb) {
		float alpha = (float) (argb >> 24 & 0xFF) / 255.0f;
		float red = (float) (argb >> 16 & 0xFF) / 255.0f;
		float green = (float) (argb >> 8 & 0xFF) / 255.0f;
		float blue = (float) (argb & 0xFF) / 255.0f;
		GlStateManager.color(red, green, blue, alpha);
	}

	public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
		float angle1 = angle + (float) Math.toRadians(45.0);
		float angle2 = angle - (float) Math.toRadians(45.0);
		setColor(color);
		GL11.glLineWidth(lineWidth);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2f(centerX, centerY);
		GL11.glVertex2f(centerX + length * (float) Math.cos(angle1), centerY + length * (float) Math.sin(angle1));
		GL11.glVertex2f(centerX, centerY);
		GL11.glVertex2f(centerX + length * (float) Math.cos(angle2), centerY + length * (float) Math.sin(angle2));
		GL11.glEnd();
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2.0f);
		GlStateManager.resetColor();
	}
}
