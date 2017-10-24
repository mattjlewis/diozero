package com.diozero.remote.message;

public abstract class SpiBase extends Request {
	private static final long serialVersionUID = 3335504681921809532L;

	private int controller;
	private int chipSelect;
	
	public SpiBase(int controller, int chipSelect, String correlationId) {
		super(correlationId);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
	}
	
	public int getController() {
		return controller;
	}
	
	public int getChipSelect() {
		return chipSelect;
	}
}
