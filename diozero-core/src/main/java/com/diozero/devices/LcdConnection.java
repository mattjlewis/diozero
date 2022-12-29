package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     LcdConnection.java
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

import java.util.Arrays;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.mcp23xxx.MCP23xxx;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.BitManipulation;

/**
 * Interface for connecting to LCD displays using 4-bit data mode (D4-D7).
 * <p>
 * Data is packed into single 1-byte payload - 4-bits contain data, the other 4
 * bits control backlight, enable, read/write, and register select flags.
 */
public interface LcdConnection extends AutoCloseable {
    void write(byte value);

    /**
     * Control whether the data bits in the first or last 4-bits
     *
     * @return true if the data bits are in the high nibble (bits 4:7)
     */
    boolean isDataInHighNibble();

    /**
     * Identify the bit in the byte payload that refers to the backlight control
     * flag 1=on, 0=off.
     *
     * @return the backlight control bit number
     */
    int getBacklightBit();

    /**
     * Identify the bit in the byte payload that refers to the enable flag to start
     * read/write.
     * <p>
     * Falling edge triggered
     *
     * @return the enable flag bit number
     */
    int getEnableBit();

    /**
     * Identify the bit in the byte payload that refers to the read/write flag. Not
     * implemented.
     * <p>
     * R/W=0: Write, R/W=1: Read
     *
     * @return the read/write flag bit number
     */
    int getDataReadWriteBit();

    /**
     * Identify the bit in the byte payload that refers to the register select flag.
     * <p>
     * RS=0: Command, RS=1: Data
     *
     * @return the register select flag bit number
     */
    int getRegisterSelectBit();

    @Override
    void close() throws RuntimeIOException;

    /**
     * For connections via a GPIO expansion board.
     */
    abstract class GpioExpansionLcdConnection implements LcdConnection {
        private GpioExpander gpioExpander;
        private int port;
        private boolean dataInHighNibble;
        private int registerSelectBit;
        private int dataReadWriteBit;
        private int enableBit;
        private int backlightBit;

        public GpioExpansionLcdConnection(GpioExpander gpioExpander, int port, boolean dataInHighNibble,
                                          int registerSelectBit, int dataReadWriteBit, int enableBit, int backlightBit) {
            this.gpioExpander = gpioExpander;
            this.port = port;

            gpioExpander.setDirections(port, GpioExpander.ALL_OUTPUT);
        }

        @Override
        public void write(byte values) {
            gpioExpander.setValues(port, values);
        }

        @Override
        public boolean isDataInHighNibble() {
            return dataInHighNibble;
        }

        @Override
        public int getRegisterSelectBit() {
            return registerSelectBit;
        }

        @Override
        public int getDataReadWriteBit() {
            return dataReadWriteBit;
        }

        @Override
        public int getEnableBit() {
            return enableBit;
        }

        @Override
        public int getBacklightBit() {
            return backlightBit;
        }

        @Override
        public void close() throws RuntimeIOException {
            gpioExpander.close();
        }
    }

    /**
     * Connect via an Output Shift Register.
     */
    class OutputShiftRegisterLcdConnection extends GpioExpansionLcdConnection {
        /**
         * Constructor.
         *
         * @param osr               the output shift register instance
         * @param port              the port containing the 8 outputs that are connected
         *                          to the LCD
         * @param dataInHighNibble  set to true if the d4:d7 of the LCD are connected to
         *                          GPIOs 4-7 in the specified port
         * @param registerSelectBit the output number in the specified port that is
         *                          connected to the RS pin of the LCD
         * @param dataReadWriteBit  the output number in the specified port that is
         *                          connected to the RW pin of the LCD
         * @param enableBit         the output number in the specified port that is
         *                          connected to the E pin of the LCD
         * @param backlightBit      the output number in the specified port that is
         *                          connected to the A pin of the LCD
         */
        public OutputShiftRegisterLcdConnection(OutputShiftRegister osr, int port, boolean dataInHighNibble,
                                                int registerSelectBit, int dataReadWriteBit, int enableBit, int backlightBit) {
            super(osr, port, dataInHighNibble, registerSelectBit, dataReadWriteBit, enableBit, backlightBit);
        }
    }

