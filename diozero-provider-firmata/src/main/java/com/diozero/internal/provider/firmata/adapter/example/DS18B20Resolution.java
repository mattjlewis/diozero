package com.diozero.internal.provider.firmata.adapter.example;

public enum DS18B20Resolution {
	// Actually 93.75ms, 187.5ms, 375ms, 750ms
	_9BIT(0b00, 94), _10BIT(0b01, 188), _11BIT(0b10, 375), _12BIT(0b11, 750);

	private byte value;
	private int temperatureConversionTime;

	private DS18B20Resolution(int value, int temperatureConversionTime) {
		this.value = (byte) value;
		this.temperatureConversionTime = temperatureConversionTime;
	}

	public byte value() {
		return value;
	}

	public int temperatureConversionTime() {
		return temperatureConversionTime;
	}

	public byte toConfigReg() {
		return (byte) (value << 5 | 0x1f);
	}

	public static DS18B20Resolution fromConfigReg(byte configRegValue) {
		switch ((configRegValue >> 5) & 0b11) {
		case 0b00:
			return _9BIT;
		case 0b01:
			return _10BIT;
		case 0b10:
			return _11BIT;
		case 0b11:
		default:
			return _12BIT;
		}
	}
}
