package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.Button;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.SPIConstants;
import com.diozero.internal.provider.mcp23xxx.MCP23S17;
import com.diozero.util.SleepUtil;

public class MCP23S17Test {
	public static void main(String[] args) {
		try (MCP23S17 expander = new MCP23S17(SPIConstants.DEFAULT_SPI_CONTROLLER, SPIConstants.CE1);
				Button button = new Button(expander, 4, GpioPullUpDown.NONE)) {
			Logger.info("Using {}", expander.getName());
			button.whenPressed(() -> Logger.info("Pressed"));
			button.whenReleased(() -> Logger.info("Released"));
			for (int i=0; i<10; i++) {
				Logger.info(button.getValue());
				Logger.info(expander.getValues(0));
				Logger.info(expander.getValues(1));
				Logger.info("Sleeping for 1sec");
				SleepUtil.sleepSeconds(1);
			}
			SleepUtil.sleepSeconds(10);
		} catch (Throwable t) {
			Logger.error(t, "Error: " + t);
		}
	}
}
