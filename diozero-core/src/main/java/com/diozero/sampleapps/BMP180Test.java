package com.diozero.sampleapps;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.BMP180;
import com.diozero.BMP180.BMPMode;
import com.diozero.api.I2CConstants;

/**
 * JDK Device I/O 1.0:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.BMP180Test
 * JDK Device I/O 1.1:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.BMP180Test
 * Pi4j:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.BMP180Test
 * wiringPi:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.BMP180Test
 * pigpgioJ:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar -Djava.library.path=. com.diozero.sampleapps.BMP180Test
 */
public class BMP180Test {
	private static final Logger logger = LogManager.getLogger(BMP180Test.class);
	
	public static void main(String[] args) {
		try (BMP180 bmp180 = new BMP180(1, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY)) {
			bmp180.init(BMPMode.STANDARD);
			logger.debug("Opened device");

			double temp = bmp180.getTemperature();
			System.out.format("Temperature=%.2f%n", Double.valueOf(temp));
			double pressure = bmp180.getPressure();
			System.out.format("Pressure=%.2f%n", Double.valueOf(pressure));
		} catch (IOException ioe) {
			logger.error("Error: " + ioe, ioe);
		}
	}
}
