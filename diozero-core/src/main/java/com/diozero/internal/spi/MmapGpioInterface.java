package com.diozero.internal.spi;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;

public interface MmapGpioInterface extends AutoCloseable {
	void initialise();
	@Override
	public void close();
	
	DeviceMode getMode(int gpio);
	void setMode(int gpio, DeviceMode mode);
	void setPullUpDown(int gpio, GpioPullUpDown pud);
	boolean gpioRead(int gpio);
	void gpioWrite(int gpio, boolean value);
}
