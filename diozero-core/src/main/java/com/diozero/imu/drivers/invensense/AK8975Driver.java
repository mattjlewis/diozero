package com.diozero.imu.drivers.invensense;

import java.io.IOException;

import com.diozero.api.I2CDevice;
import com.diozero.util.SleepUtil;

/**
 * Output data resolution is 13 bit (0.3 uT per LSB), Full scale measurement range is +/-1200 uT
 */
public class AK8975Driver extends I2CDevice implements AK8975Constants {
    private short[] mag_sens_adj = new short[3];
    
	public AK8975Driver(int controllerNumber, int addressSize, int clockFrequency) throws IOException {
		this(controllerNumber, addressSize, clockFrequency, AK8975_MAG_ADDRESS);
	}

	public AK8975Driver(int controllerNumber, int addressSize, int clockFrequency, int address) throws IOException {
		super(controllerNumber, address, addressSize, clockFrequency);
	}
	
	public void init() throws IOException {
	    byte[] data = new byte[4];
	    data[0] = AKM_POWER_DOWN;
	    writeByte(AKM_REG_CNTL, data[0]);
	    SleepUtil.sleepMillis(1);

	    data[0] = AKM_FUSE_ROM_ACCESS;
	    writeByte(AKM_REG_CNTL, data[0]);
	    SleepUtil.sleepMillis(1);

	    /* Get sensitivity adjustment data from fuse ROM. */
	    data = readBytes(AKM_REG_ASAX, 3);
	    mag_sens_adj[0] = (short)(data[0] + 128);
	    mag_sens_adj[1] = (short)(data[1] + 128);
	    mag_sens_adj[2] = (short)(data[2] + 128);

	    data[0] = AKM_POWER_DOWN;
	    writeByte(AKM_REG_CNTL, data[0]);
	    SleepUtil.sleepMillis(1);
	}

	public short[] get_mag_sens_adj() {
		return mag_sens_adj;
	}
}
