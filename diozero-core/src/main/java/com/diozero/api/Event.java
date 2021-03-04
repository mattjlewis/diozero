package com.diozero.api;

public class Event {
	private long epochTime;
	private long nanoTime;
	
	public Event(long epochTime, long nanoTime) {
		this.epochTime = epochTime;
		this.nanoTime = nanoTime;
	}

	public long getEpochTime() {
		return epochTime;
	}

	public long getNanoTime() {
		return nanoTime;
	}
}
