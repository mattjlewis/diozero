package com.diozero.sandpit;

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
 *  sudo java -classpath dio-zero-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.HCSR04UsingEvents 17 27
 * JDK Device I/O:
 *  sudo java -classpath dio-zero-0.2-SNAPSHOT.jar com.diozero.sandpit.HCSR04UsingEvents 17 27
 *
 */
public class HCSR04UsingEvents implements DistanceSensorInterface, Closeable, InputEventListener<DigitalPinEvent> {
	public static void main(String[] args) {
		if (args.length != 2) {
			Logger.error("Usage: HCSR04UsingEvents <trigger GPIO> <echo GPIO>");
			System.exit(1);
		}
		int trigger_pin = Integer.parseInt(args[0]);
		int echo_pin = Integer.parseInt(args[1]);
		try (HCSR04UsingEvents device = new HCSR04UsingEvents()) {
			device.init(trigger_pin, echo_pin);
			
			while (true) {
				Logger.info("Distance = {} cm", String.format("%.3f", Double.valueOf(device.getDistanceCm())));
				SleepUtil.sleepMillis(1000);
			}
		} catch (RuntimeIOException ex) {
			Logger.error(ex, "I/O error with HC-SR04 device: {}", ex.getMessage());
		}
	}

	private static long MS_IN_SEC = 1000;
	private static long US_IN_SEC = MS_IN_SEC * 1000;
	private static long NS_IN_SEC = US_IN_SEC * 1000;
	// Spec says #10us pulse (min) = 10,000 ns
	private static final int PULSE_NS = 10_000; 
	private static final double MAX_DISTANCE_CM = 400; // Max distance measurement
	private static final double SPEED_OF_SOUND_CM_PER_S = 34029; // Approx Speed of Sound at sea level and 15 degC
	// After trigger goes low the device sends 8 ultrasonic bursts @ 40kHz
	private static final long ULTRASONIC_BURST_TIME_NS = (NS_IN_SEC / 40_000) * 8;
	private static final long MAX_DELAY_TO_ECHO_HIGH_NS = NS_IN_SEC;
	// Calculate the max time (in ns) that the echo pulse stays high
	private static final long EXPECTED_MAX_ECHO_TIME_NS = (int) (MAX_DISTANCE_CM * 2 * NS_IN_SEC / SPEED_OF_SOUND_CM_PER_S);
	private static final long MAX_ECHO_HIGH_TIME_NS = NS_IN_SEC;

	private DigitalOutputDevice trigger;
	private DigitalInputDevice echo;
	private State state = State.STARTING_UP;
	private long echoOnTimeNs;
	private long echoOnTimeMs;
	private long echoOffTimeNs;
	private long echoOffTimeMs;

	/**
	 * Initialise GPIO to echo and trigger pins
	 *
	 * @param triggerGpioPin
	 * @param echoGpioPin
	 */
	public void init(int triggerGpioNum, int echoGpioNum) throws RuntimeIOException {
		// Define device for trigger pin at HCSR04
		trigger = new DigitalOutputDevice(triggerGpioNum);
		// Define device for echo pin at HCSR04
		echo = new DigitalInputDevice(echoGpioNum, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
		echo.addListener(this);

		trigger.setValue(false);
		// Wait for 0.5 seconds
		SleepUtil.sleepSeconds(0.5);
	}

	/**
	 * Send a pulse to HCSR04 and compute the echo to obtain distance
	 *
	 * @return distance in cm
	 */
	@Override
	public double getDistanceCm() throws RuntimeIOException {
		// Send a pulse trigger of 10 us duration
		trigger.setValue(true);
		state = State.WAITING_FOR_ECHO_ON;
		SleepUtil.sleep(0, PULSE_NS);// wait 10 us (10,000ns)
		trigger.setValue(false);
		long trigger_off_time = System.nanoTime(); // ns
		
		synchronized (this) {
			try {
				wait((MAX_DELAY_TO_ECHO_HIGH_NS + MAX_ECHO_HIGH_TIME_NS) / 1000 / 1000);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		if (state != State.FINISHED) {
			Logger.error("Illegal state {}, wait must have timed out or error occurred", state);
			return -1;
		}
		
		Logger.info("Time from trigger off to echo on = {}ns, ultrasonic burst time={}ns",
				Long.valueOf(echoOnTimeNs - trigger_off_time), Long.valueOf(ULTRASONIC_BURST_TIME_NS));
		Logger.info("Time from echo on to echo off = {}ns, ({}ms), max expected time={}ns",
				Long.valueOf(echoOffTimeNs - echoOnTimeNs),
				Long.valueOf(echoOffTimeMs - echoOnTimeMs),
				Long.valueOf(EXPECTED_MAX_ECHO_TIME_NS));

		double ping_duration_s = (echoOffTimeNs - echoOnTimeNs) / (double)NS_IN_SEC;

		// Distance = velocity * time taken
		// Half the ping duration as it is the time to the object and back
		double distance = SPEED_OF_SOUND_CM_PER_S * (ping_duration_s / 2.0);
		if (distance > MAX_DISTANCE_CM) {
			distance = MAX_DISTANCE_CM;
		}

		return distance;
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
	public synchronized void valueChanged(DigitalPinEvent event) {
		Logger.debug("valueChanged({}), state={}", event, state);
		switch (state) {
		case WAITING_FOR_ECHO_ON:
			if (event.getValue()) {
				echoOnTimeNs = event.getNanoTime();
				echoOnTimeMs = event.getEpochTime();
				state = State.WAITING_FOR_ECHO_OFF;
			}
			break;
		case WAITING_FOR_ECHO_OFF:
			if (!event.getValue()) {
				echoOffTimeNs = event.getNanoTime();
				echoOffTimeMs = event.getEpochTime();
				state = State.FINISHED;
				notify();
			}
			break;
		default:
			// Nothing to do
		}
	}
}

enum State {
	STARTING_UP, WAITING_FOR_ECHO_ON, WAITING_FOR_ECHO_OFF, ERROR, FINISHED;
}
