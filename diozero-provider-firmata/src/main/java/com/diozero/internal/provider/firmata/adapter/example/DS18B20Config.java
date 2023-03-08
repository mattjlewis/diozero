package com.diozero.internal.provider.firmata.adapter.example;

public class DS18B20Config {
	private DS18B20Resolution resolution;
	private byte alarmHigh;
	private byte alarmLow;

	public DS18B20Config(DS18B20Resolution resolution, byte alarmHigh, byte alarmLow) {
		this.resolution = resolution;
		this.alarmHigh = alarmHigh;
		this.alarmLow = alarmLow;
	}

	public DS18B20Resolution getResolution() {
		return resolution;
	}

	public byte getAlarmHigh() {
		return alarmHigh;
	}

	public byte getAlarmLow() {
		return alarmLow;
	}
}
