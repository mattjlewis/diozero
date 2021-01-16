package com.diozero.devices.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     HCSR04UsingWait.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.WaitableDigitalInputDevice;
import com.diozero.devices.DistanceSensorInterface;
import com.diozero.util.SleepUtil;

/**
 * User's manual:
 * https://docs.google.com/document/d/1Y-yZnNhMYy7rwhAgyL_pfa39RsB-x2qR4vP8saG73rE/edit#
 * Product specification:
 * http://www.micropik.com/PDF/HCSR04.pdf
 * 
 * Provides 2cm - 400cm non-contact measurement function, the ranging accuracy
 * can reach to 3mm You only need to supply a short 10uS pulse to the trigger
 * input to start the ranging, and then the module will send out an 8 cycle
 * burst of ultrasound at 40 kHz and raise its echo. The Echo is a distance
 * object that is pulse width and the range in proportion. We suggest to use over
 * 60ms measurement cycle, in order to prevent trigger signal to the echo signal
 */
public class HCSR04UsingWait implements DistanceSensorInterface {
	public static void main(String[] args) {
		if (args.length != 2) {
			Logger.error("Usage: {} <trigger GPIO> <echo GPIO>", HCSR04UsingWait.class.getName());
			System.exit(1);
		}
		int trigger_pin = Integer.parseInt(args[0]);
		int echo_pin = Integer.parseInt(args[1]);
		
		try (HCSR04UsingWait device = new HCSR04UsingWait(trigger_pin, echo_pin)) {
			while (true) {
				Logger.info("Distance = {} cm", String.format("%.3f", Double.valueOf(device.getDistanceCm())));
				SleepUtil.sleepMillis(1000);
			}
		} catch (RuntimeIOException ex) {
			Logger.error(ex, "I/O error with HC-SR04 device: {}", ex.getMessage());
		}
	}

	// Spec says #10us pulse (min) = 10,000 ns
	private static final int PULSE_NS = 10_000; 
	private static final int MAX_DISTANCE_CM = Math.round(400 * 1.5f); // Max distance measurement (400cm + 50%)
	private static final double SPEED_OF_SOUND_CM_PER_S = 34029; // Approx Speed of Sound at sea level and 15 degC
	// Calculate the max time (in ns) that the echo pulse can stay high (35.26ms)
	private static final int MAX_ECHO_TIME_MS = (int) Math.floor(MAX_DISTANCE_CM * 2 * SleepUtil.MS_IN_SEC / SPEED_OF_SOUND_CM_PER_S) + 1;

	private DigitalOutputDevice trigger;
	private WaitableDigitalInputDevice echo;

	/**
	 * Initialise GPIO to echo and trigger pins
	 *
	 * @param triggerGpioNum GPIO connected to the HC-SR04 trigger pin
	 * @param echoGpioNum GPIO connected to the HC-SR04 echo pin
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public HCSR04UsingWait(int triggerGpioNum, int echoGpioNum) throws RuntimeIOException {
		// Define device for trigger pin at HCSR04
		trigger = new DigitalOutputDevice(triggerGpioNum, true, false);
		// Define device for echo pin at HCSR04
		echo = new WaitableDigitalInputDevice(echoGpioNum, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.BOTH);

		// Sleep for 20 ms - let the device settle?
		SleepUtil.sleepMillis(20);
	}

	/**
	 * Send a pulse to HCSR04 and compute the echo to obtain distance
	 *
	 * @return distance in cm
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public float getDistanceCm() throws RuntimeIOException {
		double ping_duration_s;
		try {
			// Send a pulse trigger of 10 us duration
			trigger.setValueUnsafe(true);
			// wait 10 us (10,000ns)
			Thread.sleep(0, PULSE_NS);
			trigger.setValueUnsafe(false);
		
			// Wait up to 1 second for the echo pin to rise
			if (! echo.waitForValue(true, 1000)) {
				Logger.warn("Timed-out waiting for echo pin to go high, max={}ms", Integer.valueOf(1000));
				return -1;
			}
			long echo_on_time = System.nanoTime();
			
			// Wait up to 36ms for the echo pin to fall (that is the maximum echo time given
			// a maximum distance of 600cm, any longer means something's gone wrong)
			if (! echo.waitForValue(false, MAX_ECHO_TIME_MS)) {
				Logger.warn("Timed-out waiting for echo pin to go low, max={}ms", Long.valueOf(MAX_ECHO_TIME_MS));
				return -1;
			}
			long echo_off_time = System.nanoTime();

			ping_duration_s = (echo_off_time - echo_on_time) / (double)SleepUtil.NS_IN_SEC;
			Logger.info("Ping duration = {}ms", Double.valueOf((ping_duration_s * 1000)));
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
			return -1;
		}
		
		// Calculate distance as time for echo multiplied by speed of sound divided by two to
		// compensate for travel to and from the reflector
		
		// Distance = velocity * time taken
		// Half the ping duration as it is the time to the object and back
		double distance = SPEED_OF_SOUND_CM_PER_S * (ping_duration_s / 2.0);
		if (distance > MAX_DISTANCE_CM) {
			distance = MAX_DISTANCE_CM;
		}

		return (float)distance;
	}

	/**
	 * Free device GPIOs
	 */
	@Override
	public void close() {
		Logger.trace("close()");
		if (trigger != null) { trigger.close(); }
		if (echo != null) { echo.close(); }
	}
}
