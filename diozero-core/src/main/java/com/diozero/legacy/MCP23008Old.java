package com.diozero.legacy;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.mcp23xxx.MCP23x08;
import com.diozero.internal.spi.*;
import com.diozero.util.*;

/**
 * Datasheet: <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21919e.pdf">http://ww1.microchip.com/downloads/en/DeviceDoc/21919e.pdf</a>.
 * <p>The MCP23X08 consists of multiple 8-bit configuration registers for input, output and polarity selection. The
 * system master can enable the I/Os as either inputs or outputs by writing the I/O configuration bits (IODIRA/B).
 * The data for each input or output is kept in the corresponding input or output register. The polarity of
 * the Input Port register can be inverted with the Polarity Inversion register. All registers can be read by the
 * system master.</p>
 * <p>The interrupt output can be configured to activate under two conditions (mutually exclusive):</p>
 * <ol>
 * <li>When any input state differs from its corresponding input port register state, this is used to indicate to the
 * system master that an input state has changed.</li>
 * <li>When an input state differs from a preconfigured register value (DEFVAL register).</li>
 * </ol>
 * <p>The Interrupt Capture register captures port values at the time of the interrupt, thereby saving the condition
 * that caused the interrupt.</p>
 * <p>The Power-on Reset (POR) sets the registers to their default values and initializes the device state machine.</p>
 * <p>The hardware address pins are used to determine the device address.</p>
 */
