package com.diozero.internal.provider.sysfs;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class BbbPwmSysFsPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private int gpio;
	private int pwmChip;
	private int pwmNum;
	private FileWriter enableFile;
	private FileWriter periodFile;
	private FileWriter dutyFile;
	private int periodNs;

	public BbbPwmSysFsPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int gpio,
			int frequency, float initialValue) {
		super(key, deviceFactory);
		
		this.gpio = gpio;
		
		periodNs = 1_000_000_000 / frequency;
		
		// FIXME How to work this out? Temporarily hardcode to GPIO 50 (EHRPWM1A, P9_14)
		String chip = "48302000";
		String address = "48302200";
		pwmNum = 0;
		
		Path chip_path = FileSystems.getDefault().getPath("/sys/devices/platform/ocp/" + chip + ".epwmss/" + address + ".pwm/pwm");
		pwmChip = -1;
		try (DirectoryStream<Path> dirs = Files.newDirectoryStream(chip_path, "pwm*")) {
			for (Path p : dirs) {
				String dir = p.getFileName().toString();
				Logger.info("Got {}" + dir);
				pwmChip = Integer.parseInt(dir.substring(dir.length()-1));
				Logger.info("Found pwmChip {}", Integer.valueOf(pwmChip));
			}
		} catch (IOException e) {
			Logger.error(e, "Error: " + e);
		}
		if (pwmChip == -1) {
			throw new RuntimeIOException("Unable to find pwmchip for chip " + chip + ", address " + address);
		}
		
		Path pwm_chip_root = FileSystems.getDefault().getPath("/sys/class/pwm/pwmchip" + pwmChip);
		// TODO Write pwmNum to pwm_chip_root.resolve("export");
		Path pwm_root = pwm_chip_root.resolve("pwm" + pwmNum);
		
		try {
			enableFile = new FileWriter(pwm_root.resolve("enable").toFile());
			periodFile = new FileWriter(pwm_root.resolve("period").toFile());
			dutyFile = new FileWriter(pwm_root.resolve("duty_cycle").toFile());
			
			setEnabled(true);
			setPeriod(periodNs);
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening duty file for pin " + gpio, e);
		}
		setValue(0);
	}

	@Override
	public void closeDevice() {
		try { setEnabled(false); } catch (Exception e) { }
		try { dutyFile.close(); } catch (Exception e) { }
		try { periodFile.close(); } catch (Exception e) { }
		try { enableFile.close(); } catch (Exception e) { }
	}

	@Override
	public int getPin() {
		return gpio;
	}
	
	@Override
	public float getValue() throws RuntimeIOException {
		/*try {
			//dutyFile.seek(0);
			return Integer.parseInt(dutyFile.readLine()) / (float) periodNs;
		} catch (IOException e) {
			closeDevice();
			throw new RuntimeIOException("Error setting duty for PWM " + gpio, e);
		}*/
		return 0;
	}
	
	@Override
	public void setValue(float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("Invalid value, must be 0..1");
		}
		
		try {
			//dutyFile.seek(0);
			dutyFile.write(Integer.toString((int) (value * periodNs)));
			dutyFile.flush();
		} catch (IOException e) {
			closeDevice();
			throw new RuntimeIOException("Error setting duty for PWM #" + gpio, e);
		}
	}
	
	private void setEnabled(boolean enabled) throws IOException {
		//enableFile.seek(0);
		enableFile.write(enabled ? "1" : "0");
		enableFile.flush();
	}
	
	private void setPeriod(int periodNs) throws IOException {
		periodFile.write(Integer.toString(periodNs));
		periodFile.flush();
	}
}
