package com.diozero.internal.provider.firmata.ds18b20;

public class DS18B20Config {
	private byte alarmHighTrigger;
	private byte alarmLowTrigger;
	private DS18B20Resolution resolution;

	public DS18B20Config(byte alarmHighTrigger, byte alarmLowTrigger, DS18B20Resolution resolution) {
		this.resolution = resolution;
		this.alarmHighTrigger = alarmHighTrigger;
		this.alarmLowTrigger = alarmLowTrigger;
	}

	public byte getAlarmHighTrigger() {
		return alarmHighTrigger;
	}

	void setAlarmHighTrigger(byte alarmHighTrigger) {
		this.alarmHighTrigger = alarmHighTrigger;
	}

	public byte getAlarmLowTrigger() {
		return alarmLowTrigger;
	}

	void setAlarmLowTrigger(byte alarmLowTrigger) {
		this.alarmLowTrigger = alarmLowTrigger;
	}

	public DS18B20Resolution getResolution() {
		return resolution;
	}

	void setResolution(DS18B20Resolution resolution) {
		this.resolution = resolution;
	}
}
