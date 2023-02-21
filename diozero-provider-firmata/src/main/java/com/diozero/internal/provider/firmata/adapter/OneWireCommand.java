package com.diozero.internal.provider.firmata.adapter;

public enum OneWireCommand {
	RESET(0), SKIP(1), SELECT(2), READ(3), DELAY(4), WRITE(5);

	private byte mask;

	OneWireCommand(int bit) {
		this.mask = (byte) (1 << bit);
	}

	public byte mask() {
		return mask;
	}
}
