package com.diozero.api;

import java.io.Closeable;
import java.io.IOException;

public interface MotorInterface extends Closeable {
	void forward(float speed) throws IOException;
	void backward(float speed) throws IOException;
	void stop() throws IOException;
	float getValue() throws IOException;
	void setValue(float value) throws IOException;
	boolean isActive() throws IOException;
	void reverse() throws IOException;
}
