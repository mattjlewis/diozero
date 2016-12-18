package com.diozero.internal.provider.sysfs;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class ChipSysFsPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private int gpio;
	private int pwmChip;
	private int pwmNum;
	private Path pwmRoot;
	private RandomAccessFile dutyFile;
	private int periodNs;
	
	public ChipSysFsPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int gpio,
			int frequency, float initialValue) {
		super(key, deviceFactory);
		
		this.gpio = gpio;
		pwmNum = gpio;
		pwmChip = 0;
		Path pwm_chip_root = FileSystems.getDefault().getPath("/sys/class/pwm/pwmchip" + pwmChip);
		pwmRoot = pwm_chip_root.resolve("pwm" + pwmNum);
		
		periodNs = 1_000_000_000 / frequency;
		
		try {
			if (! pwmRoot.toFile().exists()) {
				File export_file = pwm_chip_root.resolve("export").toFile();
				try (FileWriter writer = new FileWriter(export_file)) {
					writer.write(String.valueOf(pwmNum));
				}
			}
			//setPolarity(Polarity.NORMAL);
			setEnabled(true);
			setPeriod(periodNs);
			
			dutyFile = new RandomAccessFile(pwmRoot.resolve("duty_cycle").toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening PWM #" + pwmNum, e);
		}
		setValue(0);
	}

	@Override
	public void closeDevice() {
		try { setEnabled(false); } catch (Exception e) { }
		try { dutyFile.close(); } catch (Exception e) { }
		Path pwm_chip_root = FileSystems.getDefault().getPath("/sys/class/pwm/pwmchip" + pwmChip);
		try (FileWriter writer = new FileWriter(pwm_chip_root.resolve("unexport").toFile())) {
			writer.write(String.valueOf(pwmNum));
		} catch (Exception e) {
			Logger.warn(e, "Error closing pwm #" + pwmNum);
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getPwmNum() {
		return pwmNum;
	}
	
	@Override
	public float getValue() throws RuntimeIOException {
		isOpen();
		try {
			dutyFile.seek(0);
			return Integer.parseInt(dutyFile.readLine()) / (float) periodNs;
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error getting duty for PWM " + gpio, e);
		}
	}
	
	@Override
	public void setValue(float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("Invalid value, must be 0..1");
		}
		
		try {
			//dutyFile.seek(0);
			dutyFile.writeBytes(Integer.toString((int) (value * periodNs)));
			//dutyFile.flush();
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error setting duty for PWM #" + pwmNum, e);
		}
	}
	
	private void setEnabled(boolean enabled) throws IOException {
		Logger.info("setEnabled(" + enabled + ")");
		try (FileWriter writer = new FileWriter(pwmRoot.resolve("enable").toFile())) {
			writer.write(enabled ? "1" : "0");
		}
	}
	
	private void setPeriod(int periodNs) throws IOException {
		Logger.info("setPeriod(" + periodNs + ")");
		try (FileWriter period_file = new FileWriter(pwmRoot.resolve("period").toFile())) {
			period_file.write(Integer.toString(periodNs));
		}
		this.periodNs = periodNs;
	}
	
	protected void setPolarity(Polarity polarity) throws IOException {
		Logger.info("setPolarity(" + polarity + ")");
		try (FileWriter writer = new FileWriter(pwmRoot.resolve("polarity").toFile())) {
			writer.write(polarity.getValue());
			writer.flush();
		}
	}
	
	public static enum Polarity {
		NORMAL("normal"), INVERSED("inversed");
		
		private String value;
		private Polarity(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
}
