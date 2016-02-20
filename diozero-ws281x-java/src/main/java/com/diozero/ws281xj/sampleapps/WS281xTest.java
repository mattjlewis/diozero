package com.diozero.ws281xj.sampleapps;

import com.diozero.ws281xj.PixelAnimations;
import com.diozero.ws281xj.PixelColour;
import com.diozero.ws281xj.WS281x;

public class WS281xTest {
	public static void main(String[] args) {
		int gpio_num = 18;
		int brightness = 64;	// 0..255
		int num_pixels = 12;
		
		try (WS281x ws281x = new WS281x(gpio_num, brightness, num_pixels)) {
			rainbowColours(ws281x);
			test2(ws281x);
			
			while (true) {
				loop(ws281x);
			}
		} catch (Throwable t) {
			System.out.println("Error: " + t);
			t.printStackTrace();
		}
	}
	
	private static void rainbowColours(WS281x ws281x) {
		System.out.println("rainbowColours()");
		
		int[] colours = PixelColour.RAINBOW;
		
		for (int i=0; i<400; i++) {
			for (int pixel=0; pixel<ws281x.getNumPixels(); pixel++) {
				ws281x.setPixelColour(pixel, colours[(i+pixel) % colours.length]);
			}
			
			ws281x.render();
			PixelAnimations.delay(50);
		}
	}
	
	private static void test2(WS281x ws281x) {
		System.out.println("test2()");
		
		// Set all off
		ws281x.allOff();
		
		int delay = 20;

		// Gradually add red
		System.out.println("Adding red...");
		for (int i=0; i<256; i++) {
			for (int pixel=0; pixel<ws281x.getNumPixels(); pixel++) {
				ws281x.setRedComponent(pixel, i);
			}
			
			ws281x.render();
			PixelAnimations.delay(delay);
		}

		// Gradually add green
		System.out.println("Adding green...");
		for (int i=0; i<256; i++) {
			for (int pixel=0; pixel<ws281x.getNumPixels(); pixel++) {
				ws281x.setGreenComponent(pixel, i);
			}
			
			ws281x.render();
			PixelAnimations.delay(delay);
		}

		// Gradually add blue
		System.out.println("Adding blue...");
		for (int i=0; i<256; i++) {
			for (int pixel=0; pixel<ws281x.getNumPixels(); pixel++) {
				ws281x.setGreenComponent(pixel, i);
			}
			
			ws281x.render();
			PixelAnimations.delay(delay);
		}
		
		// Set all off
		ws281x.allOff();
	}
	
	private static void loop(WS281x ws281x) {
		System.out.println("loop()");
		
		// Some example procedures showing how to display to the pixels:
		System.out.println("colourWipe - red");
		PixelAnimations.colourWipe(ws281x, PixelColour.createColourRGB(255, 0, 0), 50); // Red
		System.out.println("colourWipe - green");
		PixelAnimations.colourWipe(ws281x, PixelColour.createColourRGB(0, 255, 0), 50); // Green
		System.out.println("colourWipe - blue");
		PixelAnimations.colourWipe(ws281x, PixelColour.createColourRGB(0, 0, 255), 50); // Blue
		//PixelAnimations.colourWipe(PixelColour.createColour(0, 0, 0, 255), 50); // White RGBW
		// Send a theatre pixel chase in...
		System.out.println("theatreChase - white");
		PixelAnimations.theatreChase(ws281x, PixelColour.createColourRGB(127, 127, 127), 50); // White
		System.out.println("theatreChase - red");
		PixelAnimations.theatreChase(ws281x, PixelColour.createColourRGB(127, 0, 0), 50); // Red
		System.out.println("theatreChase - blue");
		PixelAnimations.theatreChase(ws281x, PixelColour.createColourRGB(0, 0, 127), 50); // Blue
		
		System.out.println("rainbow");
		PixelAnimations.rainbow(ws281x, 20);
		System.out.println("rainbowCycle");
		PixelAnimations.rainbowCycle(ws281x, 20);
		System.out.println("theatreChaseRainbow");
		PixelAnimations.theatreChaseRainbow(ws281x, 50);
	}
}
