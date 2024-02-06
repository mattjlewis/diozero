package com.diozero.sampleapps.oled;

import com.diozero.devices.oled.SH1106;
import com.diozero.devices.oled.SsdOledCommunicationChannel;

/**
 * {@code java -cp diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.oled.SH1106Test}
 */
public class SH1106Test extends MonochromeSsdOledTest {
    public static void main(String[] args) {
        SsdOledCommunicationChannel channel = getChannel(args);
        try (SH1106 display = new SH1106(channel)) {
            sierpinski(display);
            customImage(display);
            animateText(display, "SH1106");
            display.clear();
        }
    }
}