    /**
     * MCP23S17 GPIOB to HD44780.
     * <p>
     * Wiring:
     *
     * <pre>
     * PH_PIN_D4 = 0
     * PH_PIN_D5 = 1
     * PH_PIN_D6 = 2
     * PH_PIN_D7 = 3
     * PH_PIN_ENABLE = 4
     * PH_PIN_RW = 5
     * PH_PIN_RS = 6
     * PH_PIN_LED_EN = 7
     * </pre>
     */
    class PiFaceCadLcdConnection extends GpioExpansionLcdConnection {
        private static final int CHIP_SELECT = 1;
        private static final int ADDRESS = 0;
        private static final int PORT = 1;

        private static final byte REGISTER_SELECT_BIT = 6;
        private static final byte DATA_READ_WRITE_BIT = 5;
        private static final byte ENABLE_BIT = 4;
        private static final int BACKLIGHT_BIT = 7;

        public PiFaceCadLcdConnection(int controller) {
            super(new MCP23S17(controller, CHIP_SELECT, ADDRESS, MCP23xxx.INTERRUPT_GPIO_NOT_SET), PORT, false,
                    REGISTER_SELECT_BIT, DATA_READ_WRITE_BIT, ENABLE_BIT, BACKLIGHT_BIT);
        }
    }

    /**
     * Connected via the PCF8574 I2C GPIO expansion backpack Default PCF8574 GPIO to
     * HD44780 pin map:
     *
     * <pre>
     * PH_PIN_RS = 0
     * PH_PIN_RW = 1
     * PH_PIN_ENABLE = 2
     * PH_PIN_LED_EN = 3
     * PH_PIN_D4 = 4
     * PH_PIN_D5 = 5
     * PH_PIN_D6 = 6
     * PH_PIN_D7 = 7
     * </pre>
     */
    class PCF8574LcdConnection extends GpioExpansionLcdConnection {
        // Default I2C device address for the PCF8574
        public static final int DEFAULT_DEVICE_ADDRESS = 0x27;
        private static final int PORT = 0;

        private static final byte REGISTER_SELECT_BIT = 0;
        private static final byte DATA_READ_WRITE_BIT = 1;
        private static final byte ENABLE_BIT = 2;
        private static final int BACKLIGHT_BIT = 3;

        public PCF8574LcdConnection(int controller) {
            this(controller, DEFAULT_DEVICE_ADDRESS);
        }

        public PCF8574LcdConnection(int controller, int deviceAddress) {
            super(new PCF8574(controller, deviceAddress), PORT, true, REGISTER_SELECT_BIT, DATA_READ_WRITE_BIT,
                    ENABLE_BIT, BACKLIGHT_BIT);
        }
    }

    /**
     * Connect via individual GPIO pins, uses 4-bit mode (data pins D4-D7).
     * <p>
     * Wiring (from left-to-right):
     *
     * <pre>
     * Vss: GND
     * Vdd: 5v
     * V0: Contrast adjustment (connect to Vdd for full brightness)
     * RS: Register Select - GPIO
     * RW: Data read/write (not required, read mode not used - can connect to GND)
     * E: Enable - GPIO
     * D0-D3: Don't connect (currently only 4-bit mode is supported)
     * D4-D7: Data pins - GPIO
     * A: Backlight LED Cathode (+) - GPIO (need to check 3v3/5v) or Vdd (always on)
     * K: Backlight LED Anode (-) - GND
     * </pre>
     */
    class GpioLcdConnection implements LcdConnection {
        private static final int BACKLIGHT_BIT = 4;
        private static final int ENABLE_BIT = 5;
        private static final int DATA_RW_BIT = 6;
        private static final int REGISTER_SELECT_BIT = 7;

