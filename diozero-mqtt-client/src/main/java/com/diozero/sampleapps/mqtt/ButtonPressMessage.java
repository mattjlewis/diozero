package com.diozero.sampleapps.mqtt;

public class ButtonPressMessage {
	private String hostname;
	private int gpio;
	private long epochTime;
	private boolean value;

	public ButtonPressMessage() {
	}

	public ButtonPressMessage(String hostname, int gpio, long epochTime, boolean value) {
		this.hostname = hostname;
		this.gpio = gpio;
		this.epochTime = epochTime;
		this.value = value;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getGpio() {
		return gpio;
	}

	public void setGpio(int gpio) {
		this.gpio = gpio;
	}

	public long getEpochTime() {
		return epochTime;
	}

	public void setEpochTime(long epochTime) {
		this.epochTime = epochTime;
	}

	public boolean isValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}
}
