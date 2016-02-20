package com.diozero.ws281xj;

import java.awt.Color;

/**
 * 0x00RRGGBB
 */
public class PixelColour {
	public static final int RED				= 0x00200000;
	public static final int ORANGE			= 0x00201000;
	public static final int YELLOW			= 0x00202000;
	public static final int GREEN			= 0x00002000;
	public static final int LIGHT_BLUE		= 0x00002020;
	public static final int BLUE			= 0x00000020;
	public static final int PURPLE			= 0x00100010;
	public static final int PINK			= 0x00200010;
	
	public static final int[] RAINBOW = { PixelColour.RED, PixelColour.ORANGE, PixelColour.YELLOW, PixelColour.GREEN,
			PixelColour.LIGHT_BLUE, PixelColour.BLUE, PixelColour.PURPLE, PixelColour.PINK };
	
	private static final int WHITE_MASK		= 0x00ffffff;
	private static final int RED_MASK		= 0x00ff0000;
	private static final int GREEN_MASK		= 0x0000ff00;
	private static final int BLUE_MASK		= 0x000000ff;
	
	private static final int RED_OFF_MASK	= 0x0000ffff;
	private static final int GREEN_OFF_MASK	= 0x00ff00ff;
	private static final int BLUE_OFF_MASK	= 0x00ffff00;
	
	public static final int COLOUR_COMPONENT_MAX = 0xff;
	
	/**
	 * Input a value 0 to 255 to get a colour value.
	 * The colours are a transition r - g - b - back to r.
	 */
	public static int wheel(int wheelPos) {
		int max = COLOUR_COMPONENT_MAX;
		int one_third = COLOUR_COMPONENT_MAX/3;
		int two_thirds = COLOUR_COMPONENT_MAX*2/3;
		
		int wheel_pos = max - wheelPos;
		if (wheel_pos < one_third) {
			return createColourRGB(max - wheel_pos * 3, 0, wheel_pos * 3);
		}
		if (wheel_pos < two_thirds) {
			wheel_pos -= one_third;
			return createColourRGB(0, wheel_pos * 3, max - wheel_pos * 3);
		}
		wheel_pos -= two_thirds;
		return createColourRGB(wheel_pos * 3, max - wheel_pos * 3, 0);
	}

	/**
	 * Create a colour from relative RGB values
	 * @param red Red %, {@code 0 to 1}
	 * @param green Green %, {@code 0 to 1}
	 * @param blue Blue %, {@code 0 to 1}
	 * @return RGB colour integer value
	 */
	public static int createColourRGB(float red, float green, float blue) {
		return createColourRGB((int)(COLOUR_COMPONENT_MAX*red),
				(int)(COLOUR_COMPONENT_MAX*green), (int)(COLOUR_COMPONENT_MAX*blue));
	}

	/**
	 * Create a colour from int RGB values
	 * @param red Red component {@code 0 to 255}
	 * @param green Green component {@code 0 to 255}
	 * @param blue Blue component {@code 0 to 255}
	 * @return RGB colour integer value
	 */
	public static int createColourRGB(int red, int green, int blue) {
		validateColourComponent("Red", red);
		validateColourComponent("Green", green);
		validateColourComponent("Blue", blue);
		return red<<16 | green << 8 | blue;
	}

    /**
     * Creates a colour based on the specified values in the HSB colour model.
     *
     * @param hue The hue, in degrees, {@code 0.0 to 360.0}
     * @param saturation The saturation %, {@code 0.0 to 1.0}
     * @param brightness The brightness %, {@code 0.0 to 1.0}
     * @return RGB colour integer value
     * @throws IllegalArgumentException if {@code hue}, {@code saturation}, {@code brightness} are out of range
     */
	public static int createColourHSB(float hue, float saturation, float brightness) {
		// Take advantage of Hue Saturation Brightness utility method in java.awt.Color
		return Color.HSBtoRGB(hue, saturation, brightness) & WHITE_MASK;
	}

    /**
     * Creates a colour based on the specified values in the HSL coluor model.
     *
     * @param hue The hue, in degrees, {@code 0.0 to 360.0}
     * @param saturation The saturation %, {@code 0.0 to 1.0}
     * @param luminance The luminance %, {@code 0.0 to 1.0}
     * @return RGB colour integer value
     * @throws IllegalArgumentException if {@code hue}, {@code saturation}, {@code brightness} are out of range
     */
	public static int createColourHSL(float hue, float saturation, float luminance) {
		// Hue Saturation Luminance - see https://tips4java.wordpress.com/2009/07/05/hsl-color/
		// Or javafx
		//javafx.scene.paint.Color c = javafx.scene.paint.Color.web("hsl(270,100%,100%)");// blue as an hsl web value, implicit alpha
		
		if (saturation < 0.0f || saturation > 1.0f) {
			String message = "Color parameter outside of expected range - Saturation";
			throw new IllegalArgumentException(message);
		}

		if (luminance < 0.0f || luminance > 1.0f) {
			String message = "Color parameter outside of expected range - Luminance";
			throw new IllegalArgumentException(message);
		}

		// Formula needs all values between 0 - 1.
		hue = hue % 360.0f;
		hue /= 360f;

		float q = 0;

		if (luminance < 0.5)
			q = luminance * (1 + saturation);
		else
			q = (luminance + saturation) - (saturation * luminance);

		float p = 2 * luminance - q;

		float r = Math.max(0, HueToRGB(p, q, hue + (1.0f / 3.0f)));
		float g = Math.max(0, HueToRGB(p, q, hue));
		float b = Math.max(0, HueToRGB(p, q, hue - (1.0f / 3.0f)));

		r = Math.min(r, 1.0f);
		g = Math.min(g, 1.0f);
		b = Math.min(b, 1.0f);
		
		return createColourRGB(r, g, b);
	}

	private static float HueToRGB(float p, float q, float h) {
		if (h < 0)
			h += 1;

		if (h > 1)
			h -= 1;

		if (6 * h < 1) {
			return p + ((q - p) * 6 * h);
		}

		if (2 * h < 1) {
			return q;
		}

		if (3 * h < 2) {
			return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
		}

		return p;
	}
	
	public static void validateColourComponent(String colour, int value) {
		if (value < 0 || value >= 256) {
			throw new IllegalArgumentException("Illegal colour value (" + value +
					") for '" + colour + "' - must be 0.." + COLOUR_COMPONENT_MAX);
		}
	}

	public static int getRedComponent(int colour) {
		return (colour & RED_MASK) >> 16;
	}
	
	public static int setRedComponent(final int colour, int red) {
		validateColourComponent("Red", red);
		int new_colour = colour & RED_OFF_MASK;
		new_colour |= red << 16;
		return new_colour;
	}
	
	public static int getGreenComponet(int colour) {
		return (colour & GREEN_MASK) >> 8;
	}
	
	public static int setGreenComponent(final int colour, int green) {
		validateColourComponent("Green", green);
		int new_colour = colour & GREEN_OFF_MASK;
		new_colour |= green << 8;
		return new_colour;
	}
	
	public static int getBlueComponent(int colour) {
		return colour & BLUE_MASK;
	}

	public static int setBlueComponent(final int colour, int blue) {
		validateColourComponent("Blue", blue);
		int new_colour = colour & BLUE_OFF_MASK;
		new_colour |= blue;
		return new_colour;
	}
}
