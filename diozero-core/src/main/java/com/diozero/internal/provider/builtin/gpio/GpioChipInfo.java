package com.diozero.internal.provider.builtin.gpio;

public class GpioChipInfo {
	private String name;
	private String label;
	private int numLines;
	
	public GpioChipInfo(String name, String label, int numLines) {
		this.name = name;
		this.label = label;
		this.numLines = numLines;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public int getNumLines() {
		return numLines;
	}
}
