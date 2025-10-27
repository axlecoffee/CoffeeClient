package io.github.moulberry.notenoughupdates.coffeeclient.util;

import java.awt.Color;

public class ColorUtil {

    public static Color fromHSB(float hue, float saturation, float brightness) {
        return Color.getHSBColor(hue, saturation, brightness);
    }

    public static Color interpolate(float ratio, Color start, Color end) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * ratio);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * ratio);
        return new Color(r, g, b, a);
    }

    public static Color darker(Color color, float factor) {
        return new Color(
                Math.max((int) (color.getRed() * factor), 0),
                Math.max((int) (color.getGreen() * factor), 0),
                Math.max((int) (color.getBlue() * factor), 0),
                color.getAlpha());
    }

    public static Color getHealthBlend(float healthPercent) {
        if (healthPercent >= 0.75f) {
            return interpolate((healthPercent - 0.75f) / 0.25f, Color.YELLOW, Color.GREEN);
        } else if (healthPercent >= 0.5f) {
            return interpolate((healthPercent - 0.5f) / 0.25f, Color.ORANGE, Color.YELLOW);
        } else if (healthPercent >= 0.25f) {
            return interpolate((healthPercent - 0.25f) / 0.25f, Color.RED, Color.ORANGE);
        } else {
            return interpolate(healthPercent / 0.25f, new Color(139, 0, 0), Color.RED);
        }
    }
}
