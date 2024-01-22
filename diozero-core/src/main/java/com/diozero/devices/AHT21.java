package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AHT21.java
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

import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.Crc;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;
import org.tinylog.Logger;

import java.nio.ByteOrder;

import static java.lang.Byte.toUnsignedInt;

/*
 Datasheet:
 https://asairsensors.com/wp-content/uploads/2021/09/Data-Sheet-AHT21-Humidity-and-Temperature-Sensor-ASAIR-V1.0.03.pdf
 */

public class AHT21 implements ThermometerInterface, HygrometerInterface {
    public static final int DEFAULT_I2C_ADDRESS = 0x38;
    public static final byte READ_COMMAND = 0x71;
    public static final byte WRITE_COMMAND = 0x70;
    public static final byte[] MEASURE_COMMAND = new byte[]{(byte) 0xAC, 0x33, 0x00};

    private static final float TWO_TO_THE_POWER_OF_20 = 1_048_576;
    private static final Crc.Params CRC8_PARAMS = new Crc.Params(0x31, 0xff, false, false, 0x00);

    private final I2CDeviceInterface i2c;

    public AHT21() {
        this(1);
    }

    public AHT21(int controller) {
        this(DEFAULT_I2C_ADDRESS, controller);
    }

    public AHT21(int address, int controller) {
        this.i2c = I2CDevice.builder(address)
                .setController(controller)
                .setByteOrder(ByteOrder.BIG_ENDIAN)
                .build();

        setUpAHT21();
    }

    private void setUpAHT21() {
        /*
        After power-on, wait for ≥100ms. Before reading the temperature and humidity value, get a byte of status word
        by sending 0x71. If the status word and 0x18 are not equal to 0x18, initialize the 0x1B, 0x1C, 0x1E registers,
        details Please refer to official website routine for the initialization process; if they are equal, proceed
        to the next step.
         */
        SleepUtil.sleepMillis(100);
        i2c.writeByte(READ_COMMAND);
        final byte stateByte = i2c.readByte();
        if ((stateByte & 0x18) != 0x18) {
            Logger.debug("Initialization required, state: {}", Hex.encode(stateByte));
            initRegistries();
        }

        // Wait 10ms before sending 0xAC command (trigger measurement).
        SleepUtil.sleepMillis(10);
    }

    /**
     * Initialize the 0x1B, 0x1C, 0x1E registers.
     */
    private void initRegistries() {
        resetRegistry((byte) 0x1B);
        resetRegistry((byte) 0x1C);
        resetRegistry((byte) 0x1E);
    }

    /**
     * Reset registry.
     * <b>Implementation note:</b> Adapted from
     * <a href="http://aosong.com/userfiles/files/software/AHT20-21%20DEMO%20V1_3(1).rar">datasheet reference</a>
     *
     * @param registry the registry
     * @see <a href="http://aosong.com/userfiles/files/software/AHT20-21%20DEMO%20V1_3(1).rar">datasheet reference</a>
     */
    private void resetRegistry(byte registry) {
        i2c.writeByte(WRITE_COMMAND);
        i2c.writeBytes(registry, (byte) 0x00, (byte) 0x00);

        SleepUtil.sleepMillis(5);
        i2c.writeBytes(READ_COMMAND);
        final byte[] resp = new byte[3];
        i2c.readBytes(resp);

        SleepUtil.sleepMillis(10);
        i2c.writeByte(WRITE_COMMAND);
        final byte firstByte = (byte) (0xB0 | toUnsignedInt(registry));
        i2c.writeBytes(firstByte, resp[1], resp[2]);
    }

    private void checkCrc(byte[] resp) {
        final byte[] data = new byte[6];
        System.arraycopy(resp, 0, data, 0, data.length);

        final int computedCrc = Crc.crc8(CRC8_PARAMS, data);
        final int expectedCrc = toUnsignedInt(resp[6]);

        if (computedCrc != expectedCrc) {
            Logger.debug("CRC check failed");
            throw new RuntimeIOException("Sensor returned invalid data");
        }
    }

    /**
     * Read both humidity and temperature.
     *
     * @return [relative humidity[%], temperature (Celsius)]
     */
    public float[] getValues() {
        i2c.writeBytes(MEASURE_COMMAND);

        /*
        Wait 80ms for the measurement to be completed, if the read status word Bit[7] is 0, it means the measurement
        is completed, and then six bytes can be read continuously; otherwise, continue to wait.
         */
        SleepUtil.sleepMillis(80);
        while ((i2c.readByte() & 0x80) == 0x80) { //first bit of byte = 1 => device busy
            Logger.debug("Device busy");
            SleepUtil.sleepMillis(10);
        }

        /*
        After receiving six bytes, the next byte is CRC check data, which the user can read as needed. If the receiver
        needs CRC check, it will send an ACK reply after receiving the sixth byte, otherwise it will send a NACK reply.
         */

        i2c.writeByte(READ_COMMAND);
        // 1st status byte,
        // then 5 bytes with hum & temp,
        // last byte crc
        final byte[] resp = new byte[7];
        i2c.readBytes(resp);

        checkCrc(resp);
        // CRC ok, time to parse

        // skip status
        // humidity is next with 20 bits
        final int hum = (toUnsignedInt(resp[1]) << 16 | toUnsignedInt(resp[2]) << 8 | toUnsignedInt(resp[3])) >> 4;
        // temperature is next with 20 bits
        final int temp = (toUnsignedInt(resp[3]) & 0x0F) << 16 | toUnsignedInt(resp[4]) << 8 | toUnsignedInt(resp[5]);

        // magic formula from datasheet
        final float relativeHum = (hum / TWO_TO_THE_POWER_OF_20) * 100;         // RH[%] = (raw / 2^20) * 100
        final float temperature = (temp / TWO_TO_THE_POWER_OF_20) * 200 - 50;   //  T[C] = (raw / 2^20) * 200 - 50

        return new float[]{relativeHum, temperature};
    }

    @Override
    public float getRelativeHumidity() {
        return getValues()[0];
    }

    @Override
    public float getTemperature() throws RuntimeIOException {
        return getValues()[1];
    }

    @Override
    public void close() throws RuntimeIOException {
        this.i2c.close();
    }
}
