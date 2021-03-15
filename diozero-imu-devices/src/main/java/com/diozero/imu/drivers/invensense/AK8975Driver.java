package com.diozero.imu.drivers.invensense;

import com.diozero.api.DeviceInterface;
import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Output data resolution is 13 bit (0.3 uT per LSB), Full scale measurement
 * range is +/-1200 uT
 */
public class AK8975Driver implements DeviceInterface, AK8975Constants {
	private short[] mag_sens_adj = new short[3];
	private I2CDevice i2cDevice;

	public AK8975Driver(int controller) throws RuntimeIOException {
		this(controller, AK8975_MAG_ADDRESS);
	}

	public AK8975Driver(int controller, int address) throws RuntimeIOException {
		i2cDevice = I2CDevice.builder(address).setController(controller).build();
	}

	public void init() throws RuntimeIOException {
		byte[] data = new byte[4];
		data[0] = AKM_POWER_DOWN;
		i2cDevice.writeByteData(AKM_REG_CNTL, data[0]);
		SleepUtil.sleepMillis(1);

		data[0] = AKM_FUSE_ROM_ACCESS;
		i2cDevice.writeByteData(AKM_REG_CNTL, data[0]);
		SleepUtil.sleepMillis(1);

		/* Get sensitivity adjustment data from fuse ROM. */
		data = i2cDevice.readI2CBlockDataByteArray(AKM_REG_ASAX, 3);
		mag_sens_adj[0] = (short) (data[0] + 128);
		mag_sens_adj[1] = (short) (data[1] + 128);
		mag_sens_adj[2] = (short) (data[2] + 128);

		data[0] = AKM_POWER_DOWN;
		i2cDevice.writeByteData(AKM_REG_CNTL, data[0]);
		SleepUtil.sleepMillis(1);
	}

	public short[] get_mag_sens_adj() {
		return mag_sens_adj;
	}

	@Override
	public void close() {
		i2cDevice.close();
	}
}
