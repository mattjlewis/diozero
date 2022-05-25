package com.diozero.devices.sandpit;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     TCS3200.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.util.RangeUtil;
import com.diozero.util.SleepUtil;

/**
 * <p>This class reads RGB values from a TCS3200 colour sensor. Connections:</p>
 * <pre>
 * GND   Ground.
 * VDD   Supply Voltage (2.7-5.5V)
 * /OE   Output enable, active low. When OE is high OUT is disabled
 *       allowing multiple sensors to share the same OUT line.
 * OUT   Output frequency square wave.
 * S0/S1 Output frequency scale selection.
 * S2/S3 Colour filter selection.
 * </pre>
 * <ul>
 * <li>OUT is a square wave whose frequency is proportional to the
 * intensity of the selected filter colour.</li>
 * <li>S2/S3 selects between red, green, blue, and no filter.</li>
 * <li>S0/S1 scales the frequency at 100%, 20%, 2% or off.</li>
 * </ul>
 * <p>To take a reading the colour filters are selected in turn for a
 * fraction of a second and the frequency is read and converted to Hz.</p>
 */
public class TCS3200 implements DeviceEventConsumer<DigitalInputEvent>, Runnable, DeviceInterface {
	public static final int NOT_SET = -1;
	
	private DigitalInputDevice out;
	private DigitalOutputDevice s2;
	private DigitalOutputDevice s3;
	private DigitalOutputDevice s0;
	private DigitalOutputDevice s1;
	private DigitalOutputDevice oe;
	
	private int[] rgbBlack;
	private int[] rgbWhite = { 10,000, 10,000, 10,000 };
	private int[] latestHertz;
	private int[] currentHertz;
	private int[] latestTally = { 1, 1, 1 };
	private int[] currentTally = { 1, 1, 1 };
	// Tune delay to get _samples pulses
	private double[] delay = { 0.1, 0.1, 0.1 };
	private int cycle;
	private Frequency frequency;
	private double interval;
	private int samples;
	private long startTick;
	private long lastTick;
	private boolean read;
	
	public TCS3200(int outGpio, int s2Gpio, int s3Gpio) {
		this(outGpio, s2Gpio, s3Gpio, NOT_SET, NOT_SET, NOT_SET);
	}
	
