package com.diozero.ws281xj;

public class PixelAnimations {
	public static void delay(int wait) {
		try { Thread.sleep(wait); }  catch (InterruptedException e) { }
	}
	
	public static void colourWipe(WS281x ws281x, int colour, int wait) {
		for (int i=0; i<ws281x.getNumPixels(); i++) {
			ws281x.setPixelColour(i, colour);
			ws281x.render();
			delay(wait);
		}
	}
	
	public static void rainbow(WS281x ws281x, int wait) {
		for (int j=0; j<256; j++) {
			for (int i=0; i<ws281x.getNumPixels(); i++) {
				ws281x.setPixelColour(i, PixelColour.wheel((i+j) & 255));
			}
			ws281x.render();
			delay(wait);
		}
	}
	
	/** Slightly different, this makes the rainbow equally distributed throughout */
	public static void rainbowCycle(WS281x ws281x, int wait) {
		for (int j=0; j<256*5; j++) { // 5 cycles of all colors on wheel
			for (int i=0; i<ws281x.getNumPixels(); i++) {
				ws281x.setPixelColour(i, PixelColour.wheel(((i * 256 / ws281x.getNumPixels()) + j) & 255));
			}
			ws281x.render();
			delay(wait);
		}
	}
	
	/** Theatre-style crawling lights. */
	public static void theatreChase(WS281x ws281x, int c, int wait) {
		for (int j=0; j<10; j++) {  //do 10 cycles of chasing
			for (int q=0; q < 3; q++) {
				for (int i=0; i < ws281x.getNumPixels(); i=i+3) {
					ws281x.setPixelColour(i+q, c);    //turn every third pixel on
				}
				ws281x.render();

				delay(wait);

				for (int i=0; i < ws281x.getNumPixels(); i=i+3) {
					ws281x.setPixelColour(i+q, 0);        //turn every third pixel off
				}
			}
		}
	}

	/** Theatre-style crawling lights with rainbow effect */
	public static void theatreChaseRainbow(WS281x ws281x, int wait) {
		for (int j=0; j < 256; j++) {     // cycle all 256 colours in the wheel
			for (int q=0; q < 3; q++) {
				for (int i=0; i < ws281x.getNumPixels(); i=i+3) {
					ws281x.setPixelColour(i+q, PixelColour.wheel( (i+j) % 255));    //turn every third pixel on
				}
				ws281x.render();

				delay(wait);

				for (int i=0; i < ws281x.getNumPixels(); i=i+3) {
					ws281x.setPixelColour(i+q, 0);        //turn every third pixel off
				}
			}
		}
	}	
}
