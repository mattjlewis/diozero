package com.diozero.ws281xj;

public interface LedDriverInterface extends AutoCloseable {
	@Override
	void close();

	int getNumPixels();
	
	/**
	 * Push any updated colours to the LED strip.
	 */
	void render();
	
	/**
	 * Turn off all pixels and render.
	 */
	void allOff();
	
	/**
	 * Get the current colour for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 24-bit RGB colour value.
	 */
	int getPixelColour(int pixel);
	
	/**
	 * Set the colour for the specified pixel.
	 * @param pixel Pixel number.
	 * @param colour Colour represented as a 24bit RGB integer (0x0RGB).
	 */
	void setPixelColour(int pixel, int colour);
	
	/**
	 * Set the colour for the specified pixel using individual red / green / blue 8-bit values.
	 * @param pixel Pixel number.
	 * @param red 8-bit value for the red component.
	 * @param green 8-bit value for the green component.
	 * @param blue 8-bit value for the blue component.
	 */
	default void setPixelColourRGB(int pixel, int red, int green, int blue) {
		setPixelColour(pixel, PixelColour.createColourRGB(red, green, blue));
	}
	
	/**
	 * Set the colour for the specified pixel using Hue Saturation Brightness (HSB) values.
	 * @param pixel Pixel number.
	 * @param hue Float value in the range 0..1 representing the hue.
	 * @param saturation Float value in the range 0..1 representing the colour saturation.
	 * @param brightness Float value in the range 0..1 representing the colour brightness.
	 */
	default void setPixelColourHSB(int pixel, float hue, float saturation, float brightness) {
		setPixelColour(pixel, PixelColour.createColourHSB(hue, saturation, brightness));
	}
	
	/**
	 * <p>Set the colour for the specified pixel using Hue Saturation Luminance (HSL) values.</p>
	 * <p>HSL colour mapping code taken from <a href="https://tips4java.wordpress.com/2009/07/05/hsl-color/">this HSL Color class by Rob Camick</a>.</p>
	 * @param pixel Pixel number.
	 * @param hue Represents the colour (think colours of the rainbow), specified in degrees from 0 - 360. Red is 0, green is 120 and blue is 240.
	 * @param saturation Represents the purity of the colour. Range is 0..1 with 1 fully saturated and 0 gray.
	 * @param luminance Represents the brightness of the colour. Range is 0..1 with 1 white 0 black.
	 */
	default void setPixelColourHSL(int pixel, float hue, float saturation, float luminance) {
		setPixelColour(pixel, PixelColour.createColourHSL(hue, saturation, luminance));
	}
	
	/**
	 * Get the 8-bit red component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 8-bit red component value.
	 */
	default int getRedComponent(int pixel) {
		return PixelColour.getRedComponent(getPixelColour(pixel));
	}
	
	/**
	 * Set the 8-bit red component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @param red 8-bit red component value.
	 */
	default void setRedComponent(int pixel, int red) {
		setPixelColour(pixel, PixelColour.setRedComponent(getPixelColour(pixel), red));
	}
	
	/**
	 * Get the 8-bit green component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 8-bit green component value.
	 */
	default int getGreenComponent(int pixel) {
		return PixelColour.getGreenComponent(getPixelColour(pixel));
	}
	
	/**
	 * Set the 8-bit green component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @param green 8-bit green component value.
	 */
	default void setGreenComponent(int pixel, int green) {
		setPixelColour(pixel, PixelColour.setGreenComponent(getPixelColour(pixel), green));
	}
	
	/**
	 * Get the 8-bit blue component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 8-bit blue component value.
	 */
	default int getBlueComponent(int pixel) {
		return PixelColour.getBlueComponent(getPixelColour(pixel));
	}
	
	/**
	 * Set the 8-bit blue component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @param blue 8-bit blue component value.
	 */
	default void setBlueComponent(int pixel, int blue) {
		setPixelColour(pixel, PixelColour.setBlueComponent(getPixelColour(pixel), blue));
	}
}
