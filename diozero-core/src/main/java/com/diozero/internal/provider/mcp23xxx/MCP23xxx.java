package com.diozero.internal.provider.mcp23xxx;

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
import com.diozero.internal.provider.mcp23017.MCP23017DigitalInputDevice;
import com.diozero.internal.spi.*;
import com.diozero.util.BitManipulation;
import com.diozero.util.MutableByte;
import com.diozero.util.RuntimeIOException;

/**
 * Support for both MCP23008 and MCP23017 GPIO expansion boards.
 */
public abstract class MCP23xxx extends AbstractDeviceFactory
implements GpioDeviceFactoryInterface, InputEventListener<DigitalInputEvent>, Closeable {
	private static enum InterruptMode {
		DISABLED, BANK_A_ONLY, BANK_B_ONLY, BANK_A_AND_B, MIRRORED;
	}
	
	/** Controls how the registers are addressed
	 * 1 = The registers associated with each port are separated into different banks
	 * 0 = The registers are in the same bank (addresses are sequential) */
	private static final byte IOCON_BANK_BIT = 7;
	/** INT Pins Mirror bit
	 * 1 = The INT pins are internally connected
	 * 0 = The INT pins are not connected. INTA is associated with PortA and INTB is associated with PortB */
	private static final byte IOCON_MIRROR_BIT = 6;
	/** Sequential Operation mode bit
	 * 1 = Sequential operation disabled, address pointer does not increment.
	 * 0 = Sequential operation enabled, address pointer increments */
	private static final byte IOCON_SEQOP_BIT = 5;
	/** Slew Rate control bit for SDA output
	 * 1 = Slew rate disabled.
	 * 0 = Slew rate enabled */
	//private static final byte IOCON_DISSLW_BIT = 4;
	/** Hardware Address Enable bit (MCP23S17 only). Address pins are always enabled on MCP23017
	 * 1 = Enables the MCP23S17 address pins.
	 * 0 = Disables the MCP23S17 address pins */
	//private static final byte IOCON_HAEN_BIT = 3;
	/** This bit configures the INT pin as an open-drain output
	 * 1 = Open-drain output (overrides the INTPOL bit).
	 * 0 = Active driver output (INTPOL bit sets the polarity) */
	private static final byte IOCON_ODR_BIT = 2;
	/** This bit sets the polarity of the INT output pin.
	 * 1 = Active-high.
	 * 0 = Active-low */
	private static final byte IOCON_INTPOL_BIT = 1;
	
	private static final int PINS_PER_PORT = 8;
	public static final int INTERRUPT_PIN_NOT_SET = -1;

	private String deviceName;
	private DigitalInputDevice[] interruptPins;
	private MutableByte[] directions = { new MutableByte(), new MutableByte() };
	private MutableByte[] pullUps = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptOnChangeFlags = { new MutableByte(), new MutableByte() };
	private MutableByte[] defaultValues = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptCompareFlags = { new MutableByte(), new MutableByte() };
	private InterruptMode interruptMode = InterruptMode.DISABLED;
	private int numPorts;
	private int numPins;

	public MCP23xxx(int numPorts, String deviceName) throws RuntimeIOException {
		this(numPorts, deviceName, INTERRUPT_PIN_NOT_SET, INTERRUPT_PIN_NOT_SET);
	}

	public MCP23xxx(int numPorts, String deviceName, int interruptGpio) throws RuntimeIOException {
		this(numPorts, deviceName, interruptGpio, interruptGpio);
	}

	public MCP23xxx(int numPorts, String deviceName, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		super(deviceName + "-");
		
		this.numPorts = numPorts;
		numPins = numPorts*PINS_PER_PORT;
		this.deviceName = deviceName;
		
		interruptPins = new DigitalInputDevice[numPorts];
		if (interruptGpioA != INTERRUPT_PIN_NOT_SET) {
			interruptPins[0] = new DigitalInputDevice(interruptGpioA, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
			
			if (interruptGpioA == interruptGpioB) {
				interruptMode = InterruptMode.MIRRORED;
			} else {
				interruptMode = InterruptMode.BANK_A_ONLY;
			}
		}
		
		// There can only be one interrupt pin (A) if there is only one bank of pins
		if (numPorts > 1 && interruptMode != InterruptMode.MIRRORED
				&& interruptGpioB != INTERRUPT_PIN_NOT_SET) {
			interruptPins[1] = new DigitalInputDevice(interruptGpioB, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
			
			if (interruptMode == InterruptMode.BANK_A_ONLY) {
				interruptMode = InterruptMode.BANK_A_AND_B;
			} else {
				interruptMode = InterruptMode.BANK_B_ONLY;
			}
		}
	}
	
	@Override
	public final String getName() {
		return deviceName;
	}
	
	protected final void initialise() {
		// Initialise
		// Read the I/O configuration value
		byte start_iocon = readByte(getIOConReg(0));
		System.out.println(start_iocon);
		Logger.debug("Default power-on values for IOCON: 0x{}", Integer.toHexString(start_iocon));
		if (numPorts > 1) {
			// Is there an IOCONB value?
			Logger.debug("IOCONB: 0x{}", Integer.toHexString(readByte(getIOConReg(1))));
		}
		
		// Configure interrupts
		MutableByte iocon = new MutableByte(start_iocon);
		if (interruptMode == InterruptMode.MIRRORED) {
			// Enable interrupt mirroring
			iocon.setBit(IOCON_MIRROR_BIT);
			iocon.setBit(IOCON_INTPOL_BIT);
		} else if (interruptMode != InterruptMode.DISABLED) {
			// Disable interrupt mirroring
			iocon.unsetBit(IOCON_MIRROR_BIT);
			iocon.setBit(IOCON_INTPOL_BIT);
		}
		iocon.unsetBit(IOCON_BANK_BIT);
		iocon.setBit(IOCON_SEQOP_BIT);
		iocon.unsetBit(IOCON_ODR_BIT);
		if (! iocon.equals(start_iocon)) {
			writeByte(getIOConReg(0), iocon.getValue());
		}
	
		for (int port=0; port<numPorts; port++) {
			// Default all pins to output
			writeByte(getIODirReg(port), directions[port].getValue());
			// Default to normal input polarity - IPOLA/IPOLB
			writeByte(getIPolReg(port), (byte) 0);
			// Disable interrupt-on-change for all pins
			writeByte(getGPIntEnReg(port), interruptOnChangeFlags[port].getValue());
			// Set default compare values to 0
			writeByte(getDefValReg(port), defaultValues[port].getValue());
			// Disable interrupt comparison control
			writeByte(getIntConReg(port), interruptCompareFlags[port].getValue());
			// Disable pull-up resistors
			writeByte(getGPPullUpReg(port), pullUps[port].getValue());
			// Set all values to off
			writeByte(getGPIOReg(port), (byte) 0);
		}
		
		// Finally enable interrupt listeners
		for (DigitalInputDevice interrupt_pin : interruptPins) {
			if (interrupt_pin != null) {
				Logger.debug("Setting interruptPin ({}) consumer", Integer.valueOf(interrupt_pin.getGpio()));
				interrupt_pin.addListener(this);
			}
		}
	}

	@Override
	public GpioDigitalInputDeviceInterface provisionDigitalInputPin(int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numPins) {
			throw new IllegalArgumentException(
					"Invalid GPIO (" + gpio + "); must be 0.." + (numPins - 1));
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		setInputMode(gpio, pud, trigger);
		
		GpioDigitalInputDeviceInterface in_device = new MCP23xxxDigitalInputDevice(this, key, gpio, trigger);
		deviceOpened(in_device);
		
		return in_device;
	}

	@Override
	public GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int gpio, boolean initialValue) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numPins) {
			throw new IllegalArgumentException(
					"Invalid GPIO (" + gpio + "); must be 0.." + (numPins - 1));
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		setOutputMode(gpio);
		
		GpioDigitalOutputDeviceInterface out_device = new MCP23xxxDigitalOutputDevice(this, key, gpio);
		deviceOpened(out_device);
		out_device.setValue(initialValue);
		
		return out_device;
	}

	protected void setInputMode(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		// TODO Detect no change in direction?
		
		byte bit = (byte) (gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		Logger.info("setInputMode({}), directions={}", gpio, directions[port].getValue());
		
		// Set the following values: direction, pullUp, interruptCompare, defaultValue, interruptOnChange
		directions[port].setBit(bit);
		writeByte(getIODirReg(port), directions[port].getValue());
		byte new_dir = readByte(getIODirReg(port));
		Logger.info("setInputMode({}), directions={}, new_dir={}", gpio, directions[port].getValue(), new_dir);
		if (pud == GpioPullUpDown.PULL_UP) {
			pullUps[port].setBit(bit);
			writeByte(getGPPullUpReg(port), pullUps[port].getValue());
		}
		if (interruptMode != InterruptMode.DISABLED) {
			if (trigger == GpioEventTrigger.RISING) {
				defaultValues[port].unsetBit(bit);
				interruptCompareFlags[port].setBit(bit);
			} else if (trigger == GpioEventTrigger.FALLING) {
				defaultValues[port].setBit(bit);
				interruptCompareFlags[port].setBit(bit);
			} else {
				interruptCompareFlags[port].unsetBit(bit);
			}
			interruptOnChangeFlags[port].setBit(bit);
			writeByte(getDefValReg(port), defaultValues[port].getValue());
			writeByte(getIntConReg(port), interruptCompareFlags[port].getValue());
			writeByte(getGPIntEnReg(port), interruptOnChangeFlags[port].getValue());
		}
	}
	
	protected void setOutputMode(int gpio) {
		// TODO Detect no change in direction?
		
		byte bit = (byte) (gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		// Set the following values: direction, pullUp, interruptCompare, defaultValue, interruptOnChange
		directions[port].unsetBit(bit);
		writeByte(getIODirReg(port), directions[port].getValue());
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface provisionDigitalInputOutputPin(int gpio, GpioDeviceInterface.Mode mode) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numPins) {
			throw new IllegalArgumentException(
					"Invalid GPIO (" + gpio + "); must be 0.." + (numPins - 1));
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		// Nothing to do assuming that closing a pin resets it to the default output state?
		
		GpioDigitalInputOutputDeviceInterface inout_device = new MCP23xxxDigitalInputOutputDevice(this, key, gpio, mode);
		deviceOpened(inout_device);
		
		return inout_device;
	}

	public boolean getValue(int gpio) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numPins) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ deviceName + " has " + numPins + " GPIOs; must be 0.." + (numPins - 1));
		}
		
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		byte states = readByte(getGPIOReg(port));
		
		return (states & bit) != 0;
	}

	public void setValue(int gpio, boolean value) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numPins) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ deviceName + " has " + numPins + " GPIOs; must be 0.." + (numPins - 1));
		}
		
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		// Check the direction of the pin - can't set the value of input pins (direction bit is set)
		if (directions[port].isBitSet(bit)) {
			throw new IllegalStateException("Can't set value for input pin: " + gpio);
		}
		// Read the current state of this bank of GPIOs
		byte old_val = readByte(getGPIOReg(port));
		byte new_val = BitManipulation.setBitValue(old_val, value, bit);
		writeByte(getOLatReg(port), new_val);
	}
	
	public byte getValues(int port) {
		return readByte(getOLatReg(port));
	}
	
	public void setValues(int port, byte values) {
		writeByte(getOLatReg(port), values);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		// Close the interrupt pins
		for (DigitalInputDevice interrupt_pin : interruptPins) {
			if (interrupt_pin != null) { interrupt_pin.close(); }
		}
		// Close all open pins before closing the I2C device itself
		shutdown();
	}

	public void closePin(int gpio) throws RuntimeIOException {
		Logger.debug("closePin({})", Integer.valueOf(gpio));
		
		if (gpio < 0 || gpio >= numPins) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ deviceName + " has " + numPins + " GPIOs; must be 0.." + (numPins - 1));
		}
		
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		// Clean-up this pin only
		
		if (interruptOnChangeFlags[port].isBitSet(bit)) {
			interruptOnChangeFlags[port].unsetBit(bit);
			writeByte(getGPIntEnReg(port), interruptOnChangeFlags[port].getValue());
		}
		if (defaultValues[port].isBitSet(bit)) {
			defaultValues[port].unsetBit(bit);
			writeByte(getDefValReg(port), defaultValues[port].getValue());
		}
		if (interruptCompareFlags[port].isBitSet(bit)) {
			interruptCompareFlags[port].unsetBit(bit);
			writeByte(getIntConReg(port), interruptCompareFlags[port].getValue());
		}
		if (pullUps[port].isBitSet(bit)) {
			pullUps[port].unsetBit(bit);
			writeByte(getGPPullUpReg(port), pullUps[port].getValue());
		}
		if (directions[port].isBitSet(bit)) {
			directions[port].unsetBit(bit);
			writeByte(getIODirReg(port), directions[port].getValue());
		}
	}

	@Override
	@SuppressWarnings("resource")
	public void valueChanged(DigitalInputEvent event) {
		Logger.debug("valueChanged({})", event);
		
		if (! event.getValue()) {
			Logger.info("valueChanged(): value was false - ignoring");
			return;
		}
		
		// Check the event is for one of the interrupt pins
		boolean process_event = false;
		for (DigitalInputDevice interrupt_pin : interruptPins) {
			if (interrupt_pin != null && event.getPin() == interrupt_pin.getGpio()) {
				process_event = true;
				break;
			}
		}
		if (process_event) {
			Logger.error("Unexpected input event on pin {}", Integer.valueOf(event.getPin()));
			return;
		}
		
		synchronized (this) {
			try {
				byte[] intf = new byte[2];
				byte[] intcap = new byte[2];
				if (interruptMode == InterruptMode.MIRRORED) {
					intf[0] = readByte(getIntFReg(0));
					intcap[0] = readByte(getIntCapReg(0));
					intf[1] = readByte(getIntFReg(1));
					intcap[1] = readByte(getIntCapReg(1));
				} else if (interruptMode != InterruptMode.DISABLED) {
					if (interruptPins[0] != null && event.getPin() == interruptPins[0].getGpio()) {
						intf[0] = readByte(getIntFReg(0));
						intcap[0] = readByte(getIntCapReg(0));
					} else {
						intf[1] = readByte(getIntFReg(1));
						intcap[1] = readByte(getIntCapReg(1));
					}
				}
				Logger.debug("Interrupt values: [A]=(0x{}, 0x{}), [B]=(0x{}, 0x{})",
						Integer.toHexString(intf[0]), Integer.toHexString(intcap[0]),
						Integer.toHexString(intf[1]), Integer.toHexString(intcap[1]));
				for (byte bit=0; bit<7; bit++) {
					if (BitManipulation.isBitSet(intf[0], bit)) {
						boolean value = BitManipulation.isBitSet(intcap[0], bit);
						DigitalInputEvent e = new DigitalInputEvent(bit, event.getEpochTime(), event.getNanoTime(), value);
						// Notify the appropriate input device
						MCP23017DigitalInputDevice in_device = getInputDevice(bit);
						if (in_device != null) {
							in_device.valueChanged(e);
						}
					}
				}
				for (byte bit=0; bit<7; bit++) {
					if (BitManipulation.isBitSet(intf[1], bit)) {
						boolean value = BitManipulation.isBitSet(intcap[1], bit);
						DigitalInputEvent e = new DigitalInputEvent(bit+PINS_PER_PORT, event.getEpochTime(), event.getNanoTime(), value);
						// Notify the appropriate input device
						MCP23017DigitalInputDevice in_device = getInputDevice((byte)(bit+8));
						if (in_device != null) {
							in_device.valueChanged(e);
						}
					}
				}
			} catch (RuntimeIOException e) {
				// Log and ignore
				Logger.error(e, "IO error handling interrupts: {}", e);
			}
		}
	}

	private MCP23017DigitalInputDevice getInputDevice(byte gpio) {
		return getDevice(createPinKey(gpio), MCP23017DigitalInputDevice.class);
	}
	
	protected abstract int getIODirReg(int port);
	protected abstract int getIPolReg(int port);
	protected abstract int getGPIntEnReg(int port);
	protected abstract int getDefValReg(int port);
	protected abstract int getIntConReg(int port);
	protected abstract int getIOConReg(int port);
	protected abstract int getGPPullUpReg(int port);
	protected abstract int getIntFReg(int port);
	protected abstract int getIntCapReg(int port);
	protected abstract int getGPIOReg(int port);
	protected abstract int getOLatReg(int port);
	
	protected abstract byte readByte(int register);
	protected abstract void writeByte(int register, byte value);
}
