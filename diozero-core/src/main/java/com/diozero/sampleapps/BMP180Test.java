package com.diozero.sampleapps;

import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.BMP180;
import com.diozero.BMP180.BMPMode;
import com.diozero.api.I2CConstants;

/**
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.BMP180Test
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.BMP180Test
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.BMP180Test
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.BMP180Test
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar -Djava.library.path=. com.diozero.sampleapps.BMP180Test
 */
public class BMP180Test {
	public static void main(String[] args) {
		try (BMP180 bmp180 = new BMP180(1, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY)) {
			bmp180.init(BMPMode.STANDARD);
			Logger.debug("Opened device");

			double temp = bmp180.getTemperature();
			Logger.info("Temperature={}", String.format("%.2f", Double.valueOf(temp)));
			double pressure = bmp180.getPressure();
			Logger.info("Pressure={}", String.format("%.2f", Double.valueOf(pressure)));
		} catch (IOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		}
	}
}
