package com.diozero.sampleapps;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.diozero.devices.BME680;
import com.diozero.devices.BMP180;
import com.diozero.devices.BMP180.BMPMode;
import com.diozero.devices.LED;
import com.diozero.devices.MCP23008;
import com.diozero.devices.TSL2561;
import com.diozero.devices.TSL2561.TSL2561Package;
import com.diozero.util.DiozeroScheduler;

public class I2CPerfTest {
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		try (BMP180 bmp180 = new BMP180(BMPMode.ULTRA_HIGH_RESOLUTION);
				BME680 bme680 = new BME680();
				TSL2561 tsl2561 = new TSL2561(TSL2561Package.T_FN_CL);
				MCP23008 mcp23008 = new MCP23008();
				LED led1 = new LED(13);
				LED led2 = new LED(mcp23008, 1)) {
			DiozeroScheduler scheduler = DiozeroScheduler.getDaemonInstance();

			scheduler.scheduleAtFixedRate(() -> bmp180.getPressure(), 0, 100, TimeUnit.MICROSECONDS);
			scheduler.scheduleAtFixedRate(() -> bme680.getSensorData(), 0, 100, TimeUnit.MICROSECONDS);
			scheduler.scheduleAtFixedRate(() -> tsl2561.getLuminosity(), 0, 100, TimeUnit.MICROSECONDS);
			scheduler.scheduleAtFixedRate(() -> led1.toggle(), 0, 100, TimeUnit.MICROSECONDS);
			ScheduledFuture<?> led_future = scheduler.scheduleAtFixedRate(() -> led2.toggle(), 0, 100,
					TimeUnit.MICROSECONDS);
			
			// Now wait...
			led_future.get();
		}
	}
}