        private DigitalOutputDevice[] dataPins;
        private DigitalOutputDevice backlightPin;
        private DigitalOutputDevice enablePin;
        private DigitalOutputDevice dataRwPin;
        private DigitalOutputDevice registerSelectPin;

        /**
         * Use the default device factory and specify GPIO numbers. Assumes the RW pin
         * is pulled low and the backlight pin pulled high.
         *
         * @param d4                 GPIO number for d4 pin
         * @param d5                 GPIO number for d5 pin
         * @param d6                 GPIO number for d6 pin
         * @param d7                 GPIO number for d7 pin
         * @param enableGpio         enable GPIO number
         * @param registerSelectGpio register select GPIO number
         */
        public GpioLcdConnection(int d4, int d5, int d6, int d7, int enableGpio, int registerSelectGpio) {
            this(new DigitalOutputDevice(d4), new DigitalOutputDevice(d5), new DigitalOutputDevice(d6),
                    new DigitalOutputDevice(d7), null, new DigitalOutputDevice(enableGpio), null,
                    new DigitalOutputDevice(registerSelectGpio));
        }

        /**
         * Use the default device factory and specify GPIO numbers. Assumes the RW pin
         * is pulled low.
         *
         * @param d4                 GPIO number for d4 pin
         * @param d5                 GPIO number for d5 pin
         * @param d6                 GPIO number for d6 pin
         * @param d7                 GPIO number for d7 pin
         * @param backlightGpio      backlight control GPIO number (set to -1 if not
         *                           connected)
         * @param enableGpio         enable GPIO number
         * @param registerSelectGpio register select GPIO number
         */
        public GpioLcdConnection(int d4, int d5, int d6, int d7, int backlightGpio, int enableGpio,
                                 int registerSelectGpio) {
            this(new DigitalOutputDevice(d4), new DigitalOutputDevice(d5), new DigitalOutputDevice(d6),
                    new DigitalOutputDevice(d7), backlightGpio == -1 ? null : new DigitalOutputDevice(backlightGpio),
                    new DigitalOutputDevice(enableGpio), null, new DigitalOutputDevice(registerSelectGpio));
        }

        /**
         * Use the default device factory and specify GPIO numbers.
         *
         * @param d4                 GPIO number for d4 pin
         * @param d5                 GPIO number for d5 pin
         * @param d6                 GPIO number for d6 pin
         * @param d7                 GPIO number for d7 pin
         * @param backlightGpio      backlight control GPIO number (set to -1 if not
         *                           connected)
         * @param enableGpio         enable GPIO number
         * @param dataRwGpio         data read/write GPIO number (not used - connect to
         *                           GND, set to -1 if not connected)
         * @param registerSelectGpio register select GPIO number
         */
        public GpioLcdConnection(int d4, int d5, int d6, int d7, int backlightGpio, int enableGpio, int dataRwGpio,
                                 int registerSelectGpio) {
            this(new DigitalOutputDevice(d4), new DigitalOutputDevice(d5), new DigitalOutputDevice(d6),
                    new DigitalOutputDevice(d7), backlightGpio == -1 ? null : new DigitalOutputDevice(backlightGpio),
                    new DigitalOutputDevice(enableGpio), dataRwGpio == -1 ? null : new DigitalOutputDevice(dataRwGpio),
                    new DigitalOutputDevice(registerSelectGpio));
        }

