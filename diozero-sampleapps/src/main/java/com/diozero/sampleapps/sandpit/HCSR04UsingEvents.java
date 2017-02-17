package com.diozero.sampleapps.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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


import java.io.Closeable;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Note this version doesn't work as well as the event-based HCSR04UsingEvents version as the ns event timings
 * aren't from the raw device unfortunately, only as they get processed in Java
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
 * Pi4j:
 *  sudo java -classpath dio-zero-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.HCSR04UsingEvents 17 27
 * JDK Device I/O:
 *  sudo java -classpath dio-zero-0.3-SNAPSHOT.jar com.diozero.sandpit.HCSR04UsingEvents 17 27
 *
 */
public class HCSR04UsingEvents implements DistanceSensorInterface, Closeable, InputEventListener<DigitalInputEvent> {
	public static void main(String[] args) {
		if (args.length != 2) {
			Logger.error("Usage: {} <trigger GPIO> <echo GPIO>", HCSR04UsingEvents.class.getName());
			System.exit(1);
		}
		int trigger_pin = Integer.parseInt(args[0]);
		int echo_pin = Integer.parseInt(args[1]);
		try (HCSR04UsingEvents device = new HCSR04UsingEvents(trigger_pin, echo_pin)) {
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
	private static final double MAX_DISTANCE_CM = 400; // Max distance measurement
	private static final double SPEED_OF_SOUND_CM_PER_S = 34029; // Approx Speed of Sound at sea level and 15 degC
	// After trigger goes low the device sends 8 ultrasonic bursts @ 40kHz
	private static final long ULTRASONIC_BURST_TIME_NS = (SleepUtil.NS_IN_SEC / 40_000) * 8;
	private static final long MAX_DELAY_TO_ECHO_HIGH_NS = SleepUtil.NS_IN_SEC;
	// Calculate the max time (in ns) that the echo pulse stays high
	private static final long EXPECTED_MAX_ECHO_TIME_NS = (int) (MAX_DISTANCE_CM * 2 * SleepUtil.NS_IN_SEC / SPEED_OF_SOUND_CM_PER_S);
	private static final long MAX_ECHO_HIGH_TIME_NS = SleepUtil.NS_IN_SEC;
	
	// States
	private static final int STARTING_UP=0, WAITING_FOR_ECHO_ON=1, WAITING_FOR_ECHO_OFF=2, ERROR=3, FINISHED=4;

	private DigitalOutputDevice trigger;
	private DigitalInputDevice echo;
	private int state = STARTING_UP;
	private long echoOnTimeNs;
	private long echoOnTimeMs;
	private long echoOffTimeNs;
	private long echoOffTimeMs;

	/**
	 * Initialise GPIO to echo and trigger pins
	 *
	 * @param triggerGpioNum GPIO connected to the HC-SR04 trigger pin
	 * @param echoGpioNum GPIO connected to the HC-SR04 echo pin
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public HCSR04UsingEvents(int triggerGpioNum, int echoGpioNum) throws RuntimeIOException {
		// Define device for trigger pin at HCSR04
		trigger = new DigitalOutputDevice(triggerGpioNum, true, false);
		// Define device for echo pin at HCSR04
		echo = new DigitalInputDevice(echoGpioNum, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
		echo.addListener(this);

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
		// Send a pulse trigger of 10 us duration
		state = WAITING_FOR_ECHO_ON;
		trigger.setValueUnsafe(true);
		SleepUtil.sleepNanos(0, PULSE_NS);// wait 10 us (10,000ns)
		trigger.setValueUnsafe(false);
		long trigger_off_time = System.nanoTime(); // ns
		
		synchronized (this) {
			try {
				wait((MAX_DELAY_TO_ECHO_HIGH_NS + MAX_ECHO_HIGH_TIME_NS) / 1000 / 1000);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		if (state != FINISHED) {
			Logger.error("Illegal state {}, wait must have timed out or error occurred", Integer.valueOf(state));
			return -1;
		}
		
		Logger.info("Time from trigger off to echo on = {}ns, ultrasonic burst time={}ns",
				Long.valueOf(echoOnTimeNs - trigger_off_time), Long.valueOf(ULTRASONIC_BURST_TIME_NS));
		Logger.info("Time from echo on to echo off = {}ns, ({}ms), max expected time={}ns",
				Long.valueOf(echoOffTimeNs - echoOnTimeNs),
				Long.valueOf(echoOffTimeMs - echoOnTimeMs),
				Long.valueOf(EXPECTED_MAX_ECHO_TIME_NS));

		double ping_duration_s = (echoOffTimeNs - echoOnTimeNs) / (double)SleepUtil.NS_IN_SEC;

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
		Logger.debug("close()");
		if (trigger != null) { trigger.close(); }
		if (echo != null) { echo.close(); }
	}
	
	@Override
	public void valueChanged(DigitalInputEvent event) {
		if (state == STARTING_UP || state == FINISHED) {
			// Ignore
			return;
		}
		
		synchronized (this) {
			if (event.getValue() && state == WAITING_FOR_ECHO_ON) {
				state = WAITING_FOR_ECHO_OFF;
				echoOnTimeNs = event.getNanoTime();
				echoOnTimeMs = event.getEpochTime();
			} else if (!event.getValue() && state == WAITING_FOR_ECHO_OFF) {
				state = FINISHED;
				echoOffTimeNs = event.getNanoTime();
				echoOffTimeMs = event.getEpochTime();
				notify();
			} else {
				// Error unexpected event...
				Logger.warn("valueChanged({}), unexpected event for state {}", event, Integer.valueOf(state));
				state = ERROR;
				notify();
			}
		}
	}
}
