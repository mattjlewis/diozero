/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.diozero.sampleapps;

import java.io.IOException;

import com.diozero.devices.BME280;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

/**
 * Tests BME280 using I2C
 * 
 * @author gregflurry
 */
public class BME280TestI2C {
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("--- USING I2C ----");
		try (BME280 bme280 = new BME280()) {
			for (int i = 0; i < 3; i++) {
				bme280.waitDataAvailable(10, 5);
				float[] tph = bme280.getValues();
				float tF = tph[0] * (9f / 5f) + 32f;
				// System.out.println("T=" + tph[0] + " P=" + tph[1] + " H=" + tph[2]);
				System.out.println("T=" + tF + " P=" + tph[1] + " H=" + tph[2]);

				SleepUtil.sleepSeconds(1);
			}
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
}