        /**
         * Use the specified device factory and specify GPIO numbers.
         *
         * @param deviceFactory      the device factory to use for provisioning the
         *                           GPIOs
         * @param d4                 GPIO number for d4 pin
         * @param d5                 GPIO number for d5 pin
         * @param d6                 GPIO number for d6 pin
         * @param d7                 GPIO number for d7 pin
         * @param backlightGpio      backlight control GPIO number (set to -1 if not
         *                           connected)
         * @param enableGpio         enable GPIO number
         * @param dataRwGpio         data read/write GPIO number (not used - connect to
         *                           GND, set to -1 if not connected)
         * @param registerSelectGpio register select GPIO number
         */
        public GpioLcdConnection(GpioDeviceFactoryInterface deviceFactory, int d4, int d5, int d6, int d7,
                                 int backlightGpio, int enableGpio, int dataRwGpio, int registerSelectGpio) {
            this(DigitalOutputDevice.Builder.builder(d4).setDeviceFactory(deviceFactory).build(),
                    DigitalOutputDevice.Builder.builder(d5).setDeviceFactory(deviceFactory).build(),
                    DigitalOutputDevice.Builder.builder(d6).setDeviceFactory(deviceFactory).build(),
                    DigitalOutputDevice.Builder.builder(d7).setDeviceFactory(deviceFactory).build(),
                    backlightGpio == -1 ? null
                            : DigitalOutputDevice.Builder.builder(backlightGpio).setDeviceFactory(deviceFactory)
                            .build(),
                    DigitalOutputDevice.Builder.builder(enableGpio).setDeviceFactory(deviceFactory).build(),
                    dataRwGpio == -1 ? null
                            : DigitalOutputDevice.Builder.builder(dataRwGpio).setDeviceFactory(deviceFactory).build(),
                    DigitalOutputDevice.Builder.builder(registerSelectGpio).setDeviceFactory(deviceFactory).build());
        }

        /**
         * Use the specified digital output devices.
         *
         * @param d4                Digital output device for d4 pin
         * @param d5                Digital output device for d5 pin
         * @param d6                Digital output device for d6 pin
         * @param d7                Digital output device for d7 pin
         * @param backlightPin      backlight control digital output device (set to null
         *                          if not connected)
         * @param enablePin         enable digital output device
         * @param dataRwPin         data read/write digital output device (not used -
         *                          connect to GND, set to null if not connected)
         * @param registerSelectPin register select digital output device
         */
        public GpioLcdConnection(DigitalOutputDevice d4, DigitalOutputDevice d5, DigitalOutputDevice d6,
                                 DigitalOutputDevice d7, DigitalOutputDevice backlightPin, DigitalOutputDevice enablePin,
                                 DigitalOutputDevice dataRwPin, DigitalOutputDevice registerSelectPin) {
            dataPins = new DigitalOutputDevice[4];
            dataPins[0] = d4;
            dataPins[1] = d5;
            dataPins[2] = d6;
            dataPins[3] = d7;

            this.backlightPin = backlightPin;
            this.enablePin = enablePin;
            this.dataRwPin = dataRwPin;
            this.registerSelectPin = registerSelectPin;
        }

        @Override
        public void write(byte values) {
            if (backlightPin != null) {
                backlightPin.setValue(BitManipulation.isBitSet(values, BACKLIGHT_BIT));
            }
            enablePin.setValue(BitManipulation.isBitSet(values, ENABLE_BIT));
            if (dataRwPin != null) {
                dataRwPin.setValue(BitManipulation.isBitSet(values, DATA_RW_BIT));
            }
            registerSelectPin.setValue(BitManipulation.isBitSet(values, REGISTER_SELECT_BIT));

            for (int i = 0; i < dataPins.length; i++) {
                dataPins[i].setValue(BitManipulation.isBitSet(values, i));
            }
        }

        @Override
        public boolean isDataInHighNibble() {
            return false;
        }

        @Override
        public int getBacklightBit() {
            return BACKLIGHT_BIT;
        }

        @Override
        public int getEnableBit() {
            return ENABLE_BIT;
        }

        @Override
        public int getDataReadWriteBit() {
            return DATA_RW_BIT;
        }

        @Override
        public int getRegisterSelectBit() {
            return REGISTER_SELECT_BIT;
        }

        @Override
        public void close() throws RuntimeIOException {
            Arrays.asList(dataPins).forEach(DeviceInterface::close);
            if (backlightPin != null) {
                backlightPin.close();
            }
            enablePin.close();
            if (dataRwPin != null) {
                dataRwPin.close();
            }
            registerSelectPin.close();
        }
    }
}
