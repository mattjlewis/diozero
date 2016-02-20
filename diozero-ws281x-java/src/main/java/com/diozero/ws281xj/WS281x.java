package com.diozero.ws281xj;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class WS281x implements Closeable {
	private static final int SIZE_OF_INT = 4;
	private static final int DEFAULT_FREQUENCY = 800_000;	// Or 400_000
	// TODO Find out what options there are here... What do pigpio & wiringPi use?
	private static final int DEFAULT_DMA_NUM = 5;

	private static final String LIB_NAME = "ws281x";
	private static Boolean loaded = Boolean.FALSE;
	private static void init() {
		synchronized (loaded) {
			if (!loaded.booleanValue()) {
				try {
					Path path = Files.createTempFile("lib" + LIB_NAME, ".so");
					path.toFile().deleteOnExit();
					Files.copy(WS281x.class.getResourceAsStream("/lib/lib" + LIB_NAME + ".so"),
							path, StandardCopyOption.REPLACE_EXISTING);
					System.load(path.toString());
					loaded = Boolean.TRUE;
				} catch (IOException e) {
					System.out.println("Error loading library from classpath: " + e);
					e.printStackTrace();
					
					// Try load the usual way...
					System.loadLibrary(LIB_NAME);
					loaded = Boolean.TRUE;
				}
			}
		}
	}
	
	private ByteBuffer ch0LedBuffer;
	private int numPixels;

	public WS281x(int gpioNum, int brightness, int numPixels) {
		this(DEFAULT_FREQUENCY, DEFAULT_DMA_NUM, gpioNum, brightness, numPixels);
	}

	public WS281x(int frequency, int dmaNum, int gpioNum, int brightness, int numPixels) {
		init();
		
		this.numPixels = numPixels;
		
		ch0LedBuffer = WS281xNative.initialise(frequency, dmaNum, gpioNum, brightness, numPixels);
		System.out.println("order=" + ch0LedBuffer.order());
		ch0LedBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		Runtime.getRuntime().addShutdownHook(new Thread(this::close));
	}

	@Override
	public void close() {
		System.out.println("close()");
		allOff();
		ch0LedBuffer = null;
		WS281xNative.terminate();
	}
	
	public int getNumPixels() {
		return numPixels;
	}
	
	@SuppressWarnings("static-method")
	public void render() {
		int rc = WS281xNative.render();
		if (rc != 0) {
			throw new RuntimeException("Error in render() - " + rc);
		}
	}
	
	public void testBufferOutOfBoundsError() {
		int pixel = numPixels+1;
		try {
			// This should fail with index out of bounds error...
			int colour = ch0LedBuffer.getInt(pixel*SIZE_OF_INT);
			System.out.format("led[%d]=0x%08x%n", Integer.valueOf(pixel), Integer.valueOf(colour));
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Error: " + e);
		}
	}
	
	public void validatePixel(int pixel) {
		if (pixel < 0 || pixel >= numPixels) {
			throw new IllegalArgumentException("pixel must be 0.." + (numPixels-1));
		}
	}
	
	public void allOff() {
		for (int i=0; i<numPixels; i++) {
			setPixelColour(i, 0);
		}
		render();
	}
	
	public int getPixelColour(int pixel) {
		validatePixel(pixel);
		return ch0LedBuffer.getInt(pixel*SIZE_OF_INT);
	}
	
	public void setPixelColour(int pixel, int colour) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel*SIZE_OF_INT, colour);
	}

	public void setPixelColourRGB(int pixel, int red, int green, int blue) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel*SIZE_OF_INT, PixelColour.createColourRGB(red, green, blue));
	}

	public void setPixelColourHSB(int pixel, float hue, float saturation, float brightness) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel*SIZE_OF_INT, PixelColour.createColourHSB(hue, saturation, brightness));
	}

	public void setPixelColourHSL(int pixel, float hue, float saturation, float luminance) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel*SIZE_OF_INT, PixelColour.createColourHSL(hue, saturation, luminance));
	}

	public int getRedComponent(int pixel) {
		validatePixel(pixel);
		return PixelColour.getRedComponent(ch0LedBuffer.getInt(pixel*SIZE_OF_INT));
	}

	public void setRedComponent(int pixel, int red) {
		validatePixel(pixel);
		int index = pixel*SIZE_OF_INT;
		ch0LedBuffer.putInt(index, PixelColour.setRedComponent(ch0LedBuffer.getInt(index), red));
	}
	
	public int getGreenComponent(int pixel) {
		validatePixel(pixel);
		return PixelColour.getGreenComponet(ch0LedBuffer.getInt(pixel*SIZE_OF_INT));
	}

	public void setGreenComponent(int pixel, int green) {
		validatePixel(pixel);
		int index = pixel*SIZE_OF_INT;
		ch0LedBuffer.putInt(index, PixelColour.setGreenComponent(ch0LedBuffer.getInt(index), green));
	}
	
	public int getBlueComponent(int pixel) {
		validatePixel(pixel);
		return PixelColour.getBlueComponent(ch0LedBuffer.getInt(pixel*SIZE_OF_INT));
	}

	public void setBlueComponent(int pixel, int blue) {
		validatePixel(pixel);
		int index = pixel*SIZE_OF_INT;
		ch0LedBuffer.putInt(index, PixelColour.setBlueComponent(ch0LedBuffer.getInt(index), blue));
	}
}
