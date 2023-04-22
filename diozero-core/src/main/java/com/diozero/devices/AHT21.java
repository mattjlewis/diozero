package com.diozero.devices;

import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
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

    private byte[] readMeasurement(boolean withCrc) {
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
        // last byte crc (optional)
        final byte[] resp = new byte[withCrc ? 7 : 6];
        i2c.readBytes(resp);

        return resp;
    }

    private float[] parseResponse(byte[] resp) {
        if (resp.length > 6) { // check last byte CRC
            final byte[] data = new byte[6];
            System.arraycopy(resp, 0, data, 0, data.length);

            final int computedCrc = computeCrc(data);
            final int expectedCrc = toUnsignedInt(resp[6]);

            if (computedCrc != expectedCrc) {
                Logger.debug("CRC check failed");
                return new float[]{-1F, -1F};
            }
        }

        // skip status
        // humidity is next with 20 bits
        final int hum = (toUnsignedInt(resp[1]) << 16 | toUnsignedInt(resp[2]) << 8 | toUnsignedInt(resp[3])) >> 4;
        // temperature is next with 20 bits
        final int temp = (toUnsignedInt(resp[3]) & 0x0F) << 16 | toUnsignedInt(resp[4]) << 8 | toUnsignedInt(resp[5]);

        // magic formula from datasheet
        final float relativeHum = hum * 100F / 1024 / 1024;
        final float temperature = temp * 200F / 1024 / 1024 - 50;

        return new float[]{relativeHum, temperature};
    }

    /**
     * Compute CRC for given data.
     * The initial value of CRC is 0xFF.
     * CRC check type: CRC8/MAXIM
     * Polynomial: X8+X5+X4+1 Poly：0011 0001 0x31 When placed high, it becomes 1000 1100 0x8c
     * Adapted from
     * <a href="http://aosong.com/userfiles/files/software/AHT20-21%20DEMO%20V1_3(1).rar">datasheet reference</a>
     *
     * @param data the data
     * @return the CRC
     */
    private int computeCrc(byte[] data) {
        int crc = 0xFF;

        for (byte current : data) {
            int byteData = Byte.toUnsignedInt(current);
            crc ^= byteData;
            for (int index = 8; index > 0; --index) {
                if ((crc & 0x80) == 0x80) {
                    crc = crc << 1 ^ 0x31;
                } else {
                    crc = crc << 1;
                }
            }
        }

        return crc & 0xFF; // CRC is 1 byte
    }

    /**
     * Read both humidity and temperature.
     *
     * @param verifyCrc whether to check CRC byte. If CRC fails then returned value is [-1F, -1F], but if it passes
     *                  it increases measurement confidence
     * @return [relative humidity, temperature (Celsius)]
     */
    public float[] getValues(boolean verifyCrc) {
        return parseResponse(readMeasurement(verifyCrc));
    }

    @Override
    public float getRelativeHumidity() {
        return getValues(true)[0];
    }

    @Override
    public float getTemperature() throws RuntimeIOException {
        return getValues(true)[1];
    }

    @Override
    public void close() throws RuntimeIOException {
        this.i2c.close();
    }
}
