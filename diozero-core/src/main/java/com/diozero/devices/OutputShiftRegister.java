package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OutputShiftRegister.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import java.util.EnumSet;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.SoftwarePwmOutputDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.util.BitManipulation;
import com.diozero.util.MutableByte;
import com.diozero.util.SleepUtil;

/**
 * <p>
 * Digital output device factory using an output shift register, e.g. the
 * 74HC595 8-bit Serial-In, Parallel-Out Shift Register.
 * </p>
 *
 * <h2>Wiring for 74HC595</h2>
 *
 * <pre>
 *  Q1 1 16 Vcc
 *  Q2 2 15 Q0
 *  Q3 3 14 SER (Data Pin)
 *  Q4 4 13 ^OE (Output Enable, low == enable)
 *  Q5 5 12 RCLK (Storage Register Clock / Latch Pin)
 *  Q6 6 11 SRCLK (Shift Register Clock / Clock Pin)
 *  Q7 7 10 ^SRCLR (Shift Register Clear, low == clear)
 * GND 8 9  Qh'
 * </pre>
 *
 * <h2>Timings for SN74HC595</h2>
 * <dl>
 * <dt>Clock Frequency:</dt>
 * <dd>2V: 5MHz, 4.5V: 25MHz, 6V: 29MHz</dd>
 * <dt>Pulse Duration (SRCLK or RCLK high or low):</dt>
 * <dd>2V: 100ns, 4.5V: 20ns, 6V: 17ns</dd>
 * <dt>Setup Time (SER before SRCLK high):</dt>
 * <dd>2V: 125ns, 4.5V: 25ns, 6V: 21ns</dd>
 * <dt>Setup Time (SRCLK high before RCLK high *):</dt>
 * <dd>2V: 94ns, 4.5V: 19ns, 6V: 16ns</dd>
 * <dt>Hold Time (Hold time, SER after SRCLK high):</dt>
 * <dd>2V: 0ns, 4.5V: 0ns, 6V: 0ns</dd>
 * </dl>
 * <p>
 * * This set-up time allows the storage register to receive stable data from
 * the shift register. The clocks can be tied together, in which case the shift
 * register is one clock pulse ahead of the storage register.
 * </p>
 *
 * Credit: Seggan
 */
