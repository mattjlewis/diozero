package ${package};

import org.tinylog.Logger;

import com.diozero.devices.LED;
import com.diozero.util.SleepUtil;

public class App {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <gpio>", App.class.getName());
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);
		
		try (LED led = new LED(gpio)) {
			for (int i=0; i<5; i++) {
				Logger.info("LED on");
				led.on();
				SleepUtil.sleepMillis(500);
				Logger.info("LED off");
				led.off();
				SleepUtil.sleepMillis(500);
			}
		}
	}
}
