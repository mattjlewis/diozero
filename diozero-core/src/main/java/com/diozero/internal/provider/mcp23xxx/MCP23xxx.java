package com.diozero.internal.provider.mcp23xxx;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     MCP23xxx.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioExpander;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InputEventListener;
import com.diozero.api.PinInfo;
import com.diozero.internal.SoftwarePwmOutputDevice;
import com.diozero.internal.provider.AbstractDeviceFactory;
import com.diozero.internal.provider.GpioDeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.util.BitManipulation;
import com.diozero.util.MutableByte;
import com.diozero.util.RuntimeIOException;

/**
 * Support for both MCP23008 and MCP23017 GPIO expansion boards.
 */
public abstract class MCP23xxx extends AbstractDeviceFactory implements GpioDeviceFactoryInterface,
		PwmOutputDeviceFactoryInterface, InputEventListener<DigitalInputEvent>, GpioExpander {
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
	private static final byte IOCON_DISSLW_BIT = 4;
	/** Hardware Address Enable bit (MCP23S17 only). Address pins are always enabled on MCP23017
	 * 1 = Enables the MCP23S17 address pins.
	 * 0 = Disables the MCP23S17 address pins */
	private static final byte IOCON_HAEN_BIT = 3;
	/** This bit configures the INT pin as an open-drain output
	 * 1 = Open-drain output (overrides the INTPOL bit).
	 * 0 = Active driver output (INTPOL bit sets the polarity) */
	private static final byte IOCON_ODR_BIT = 2;
	/** This bit sets the polarity of the INT output pin.
	 * 1 = Active-high.
	 * 0 = Active-low */
	private static final byte IOCON_INTPOL_BIT = 1;
	
	private static final int GPIOS_PER_PORT = 8;
	public static final int INTERRUPT_GPIO_NOT_SET = -1;
	private static final int DEFAULT_PWM_FREQUENCY = 50;

	private String deviceName;
	private DigitalInputDevice[] interruptGpios;
	private MutableByte[] directions = { new MutableByte(), new MutableByte() };
	private MutableByte[] pullUps = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptOnChangeFlags = { new MutableByte(), new MutableByte() };
	private MutableByte[] defaultValues = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptCompareFlags = { new MutableByte(), new MutableByte() };
	private InterruptMode interruptMode = InterruptMode.DISABLED;
	private int numPorts;
	private int numGpios;

	public MCP23xxx(int numPorts, String deviceName) throws RuntimeIOException {
		this(numPorts, deviceName, INTERRUPT_GPIO_NOT_SET, INTERRUPT_GPIO_NOT_SET);
	}

	public MCP23xxx(int numPorts, String deviceName, int interruptGpio) throws RuntimeIOException {
		this(numPorts, deviceName, interruptGpio, interruptGpio);
	}

	public MCP23xxx(int numPorts, String deviceName, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		super(deviceName);
		
		this.numPorts = numPorts;
		numGpios = numPorts*GPIOS_PER_PORT;
		this.deviceName = deviceName;
		
		interruptGpios = new DigitalInputDevice[numPorts];
		if (interruptGpioA != INTERRUPT_GPIO_NOT_SET) {
			interruptGpios[0] = new DigitalInputDevice(interruptGpioA, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
			
			if (interruptGpioA == interruptGpioB) {
				interruptMode = InterruptMode.MIRRORED;
			} else {
				interruptMode = InterruptMode.BANK_A_ONLY;
			}
		}
		
		// There can only be one interrupt GPIO (A) if there is only one bank of GPIOs
		if (numPorts > 1 && interruptMode != InterruptMode.MIRRORED
				&& interruptGpioB != INTERRUPT_GPIO_NOT_SET) {
			interruptGpios[1] = new DigitalInputDevice(interruptGpioB, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
			
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
		iocon.unsetBit(IOCON_DISSLW_BIT);
		iocon.setBit(IOCON_HAEN_BIT);
		iocon.unsetBit(IOCON_ODR_BIT);
		if (! iocon.equals(start_iocon)) {
			writeByte(getIOConReg(0), iocon.getValue());
		}
	
		for (int port=0; port<numPorts; port++) {
			// Default all GPIOs to output
			writeByte(getIODirReg(port), directions[port].getValue());
			// Default to normal input polarity - IPOLA/IPOLB
			writeByte(getIPolReg(port), (byte) 0);
			// Disable interrupt-on-change for all GPIOs
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
		for (DigitalInputDevice interrupt_gpio : interruptGpios) {
			if (interrupt_gpio != null) {
				Logger.debug("Setting interruptGpio ({}) consumer", Integer.valueOf(interrupt_gpio.getGpio()));
				interrupt_gpio.addListener(this);
			}
		}
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pin_info, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		setInputMode(pin_info.getDeviceNumber(), pud, trigger);
		
		return new MCP23xxxDigitalInputDevice(this, key, pin_info.getDeviceNumber(), trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pin_info, boolean initialValue) {
		setOutputMode(pin_info.getDeviceNumber());
		
		return new MCP23xxxDigitalOutputDevice(this, key, pin_info.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pin_info, DeviceMode mode) {
		return new MCP23xxxDigitalInputOutputDevice(this, key, pin_info.getDeviceNumber(), mode);
	}
	
	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		Logger.warn("Using software PWM on gpio {}", Integer.valueOf(pinInfo.getDeviceNumber()));
		SoftwarePwmOutputDevice pwm = new SoftwarePwmOutputDevice(key, this,
				createDigitalOutputDevice(createPinKey(pinInfo), pinInfo, false), pwmFrequency, initialValue);
		return pwm;
	}

	@Override
	public int getBoardPwmFrequency() {
		return DEFAULT_PWM_FREQUENCY;
	}
	
	@Override
	public void setBoardPwmFrequency(int frequency) {
		Logger.warn("PWM frequency is fixed");
	}

	protected void setInputMode(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		// TODO Detect if there is no change in direction?
		
		byte bit = (byte) (gpio % GPIOS_PER_PORT);
		int port = gpio / GPIOS_PER_PORT;
		
		// Set the following values: direction, pullUp, interruptCompare, defaultValue, interruptOnChange
		directions[port].setBit(bit);
		writeByte(getIODirReg(port), directions[port].getValue());
		byte new_dir = readByte(getIODirReg(port));
		if (directions[port].getValue() != new_dir) {
			Logger.error("Error setting input mode for gpio {}, expected {}, read {}",
					Integer.valueOf(gpio), Byte.valueOf(directions[port].getValue()), Byte.valueOf(new_dir));
		}
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
		// TODO Detect if there is no change in direction?
		
		byte bit = (byte) (gpio % GPIOS_PER_PORT);
		int port = gpio / GPIOS_PER_PORT;
		
		// Set the following values: direction, pullUp, interruptCompare, defaultValue, interruptOnChange
		directions[port].unsetBit(bit);
		writeByte(getIODirReg(port), directions[port].getValue());
	}

	public boolean getValue(int gpio) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numGpios) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ deviceName + " has " + numGpios + " GPIOs; must be 0.." + (numGpios - 1));
		}
		
		byte bit = (byte) (gpio % GPIOS_PER_PORT);
		int port = gpio / GPIOS_PER_PORT;
		
		byte states = readByte(getGPIOReg(port));
		
		return BitManipulation.isBitSet(states, bit);
	}

	public void setValue(int gpio, boolean value) throws RuntimeIOException {
		if (gpio < 0 || gpio >= numGpios) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ deviceName + " has " + numGpios + " GPIOs; must be 0.." + (numGpios - 1));
		}
		
		byte bit = (byte)(gpio % GPIOS_PER_PORT);
		int port = gpio / GPIOS_PER_PORT;
		
		// Check the direction of the GPIO - can't set the output value for input GPIOs (direction bit is set)
		if (directions[port].isBitSet(bit)) {
			throw new IllegalStateException("Can't set value for input GPIO: " + gpio);
		}
		// Read the current state of this bank of GPIOs
		byte old_val = readByte(getGPIOReg(port));
		byte new_val = BitManipulation.setBitValue(old_val, value, bit);
		writeByte(getOLatReg(port), new_val);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		// Close the interrupt GPIOs
		for (DigitalInputDevice interrupt_gpio : interruptGpios) {
			if (interrupt_gpio != null) { interrupt_gpio.close(); }
		}
		// Close all open GPIOs before closing the I2C device itself
		super.close();
	}

	public void closeGpio(int gpio) throws RuntimeIOException {
		Logger.trace("closeGpio({})", Integer.valueOf(gpio));
		
		if (gpio < 0 || gpio >= numGpios) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ deviceName + " has " + numGpios + " GPIOs; must be 0.." + (numGpios - 1));
		}
		
		byte bit = (byte)(gpio % GPIOS_PER_PORT);
		int port = gpio / GPIOS_PER_PORT;
		
		// Clean-up this GPIO only
		
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
		// Default GPIO to input
		if (! directions[port].isBitSet(bit)) {
			directions[port].setBit(bit);
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
		
		// Check the event is for one of the interrupt gpios
		boolean process_event = false;
		for (DigitalInputDevice interrupt_gpio : interruptGpios) {
			if (interrupt_gpio != null && event.getGpio() == interrupt_gpio.getGpio()) {
				process_event = true;
				break;
			}
		}
		if (! process_event) {
			Logger.error("Unexpected interrupt event on gpio {}", Integer.valueOf(event.getGpio()));
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
					if (interruptGpios[0] != null && event.getGpio() == interruptGpios[0].getGpio()) {
						intf[0] = readByte(getIntFReg(0));
						intcap[0] = readByte(getIntCapReg(0));
					} else {
						intf[1] = readByte(getIntFReg(1));
						intcap[1] = readByte(getIntCapReg(1));
					}
				}
				for (int port=0; port<numPorts; port++) {
					for (byte bit=0; bit<8; bit++) {
						if (BitManipulation.isBitSet(intf[port], bit)) {
							int gpio = bit + port*8;
							boolean value = BitManipulation.isBitSet(intcap[port], bit);
							DigitalInputEvent e = new DigitalInputEvent(gpio, event.getEpochTime(), event.getNanoTime(), value);
							// Notify the appropriate input device
							MCP23xxxDigitalInputDevice in_device = getInputDevice((byte) gpio);
							if (in_device != null) {
								in_device.valueChanged(e);
							}
						}
					}
				}
			} catch (Throwable t) {
				// Log and ignore
				Logger.error(t, "IO error handling interrupts: {}", t);
			}
		}
	}

	private MCP23xxxDigitalInputDevice getInputDevice(byte gpio) {
		return getDevice(createPinKey(getBoardPinInfo().getByGpioNumber(gpio)), MCP23xxxDigitalInputDevice.class);
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
	
	@Override
	public final void setDirections(int port, byte directions) {
		writeByte(getIODirReg(port), directions);
	}
	
	public byte getValues(int port) {
		return readByte(getGPIOReg(port));
	}
	
	@Override
	public final void setValues(int port, byte values) {
		writeByte(getOLatReg(port), values);
	}
}