public class MCP23008Old extends AbstractDeviceFactory
implements GpioDeviceFactoryInterface, InputEventListener<DigitalInputEvent>, GpioExpander {
	public static enum InterruptMode {
		DISABLED, MIRRORED;
	}

	// Default I2C address
	private static final int DEVICE_ADDRESS = 0x20;
	private static final String DEVICE_NAME = "MCP23008";
	
	/** Controls the direction of the data I/O. When a bit is set, the corresponding pin becomes an
	 * input. When a bit is clear, the corresponding pin becomes an output */
	private static final int[] IODIR_REG = { 0x00 };
	/** This register allows the user to configure the polarity on the corresponding GPIO port bits.
	 * If a bit is set, the corresponding GPIO register bit will reflect the inverted value on the pin */
	private static final int[] IPOL_REG = { 0x01 };
	/** The GPINTEN register controls the interrupt-on-change feature for each pin. If a bit is set,
	 * the corresponding pin is enabled for interrupt-on-change. The DEFVAL and INTCON registers
	 * must also be configured if any pins are enabled for interrupt-on-change */
	private static final int[] GPINTEN_REG = { 0x02 };
	/** The default comparison value is configured in the DEFVAL register. If enabled
	 * (via GPINTEN and INTCON) to compare against the DEFVAL register, an opposite
	 * value on the associated pin will cause an interrupt to occur */
	private static final int[] DEFVAL_REG = { 0x03 };
	/** The INTCON register controls how the associated pin value is compared for the
	 * interrupt-on-change feature. If a bit is set, the corresponding I/O pin is compared
	 * against the associated bit in the DEFVAL register. If a bit value is clear, the
	 * corresponding I/O pin is compared against the previous value */
	private static final int[] INTCON_REG = { 0x04 };
	/** I/O configuration register */
	private static final int[] IOCON_REG = { 0x05 };
	/** The GPPU register controls the pull-up resistors for the port pins. If a bit is
	 * set and the corresponding pin is configured as an input, the corresponding port pin is
	 * internally pulled up with a 100 kOhm resistor */
	private static final int[] GPPU_REG = { 0x06 };
	/** The INTF register reflects the interrupt condition on the port pins of any pin that is
	 * enabled for interrupts via the GPINTEN register. A 'set' bit indicates that the
	 * associated pin caused the interrupt. This register is 'read-only'. Writes to this
	 * register will be ignored */
	private static final int[] INTF_REG = { 0x07 };
	/** The INTCAP register captures the GPIO port value at the time the interrupt occurred.
	 * The register is 'read-only' and is updated only when an interrupt occurs. The register
	 * will remain unchanged until the interrupt is cleared via a read of INTCAP or GPIO. */
	private static final int[] INTCAP_REG = { 0x08 };
	/** The GPIO register reflects the value on the port. Reading from this register reads
	 * the port. Writing to this register modifies the Output Latch (OLAT) register */
	private static final int[] GPIO_REG = { 0x09 };
	/** The OLAT register provides access to the output latches. A read from this register
	 * results in a read of the OLAT and not the port itself. A write to this register
	 * modifies the output latches that modifies the pins configured as outputs */
	private static final int[] OLAT_REG = { 0x0A };
	
	/** Sequential Operation mode bit
	 * 1 = Sequential operation disabled, address pointer does not increment.
	 * 0 = Sequential operation enabled, address pointer increments */
	private static final byte IOCON_SEQOP_BIT = 5;
	/** Slew Rate control bit for SDA output
	 * 1 = Slew rate disabled.
	 * 0 = Slew rate enabled */
	//private static final byte IOCON_DISSLW_BIT = 4;
	/** Hardware Address Enable bit (MCP23S08 only). Address pins are always enabled on MCP23008
	 * 1 = Enables the MCP23S08 address pins.
	 * 0 = Disables the MCP23S08 address pins */
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
	private static final int PORTS = 1;
	private static final int NUM_PINS = PORTS*PINS_PER_PORT;
	private static final int INTERRUPT_GPIO_NOT_SET = -1;

	private I2CDevice device;
	private BoardPinInfo boardPinInfo;
	private DigitalInputDevice interruptGpio;
	private MutableByte[] directions = { new MutableByte(), new MutableByte() };
	private MutableByte[] pullUps = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptOnChangeFlags = { new MutableByte(), new MutableByte() };
	private MutableByte[] defaultValues = { new MutableByte(), new MutableByte() };
	private MutableByte[] interruptCompareFlags = { new MutableByte(), new MutableByte() };
	private InterruptMode interruptMode = InterruptMode.DISABLED;

	public MCP23008Old() throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, INTERRUPT_GPIO_NOT_SET);
	}

	public MCP23008Old(int interruptGpio) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptGpio);
	}

	public MCP23008Old(int controller, int address, int interruptGpioNumber) throws RuntimeIOException {
		super(DEVICE_NAME + "-" + controller + "-" + address);
		
		boardPinInfo = new MCP23x08.MCP23x08BoardPinInfo();
		device = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		
		if (interruptGpioNumber != INTERRUPT_GPIO_NOT_SET) {
			interruptGpio = new DigitalInputDevice(interruptGpioNumber, GpioPullUpDown.NONE, GpioEventTrigger.RISING);
		}

		// Initialise
		// Read the I/O configuration value
		byte start_iocon = device.readByte(IOCON_REG[0]);
		Logger.debug("Default power-on values for IOCON: 0x{x}", Integer.toHexString(start_iocon));
		
		// Configure interrupts
		MutableByte iocon = new MutableByte(start_iocon);
		if (interruptMode == InterruptMode.MIRRORED) {
			iocon.setBit(IOCON_INTPOL_BIT);
		} else if (interruptMode != InterruptMode.DISABLED) {
			iocon.setBit(IOCON_INTPOL_BIT);
		}
		iocon.setBit(IOCON_SEQOP_BIT);
		iocon.unsetBit(IOCON_ODR_BIT);
		if (!iocon.equals(start_iocon)) {
			Logger.debug("Updating IOCONA to: 0x{x}", Integer.toHexString(iocon.getValue()));
			device.writeByte(IOCON_REG[0], iocon.getValue());
		}
		
		for (int port=0; port<PORTS; port++) {
			// Default all gpios to output
			setDirections(port, directions[port].getValue());
			// Default to normal input polarity - IPOLA/IPOLB
			device.writeByte(IPOL_REG[port], 0);
			// Disable interrupt-on-change for all gpios
			device.writeByte(GPINTEN_REG[port], interruptOnChangeFlags[port].getValue());
			// Set default compare values to 0
			device.writeByte(DEFVAL_REG[port], defaultValues[port].getValue());
			// Disable interrupt comparison control
			device.writeByte(INTCON_REG[port], interruptCompareFlags[port].getValue());
			// Disable pull-up resistors
			device.writeByte(GPPU_REG[port], pullUps[port].getValue());
			// Set all values to off
			device.writeByte(GPIO_REG[port], 0);
		}
		
		// Finally enable interrupt listeners
		if (interruptGpio != null) {
			Logger.debug("Setting interruptGpio ({}) consumer", Integer.valueOf(interruptGpio.getGpio()));
			interruptGpio.addListener(this);
		}
	}

	@Override
	public String getName() {
		return DEVICE_NAME + "-" + device.getController() + "-" + device.getAddress();
	}
	
	private void setInput(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		// Set the following values: direction, pullUp, interruptCompare, defaultValue, interruptOnChange
		directions[port].setBit(bit);
		setDirections(port, directions[port].getValue());
		if (pud == GpioPullUpDown.PULL_UP) {
			pullUps[port].setBit(bit);
			device.writeByte(GPPU_REG[port], pullUps[port].getValue());
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
			device.writeByte(DEFVAL_REG[port], defaultValues[port].getValue());
			device.writeByte(INTCON_REG[port], interruptCompareFlags[port].getValue());
			device.writeByte(GPINTEN_REG[port], interruptOnChangeFlags[port].getValue());
		}
	}
	
	private void setOutput(int gpio) {
		// TODO Nothing to do assuming that closing a pin resets it to the default output state?
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		int gpio = pinInfo.getDeviceNumber();
		setInput(gpio, pud, trigger);
		return new MCP23008DigitalInputDevice(this, key, gpio, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		int gpio = pinInfo.getDeviceNumber();
		setOutput(gpio);
		return new MCP23008DigitalOutputDevice(this, key, gpio, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		throw new UnsupportedOperationException("Digital Input / Output devices not yet supported by this provider");
	}

	public boolean getValue(int gpio) throws RuntimeIOException {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; must be 0.." + (NUM_PINS - 1));
		}
		
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		byte states = device.readByte(GPIO_REG[port]);
		
		return (states & bit) != 0;
	}

	public void setValue(int gpio, boolean value) throws RuntimeIOException {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; must be 0.." + (NUM_PINS - 1));
		}
		
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		// Check the direction of the pin - can't set the value of input pins (direction bit is set)
		if (directions[port].isBitSet(bit)) {
			throw new IllegalStateException("Can't set value for input pin: " + gpio);
		}
		// Read the current state of this bank of GPIOs
		byte old_val = device.readByte(GPIO_REG[port]);
		byte new_val = BitManipulation.setBitValue(old_val, value, bit);
		setValues(port, new_val);
	}
	
	@Override
	public void setDirections(int port, byte directions) {
		device.writeByte(IODIR_REG[port], directions);
	}
	
	@Override
	public void setValues(int port, byte values) {
		device.writeByte(OLAT_REG[port], values);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		// Close the interrupt gpio
		if (interruptGpio != null) { interruptGpio.close(); }
		// Close all open gpios before closing the I2C device itself
		shutdown();
		device.close();
	}

	public void closePin(int gpio) throws RuntimeIOException {
		Logger.debug("closePin({})", Integer.valueOf(gpio));
		
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; must be 0.." + (NUM_PINS - 1));
		}
		
		byte bit = (byte)(gpio % PINS_PER_PORT);
		int port = gpio / PINS_PER_PORT;
		
		// Clean-up this pin only
		
		if (interruptOnChangeFlags[port].isBitSet(bit)) {
			interruptOnChangeFlags[port].unsetBit(bit);
			device.writeByte(GPINTEN_REG[port], interruptOnChangeFlags[port].getValue());
		}
		if (defaultValues[port].isBitSet(bit)) {
			defaultValues[port].unsetBit(bit);
			device.writeByte(DEFVAL_REG[port], defaultValues[port].getValue());
		}
		if (interruptCompareFlags[port].isBitSet(bit)) {
			interruptCompareFlags[port].unsetBit(bit);
			device.writeByte(INTCON_REG[port], interruptCompareFlags[port].getValue());
		}
		if (pullUps[port].isBitSet(bit)) {
			pullUps[port].unsetBit(bit);
			device.writeByte(GPPU_REG[port], pullUps[port].getValue());
		}
		if (directions[port].isBitSet(bit)) {
			directions[port].unsetBit(bit);
			setDirections(port, directions[port].getValue());
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
		
		if (event.getGpio() != interruptGpio.getGpio()) {
			Logger.error("Unexpected input event on gpio {}", Integer.valueOf(event.getGpio()));
			return;
		}
		
		synchronized (this) {
			try {
				byte[] intf = new byte[PORTS];
				byte[] intcap = new byte[PORTS];
				for (int port=0; port<PORTS; port++) {
					intf[port] = device.readByte(INTF_REG[port]);
					intcap[port] = device.readByte(INTCAP_REG[port]);
				}
				Logger.debug("Interrupt values: [A]=(0x{}, 0x{})",
						Integer.toHexString(intf[0]), Integer.toHexString(intcap[0]));
				for (int port=0; port<PORTS; port++) {
					for (byte bit=0; bit<8; bit++) {
						if (BitManipulation.isBitSet(intf[port], bit)) {
							boolean value = BitManipulation.isBitSet(intcap[port], bit);
							DigitalInputEvent e = new DigitalInputEvent(bit, event.getEpochTime(), event.getNanoTime(), value);
							// Notify the appropriate input device
							MCP23008DigitalInputDevice device = getInputDevice((byte) (bit+8*port));
							if (device != null) {
								device.valueChanged(e);
							}
						}
					}
				}
			} catch (RuntimeIOException e) {
				// Log and ignore
				Logger.error(e, "IO error handling interrupts: {}", e);
			}
		}
	}

	private MCP23008DigitalInputDevice getInputDevice(byte gpio) {
		return getDevice(createPinKey(getBoardPinInfo().getByGpioNumber(gpio)), MCP23008DigitalInputDevice.class);
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	public static class MCP23008DigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
		private MCP23008Old mcp23008;
		private int gpio;
	
		public MCP23008DigitalOutputDevice(MCP23008Old mcp23008, String key, int gpio, boolean initialValue) {
			super(key, mcp23008);
			
			this.mcp23008 = mcp23008;
			this.gpio = gpio;
			
			setValue(initialValue);
		}
	
		@Override
		public boolean getValue() throws RuntimeIOException {
			return mcp23008.getValue(gpio);
		}
	
		@Override
		public void setValue(boolean value) throws RuntimeIOException {
			mcp23008.setValue(gpio, value);
		}
	
		@Override
		public int getGpio() {
			return gpio;
		}
	
		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			mcp23008.closePin(gpio);
		}
	}
	
	public static class MCP23008DigitalInputDevice extends AbstractInputDevice<DigitalInputEvent> implements GpioDigitalInputDeviceInterface {
		private MCP23008Old mcp23008;
		private int gpio;
		private GpioEventTrigger trigger;
	
		public MCP23008DigitalInputDevice(MCP23008Old mcp23008, String key, int gpio, GpioEventTrigger trigger) {
			super(key, mcp23008);
	
			this.mcp23008 = mcp23008;
			this.gpio = gpio;
			this.trigger = trigger;
		}
	
		@Override
		public void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			removeListener();
			mcp23008.closePin(gpio);
		}
	
		@Override
		public boolean getValue() throws RuntimeIOException {
			return mcp23008.getValue(gpio);
		}
	
		@Override
		public int getGpio() {
			return gpio;
		}
	
		@Override
		public void setDebounceTimeMillis(int debounceTime) {
			// TODO Auto-generated method stub
		}
	}
}
