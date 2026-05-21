package coffee.axle.coffeeclient.feature.render.bedplates;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BedBillboardRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final double DEFAULT_BILLBOARD_SCALE = 0.01666666753590107D;
    public static final int DEFAULT_ITEM_SIZE = 16;
    private static final float ITEM_PADDING = 2.0f;
    private static final double CORNER_RADIUS = 3.0;
    private static final int CORNER_SEGMENTS = 6;
    private static final int DEFAULT_BG_COLOR = 0x5A000000;

    public static final int BG_NONE = 0;
    public static final int BG_DEFAULT = 1;
    public static final int BG_HUD = 2;
    public static final int BG_CUSTOM = 3;

    private BedBillboardRenderer() {
    }

    public static void draw(
            Map<Block, Integer> layerMap,
            double centerX, double centerY, double centerZ,
            double scaleFactor, Color borderColor,
            float borderThickness, int itemSize,
            int bgMode, Color bgColor) {

        List<Map.Entry<Block, Integer>> sorted = new ArrayList<>(layerMap.entrySet());
        sorted.sort((a, b) -> Integer.compare(
                b.getValue(), a.getValue()));

        int count = sorted.size();
        if (count == 0) return;

        float cellSize = itemSize + ITEM_PADDING;
        float rotateX = mc.gameSettings.thirdPersonView == 2
                ? -1.0F : 1.0F;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTranslatef(
                (float) (centerX - mc.getRenderManager().viewerPosX),
                (float) (centerY - mc.getRenderManager().viewerPosY),
                (float) (centerZ - mc.getRenderManager().viewerPosZ));
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(
                -mc.getRenderManager().playerViewY,
                0.0F, 1.0F, 0.0F);
        GL11.glRotatef(
                mc.getRenderManager().playerViewX,
                rotateX, 0.0F, 0.0F);
        GL11.glScaled(
                -scaleFactor, -scaleFactor, scaleFactor);

        float plateWidth = Math.max(cellSize, count * cellSize);
        float halfWidth = plateWidth / 2f;
        float pad = 4.0f;

        float rx = -halfWidth - pad;
        float ry = -pad;
        float rw = plateWidth + pad * 2;
        float rh = cellSize + pad * 2;

        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, 1f);

        if (bgMode != BG_NONE) {
            int bgArgb = resolveBgColor(bgMode, bgColor);
            drawRoundedRect(
                    rx, ry, rw, rh, CORNER_RADIUS, bgArgb);
        }

        if (borderColor != null) {
            int hudArgb = (180 << 24)
                    | (borderColor.getRed() << 16)
                    | (borderColor.getGreen() << 8)
                    | borderColor.getBlue();
            drawRoundedRectOutline(
                    rx, ry, rw, rh,
                    CORNER_RADIUS, hudArgb, borderThickness);
        }
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();

        float offset = -halfWidth + ITEM_PADDING / 2f;
        for (Map.Entry<Block, Integer> entry : sorted) {
            Block block = entry.getKey();
            ItemStack stack = new ItemStack(
                    Item.getItemFromBlock(block), 1, 0);
            renderItem(stack, (int) offset,
                    (int) (ITEM_PADDING / 2f));
            offset += cellSize;
        }

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    public static double computeScale(
            double distance, float maxDist, int mode,
            double billboardScale) {
        double effectiveMax = maxDist <= 0
                ? 1000.0
                : Math.max(maxDist, 10.0);
        double clamped = Math.min(
                Math.max(distance, 4.0), effectiveMax);

        if (mode == 0) {
            return billboardScale * clamped * 0.65;
        } else {
            return billboardScale
                    * (0.6 + 0.4 * (clamped / 80.0))
                    * Math.sqrt(clamped) * 1.3;
        }
    }

    public static Color parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        String clean = hex.startsWith("#")
                ? hex.substring(1) : hex;
        if (clean.length() != 6 && clean.length() != 3) return null;
        try {
            if (clean.length() == 3) {
                char r = clean.charAt(0);
                char g = clean.charAt(1);
                char b = clean.charAt(2);
                clean = "" + r + r + g + g + b + b;
            }
            int rgb = Integer.parseInt(clean, 16);
            return new Color(rgb);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int resolveBgColor(int bgMode, Color bgColor) {
        switch (bgMode) {
            case BG_HUD:
            case BG_CUSTOM:
                if (bgColor != null) {
                    return (0x5A << 24)
                            | (bgColor.getRed() << 16)
                            | (bgColor.getGreen() << 8)
                            | bgColor.getBlue();
                }
                return DEFAULT_BG_COLOR;
            case BG_DEFAULT:
            default:
                return DEFAULT_BG_COLOR;
        }
    }

    private static void renderItem(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);

        mc.getRenderItem().zLevel = -150.0f;
        mc.getRenderItem()
                .renderItemAndEffectIntoGUI(stack, x, y);
        mc.getRenderItem().zLevel = 0.0f;

        GlStateManager.popMatrix();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        GlStateManager.popMatrix();
    }

    private static void drawRoundedRect(
            double x, double y, double w, double h,
            double r, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;

        int ri = (int) (red * 255);
        int gi = (int) (green * 255);
        int bi = (int) (blue * 255);
        int ai = (int) (a * 255);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(
                GL11.GL_TRIANGLE_FAN,
                DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x + w / 2, y + h / 2, 0)
                .color(ri, gi, bi, ai).endVertex();
        addRoundedRectVerticesColored(
                wr, x, y, w, h, r, ri, gi, bi, ai);
        wr.pos(x + w - r, y, 0)
                .color(ri, gi, bi, ai).endVertex();
        t.draw();
    }

    private static void drawRoundedRectOutline(
            double x, double y, double w, double h,
            double r, int color, float lineWidth) {
        float a = (color >> 24 & 0xFF) / 255f;
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;

        int ri = (int) (red * 255);
        int gi = (int) (green * 255);
        int bi = (int) (blue * 255);
        int ai = (int) (a * 255);

        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(
                GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(
                GL11.GL_LINE_LOOP,
                DefaultVertexFormats.POSITION_COLOR);
        addRoundedRectVerticesColored(
                wr, x, y, w, h, r, ri, gi, bi, ai);
        t.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private static void addRoundedRectVerticesColored(
            WorldRenderer wr,
            double x, double y, double w, double h,
            double r, int ri, int gi, int bi, int ai) {
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double a = -Math.PI / 2
                    + (Math.PI / 2) * i / CORNER_SEGMENTS;
            wr.pos(x + w - r + Math.cos(a) * r,
                    y + r + Math.sin(a) * r, 0)
                    .color(ri, gi, bi, ai).endVertex();
        }
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double a = (Math.PI / 2) * i / CORNER_SEGMENTS;
            wr.pos(x + w - r + Math.cos(a) * r,
                    y + h - r + Math.sin(a) * r, 0)
                    .color(ri, gi, bi, ai).endVertex();
        }
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double a = Math.PI / 2
                    + (Math.PI / 2) * i / CORNER_SEGMENTS;
            wr.pos(x + r + Math.cos(a) * r,
                    y + h - r + Math.sin(a) * r, 0)
                    .color(ri, gi, bi, ai).endVertex();
        }
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double a = Math.PI
                    + (Math.PI / 2) * i / CORNER_SEGMENTS;
            wr.pos(x + r + Math.cos(a) * r,
                    y + r + Math.sin(a) * r, 0)
                    .color(ri, gi, bi, ai).endVertex();
        }
    }
}
