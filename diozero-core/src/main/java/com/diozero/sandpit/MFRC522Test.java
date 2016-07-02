package com.diozero.sandpit;

import com.diozero.util.SleepUtil;

/**
 * Control an LED with a button. To run:
 * <ul>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.MFRC522Test 25}
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.MFRC522Test 25}
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.MFRC522Test 25}
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.MFRC522Test 25}
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.0.jar com.diozero.sandpit.MFRC522Test 25}
 * </ul>
 */
public class MFRC522Test {
	public static void main(String[] args) {
		try (MFRC522 mfrc522 = new MFRC522(0, 25)) {
			while (true) {
				// Scan for cards
				System.out.println("Waiting for cards");
				Response resp = mfrc522.request(MFRC522.PICC_REQIDL);
				if (resp.getStatus() == MFRC522.MI_OK) {
					System.out.print("Card detected: 0x");
					for (byte b : resp.getBackData()) {
						System.out.format("%02x", b);
					}
					System.out.println();
					
					// Get the UID of the card
					Response ac_resp = mfrc522.anticoll();
					if (ac_resp.getStatus() == MFRC522.MI_OK) {
						byte[] uid = ac_resp.getBackData();
						System.out.format("Card UID: 0x%02x%02x%02x%02x%n", uid[0], uid[1], uid[2], uid[3]);
					} else {
						System.out.println("Got bad response status from anticoll: " + resp.getStatus());
					}
				} else {
					System.out.println("Got bad response status from request: " + resp.getStatus());
				}
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
