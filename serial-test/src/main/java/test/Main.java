package test;

import com.fazecast.jSerialComm.SerialPort;

public class Main {
	public static void main(String[] args) {
		for (SerialPort port : SerialPort.getCommPorts()) {
			System.out.format("Port name: %s, Descriptive port name: %s, Port description: %s%n",
					port.getSystemPortName(), port.getDescriptivePortName(), port.getPortDescription());
		}
	}
}
