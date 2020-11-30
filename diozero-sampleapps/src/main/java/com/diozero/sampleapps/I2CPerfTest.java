package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     I2CPerfTest.java  
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
