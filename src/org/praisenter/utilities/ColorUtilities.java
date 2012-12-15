package org.praisenter.utilities;

import java.awt.Color;

/**
 * Utility class to handle colors.
 * @author William Bittle
 * @version 1.0.0
 * @since 1.0.0
 */
public class ColorUtilities {
	/** A fully transparent color (used for clearing the image) */
	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	
	/**
	 * Uses the method described at http://alienryderflex.com/hsp.html to get
	 * the <u>perceived</u> brightness of a color.
	 * @param color the color
	 * @return int brightness on the scale of 0 to 255
	 */
	public static final int getBrightness(Color color) {
		// original coefficients
		final double cr = 0.241;
		final double cg = 0.691;
		final double cb = 0.068;
		// another set of coefficients
//		final double cr = 0.299;
//		final double cg = 0.587;
//		final double cb = 0.114;
		
		double r, g, b;
		r = color.getRed();
		g = color.getGreen();
		b = color.getBlue();
		
		// compute the weighted distance
		double result = Math.sqrt(cr * r * r + cg * g * g + cb * b * b);
		
		return (int)result;
	}
	
	/**
	 * Returns a foreground color (for text) given a background color by examining
	 * the brightness of the background color.
	 * @param color the foreground color
	 * @return Color
	 */
	public static final Color getForegroundColorFromBackgroundColor(Color color) {
		int brightness = ColorUtilities.getBrightness(color);
		if (brightness < 130) {
			return Color.WHITE;
		} else {
			return Color.BLACK;
		}
	}
	
	/**
	 * Returns a random color given the offset and alpha values.
	 * @param offset the offset between 0.0 and 1.0
	 * @param alpha the alpha value between 0.0 and 1.0
	 * @return Color
	 */
	public static final Color getRandomColor(float offset, float alpha) {
		final float max = 1.0f;
		final float min = 0.0f;
		// make sure the offset is valid
		if (offset > max) offset = min;
		if (offset < min) offset = min;
		// use the offset to calculate the color
		float multiplier = max - offset;
		// compute the rgb values
		float r = (float)Math.random() * multiplier + offset;
		float g = (float)Math.random() * multiplier + offset;
		float b = (float)Math.random() * multiplier + offset;
		
		return new Color(r, g, b, alpha);
	}
	
	/**
	 * Returns a new color that is darker or lighter than the given color
	 * by the given factor.
	 * @param color the color to modify
	 * @param factor 0.0 &le; factor &le; 1.0 darkens; 1.0 &lt; factor brightens
	 * @return Color
	 * @since 1.0.1
	 */
	public static final Color getColor(Color color, float factor) {
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		hsb[2] = hsb[2] * factor;
		return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * factor));
	}
	
	/**
	 * Returns the color at the mid point of the given colors.
	 * @param c1 the first color
	 * @param c2 the second color
	 * @return Color
	 */
	public static final Color getColorAtMidpoint(Color c1, Color c2) {
		int r = (c2.getRed() - c1.getRed()) / 2 + c1.getRed();
		int g = (c2.getGreen() - c1.getGreen()) / 2 + c1.getGreen();
		int b = (c2.getBlue() - c1.getBlue()) / 2 + c1.getBlue();
		int a = (c2.getAlpha() - c1.getAlpha()) / 2 + c1.getAlpha();
		return new Color(r, g, b, a);
	}
}