public class OutputShiftRegister extends AbstractDeviceFactory
		implements GpioDeviceFactoryInterface, PwmOutputDeviceFactoryInterface, GpioExpander {
	private static final String DEVICE_NAME = "OutputShiftRegister";
	private static final int DEFAULT_PWM_FREQUENCY = 50;

	/** DS: Serial Data Input [SER Pin 14] */
	private final DigitalOutputDevice dataPin;
	/** SH_CP: Shift Register Clock Pin [SRCLK Pin 11] */
	private final DigitalOutputDevice clockPin;
	/** ST_CP. Storage Register Clock Pin / Shift Output [RCLK Pin 12] */
	private final DigitalOutputDevice latchPin;

	private boolean[] buf;
	private boolean[] values;

	private BoardPinInfo boardPinInfo;

	public OutputShiftRegister(int dataGpio, int clockGpio, int latchGpio, int numOutputs) {
		this(new DigitalOutputDevice(dataGpio), new DigitalOutputDevice(clockGpio), new DigitalOutputDevice(latchGpio),
				numOutputs);
	}

	public OutputShiftRegister(GpioDeviceFactoryInterface deviceFactory, int dataGpio, int clockGpio, int latchGpio,
			int numOutputs) {
		this(DigitalOutputDevice.Builder.builder(dataGpio).setDeviceFactory(deviceFactory).build(),
				DigitalOutputDevice.Builder.builder(clockGpio).setDeviceFactory(deviceFactory).build(),
				DigitalOutputDevice.Builder.builder(latchGpio).setDeviceFactory(deviceFactory).build(), numOutputs);
	}

	public OutputShiftRegister(DigitalOutputDevice dataPin, DigitalOutputDevice clockPin, DigitalOutputDevice latchPin,
			int numOutputs) {
		super(DEVICE_NAME);

		this.dataPin = dataPin;
		this.clockPin = clockPin;
		this.latchPin = latchPin;

		buf = new boolean[numOutputs];
		values = new boolean[numOutputs];

		boardPinInfo = new BoardPinInfo();
		for (int i = 0; i < numOutputs; i++) {
			boardPinInfo.addGpioPinInfo(i, i, EnumSet.of(DeviceMode.DIGITAL_OUTPUT));
		}
	}

	@Override
	public void setDirections(int port, byte directions) {
		// Ignore
		if (directions != 0) {
			Logger.warn("This device only supports GPIO output");
		}
	}

	@Override
	public void setValues(int port, byte newValues) {
		if (port < 0 || port >= values.length / 8) {
			throw new IllegalArgumentException("Invalid port " + port + " must be 0.." + (values.length / 8));
		}

		for (int i = 0; i < 8; i++) {
			buf[i + port * 8] = BitManipulation.isBitSet(newValues, i);
		}
		flush();
	}

	public boolean getValue(int outputPin) {
		if (outputPin < 0 || outputPin >= values.length) {
			throw new IllegalArgumentException("Invalid outputPin " + outputPin + " must be 0.." + (values.length - 1));
		}

		return values[outputPin];
	}

	public byte getValues(int port) {
		if (port < 0 || port >= values.length / 8) {
			throw new IllegalArgumentException("Invalid port " + port + " must be 0.." + (values.length / 8));
		}

		MutableByte mb = new MutableByte();
		for (int i = 0; i < 8; i++) {
			mb.setBitValue(i, values[i + port * 8]);
		}

		return mb.getValue();
	}

	public void setBufferedValue(int outputPin, boolean value) {
		if (outputPin < 0 || outputPin >= values.length) {
			throw new IllegalArgumentException("Invalid outputPin " + outputPin + " must be 0.." + (values.length - 1));
		}

		buf[outputPin] = value;
	}

	public void flush() {
		// Ground the latch pin and hold low for as long as you are transmitting
		latchPin.off();
		SleepUtil.busySleep(100);

		shiftOut();

		// Return the latch pin to high to signal to the chip that it no longer needs to
		// listen for information
		// SRCLK high before RCLK high
		SleepUtil.busySleep(100);
		latchPin.on();
	}

	private void shiftOut() {
		/*- Arduino code:
		void shiftOut(uint8_t dataPin, uint8_t clockPin, uint8_t bitOrder, uint8_t val) {
		  uint8_t i;
		
		  for (i = 0; i < 8; i++) {
		    if (bitOrder == LSBFIRST) {
		      digitalWrite(dataPin, !!(val & (1 << i)));
		    } else {
		      digitalWrite(dataPin, !!(val & (1 << (7 - i))));
		    }
		    digitalWrite(clockPin, HIGH);
		    digitalWrite(clockPin, LOW);
		  }
		}
		 */
		for (int i = buf.length - 1; i >= 0; i--) {
			dataPin.setOn(buf[i]);
			values[i] = buf[i];
			// Max SER before SRCLK high
			SleepUtil.busySleep(125);

			clockPin.on();
			// Max SRCLK pulse duration is 100ns
			SleepUtil.busySleep(100);
			clockPin.off();
		}
		dataPin.off();
	}

	public void clear() {
		for (int i = 0; i < buf.length; i++) {
			buf[i] = false;
		}
		flush();
	}

	@Override
	public void close() throws RuntimeIOException {
		clear();

		latchPin.close();
		clockPin.close();
		dataPin.close();
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		throw new UnsupportedOperationException("Only digital output is supported");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		return new OsrDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		throw new UnsupportedOperationException("Only digital output is supported");
	}

	static final class OsrDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
		private OutputShiftRegister osrDeviceFactory;
		private int gpio;

		public OsrDigitalOutputDevice(OutputShiftRegister osrDeviceFactory, String key, PinInfo pinInfo,
				boolean initialValue) {
			super(key, osrDeviceFactory);

			this.osrDeviceFactory = osrDeviceFactory;
			this.gpio = pinInfo.getDeviceNumber();

			setValue(initialValue);
		}

		@Override
		public int getGpio() {
			return gpio;
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return osrDeviceFactory.getValue(gpio);
		}

		@Override
		public void setValue(boolean value) throws RuntimeIOException {
			osrDeviceFactory.setBufferedValue(gpio, value);
			osrDeviceFactory.flush();
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.trace("closeDevice() {}", getKey());
			// Nothing to do?
			setValue(false);
		}
	}

	@Override
	public int getBoardPwmFrequency() {
		return DEFAULT_PWM_FREQUENCY;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		Logger.warn("PWM frequency is fixed");
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		Logger.warn("Using software PWM on gpio {}", Integer.valueOf(pinInfo.getDeviceNumber()));

		// Need to make sure the keys are different
		// Note this is replicating the functionality in provisionDigitalOutputDevice.
		// That method can't be called as it will throw a device already opened
		// exception.
		// Also note that SoftwarePwmOutputDevice has special cleanup functionality.
		GpioDigitalOutputDeviceInterface gpio_output_device = createDigitalOutputDevice("PWM-" + key, pinInfo, false);
		deviceOpened(gpio_output_device);

		return new SoftwarePwmOutputDevice(key, this, gpio_output_device, pwmFrequency, initialValue);
	}
}