	public TCS3200(int outGpio, int s2Gpio, int s3Gpio, int s0Gpio, int s1Gpio, int oeGpio) {
		out = new DigitalInputDevice(outGpio);
		s2 = new DigitalOutputDevice(s2Gpio);
		s3 = new DigitalOutputDevice(s3Gpio);
		if (s0Gpio != NOT_SET && s1Gpio != NOT_SET) {
			s0 = new DigitalOutputDevice(s0Gpio);
			s1 = new DigitalOutputDevice(s1Gpio);
		}
		if (oeGpio != NOT_SET) {
			// Enable device (active low)
			oe = new DigitalOutputDevice(oeGpio, false, true);
		}
		
		// Disable frequency output
		//out.setValue(false);
		
		setSampleSize(20);
		// One reading per second
		setUpdateInterval(1.0);
		// 2%
		setFrequency(Frequency.TWO_PERCENT);
		// Clear
		setFilter(Filter.CLEAR);
		
		out.addListener(this);
		
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * Get the latest RGB reading. The raw colour hertz readings are converted
	 * to RGB values as follows: RGB = 255 * (Fv - Fb) / (Fw - Fb) Where Fv is
	 * the sampled hertz, Fw is the calibrated white hertz, and Fb is the
	 * calibrated black hertz.
	 * 
	 * @return RGB values constrained to be between 0 and 255
	 */
	public int[] getRgb() {
		int top = 255;
		int[] rgb = new int[3];
		for (int c=0; c<3; c++) {
			int v = latestHertz[c] - rgbBlack[c];
			int s = rgbWhite[c] - rgbBlack[c];
			int p = top * v / s;
			if (p < 0) {
				p = 0;
			} else if (p > top) {
				p = top;
			}
			rgb[c] = p;
		}
		return rgb;
	}
	
	/**
	 * Get the latest hertz reading
	 * @return latest hertz reading
	 */
	public int[] getHertz() {
		return latestHertz;
	}
	
	/**
	 * Get the black level calibration
	 * @return black level calibration
	 */
	public int[] getBlackLevel() {
		return rgbBlack;
	}
	
	/**
	 * Set the black level calibration
	 * @param rgb new black levels
	 */
	public void setBlackLevel(int[] rgb) {
		for (int c=0; c<3; c++) {
			rgbBlack[c] = rgb[c];
		}
	}
	
	/**
	 * Get the white level calibration
	 * @return white level calibration
	 */
	public int[] getWhiteLevel() {
		return rgbWhite;
	}
	
	/**
	 * Set the white level calibration
	 * @param rgb new white levels
	 */
	public void setWhiteLevel(int[] rgb) {
		for (int c=0; c<3; c++) {
			rgbWhite[c] = rgb[c];
		}
	}
	
	/**
	 * Get the current frequency scaling
	 * @return the current frequency scaling
	 */
	public Frequency getFrequency() {
		return frequency;
	}
	
	/**
	 * Set the frequency scaling.
	 * <pre>
	 * f  S0  S1  Frequency scaling
	 * 0  L   L   Off
	 * 1  L   H   2%
	 * 2  H   L   20%
	 * 3  H   H   100%
	 * </pre>
	 * @param f Sampling frequency
	 */
	public void setFrequency(Frequency f) {
		if (s0 == null || s1 == null) {
			return;
		}
		frequency = f;
		switch (f) {
		case OFF:
			s0.off(); s1.off();
			break;
		case TWO_PERCENT:
			s0.off(); s1.on();
			break;
		case TWENTY_PERCENT:
			s0.on(); s1.off();
			break;
		default:
			// 100%
			s0.on(); s1.on();
		}
	}
	
	/**
	 * Get the interval between RGB updates
	 * @return the interval between RGB updates
	 */
	public double getUpdateInterval() {
		return interval;
	}
	
	/**
	 * Set the interval between RGB updates
	 * @param interval the interval between RGB updates ( 0.1 &lt;= interval &lt; 2.0)
	 */
	public void setUpdateInterval(double interval) {
		this.interval = RangeUtil.constrain(interval, 0.1, 2.0);
	}
	
	/**
	 * Get the sample size
	 * @return the sample size
	 */
	public int getSampleSize() {
		return samples;
	}
	
	/**
	 * Set the sample size (number of frequency cycles to accumulate)
	 * @param samples the sample size
	 */
	public void setSampleSize(int samples) {
		this.samples = RangeUtil.constrain(samples, 10, 100);
	}

	/**
	 * Pause reading (until a call to resume).
	 */
	public void pause() {
		read = false;
	}

	/**
	 * Resume reading (after a call to pause)
	 */
	public void resume() {
		read = true;
	}
	
	/**
	 * Set the colour to be sampled.
	 * f  S2  S3  Photodiode
	 * 0  L   L   Red
	 * 1  H   H   Green
	 * 2  L   H   Blue
	 * 3  H   L   Clear (no filter)
	 * @param f Filter
     */
	public void setFilter(Filter f) {
		switch (f) {
		case RED:
			s2.off(); s3.off();
			break;
		case GREEN:
			s2.on(); s3.on();
			break;
		case BLUE:
			s2.off(); s3.on();
			break;
		default:
			// Clear
			s2.on(); s3.off();
		}
	}

	@Override
	public void accept(DigitalInputEvent event) {
		int g = event.getGpio();
		long t = event.getEpochTime();
		if (g == out.getGpio()) {
			// Frequency counter
			if (cycle == 0) {
				startTick = t;
			} else {
				lastTick = t;
			}
			cycle += 1;
		} else {
			// Must be transition between colour samples
			int colour;
			if (g == s2.getGpio()) {
				if (event.getValue()) {
					// Blue -> Green
					colour = 2;
				} else {
					// Clear -> Red
					cycle = 0;
					return;
				}
			} else {
				if (event.getValue()) {
					// Red -> Blue
					colour = 0;
				} else {
					// Green -> Clear
					colour = 1;
				}
			}

			if (cycle > 1) {
				cycle -= 1;
				long td = lastTick - startTick;
				currentHertz[colour] = (int) ((1_000_000 * cycle) / td);
				currentTally[colour] = cycle;
			} else {
				currentHertz[colour] = 0;
				currentTally[colour] = 0;
			}

			cycle = 0;

			// Have we a new set of RGB?
			if (colour == 1) {
				for (int i=0; i<3; i++) {
					latestHertz[i] = currentHertz[i];
					latestTally[i] = currentTally[i];
				}
			}
		}
	}
	
	@Override
	public void run() {
		read = true;
		while (true) {
			if (read) {
				long next_time = System.currentTimeMillis() + (int) (interval*1000);

				//out.setMode(DeviceMode.DIGITAL_OUTPUT); // Enable output gpio.

				// The order Red -> Blue -> Green -> Clear is needed by the
				// callback function so that each S2/S3 transition triggers
				// a state change.  The order was chosen so that a single
				// gpio changes state between each colour to be sampled.
				setFilter(Filter.RED);
				SleepUtil.sleepSeconds(delay[0]);

				setFilter(Filter.BLUE);
				SleepUtil.sleepSeconds(delay[2]);
				
				setFilter(Filter.GREEN);
				SleepUtil.sleepSeconds(delay[1]);

				// TODO Disable output gpio
				//out.setValue(false);

				setFilter(Filter.CLEAR);

				long delay_ms = next_time - System.currentTimeMillis();
				if (delay_ms > 0) {
					SleepUtil.sleepMillis(delay_ms);
				}

				// Tune the next set of delays to get reasonable results
				// as quickly as possible.
				for (int c=0; c<3; c++) {
					// Calculate dly needed to get _samples pulses
					double dly;
					if (latestHertz[c] != 0) {
						dly = samples / (double) latestHertz[c];
					} else {
						// Didn't find any edges, increase sample time
						dly = this.delay[c] + 0.1;
					}

					// Constrain dly to reasonable values.
					dly = RangeUtil.constrain(dly, 0.001, 0.5);

					this.delay[c] = dly;
				}

			} else {
				SleepUtil.sleepSeconds(0.1);
			}
		}
	}

	@Override
	public void close() {
		oe.setOn(false);
		oe.close();
		if (s0 != null) {
			s0.close();
		}
		if (s1 != null) {
			s1.close();
		}
		s2.close();
		s3.close();
		out.close();
	}
	
	public static enum Filter {
		RED, GREEN, BLUE, CLEAR;
	}
	
	public static enum Frequency {
		OFF, TWO_PERCENT, TWENTY_PERCENT, ON;
	}
}
