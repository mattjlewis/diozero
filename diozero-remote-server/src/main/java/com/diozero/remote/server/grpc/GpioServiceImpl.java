/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Server
 * Filename:     GpioControlImpl.java
 *
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

package com.diozero.remote.server.grpc;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AnalogDeviceInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDigitalDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalDeviceInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.message.protobuf.BooleanResponse;
import com.diozero.remote.message.protobuf.FloatResponse;
import com.diozero.remote.message.protobuf.Gpio;
import com.diozero.remote.message.protobuf.GpioServiceGrpc;
import com.diozero.remote.message.protobuf.IntegerResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.sbc.DeviceFactoryHelper;

import io.grpc.stub.StreamObserver;

public class GpioServiceImpl extends GpioServiceGrpc.GpioServiceImplBase {
	private NativeDeviceFactoryInterface deviceFactory;
	private Map<Integer, BlockingQueue<DigitalInputEvent>> subscriberQueues;

	public GpioServiceImpl() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public GpioServiceImpl(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
		subscriberQueues = new ConcurrentHashMap<>();
	}

	@Override
	public void provisionDigitalInputDevice(Gpio.ProvisionDigitalInputDeviceRequest request,
			StreamObserver<Response> responseObserver) {
		Logger.debug("Provision GPIO digital input request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		try {
			if (device == null) {
				deviceFactory.provisionDigitalInputDevice(pin_info, DiozeroProtosConverter.convert(request.getPud()),
						DiozeroProtosConverter.convert(request.getTrigger()));

				response_builder.setStatus(Status.OK);
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(DeviceMode.DIGITAL_INPUT);

				response_builder.setStatus(Status.OK);
			} else if (device instanceof GpioDigitalInputDeviceInterface) {
				// TODO Update the pud / trigger?
				response_builder.setStatus(Status.OK);
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void provisionDigitalOutputDevice(Gpio.ProvisionDigitalOutputDeviceRequest request,
			StreamObserver<Response> responseObserver) {
		Logger.debug("Provision GPIO digital output request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		try {
			if (device == null) {
				deviceFactory.provisionDigitalOutputDevice(pin_info, request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(DeviceMode.DIGITAL_OUTPUT);
				inout.setValue(request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else if (device instanceof GpioDigitalOutputDeviceInterface) {
				((GpioDigitalOutputDeviceInterface) device).setValue(request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void provisionDigitalInputOutputDevice(Gpio.ProvisionDigitalInputOutputDeviceRequest request,
			StreamObserver<Response> responseObserver) {
		Logger.debug("Provision GPIO digital input output request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		try {
			if (device == null) {
				deviceFactory.provisionDigitalInputOutputDevice(pin_info,
						request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);

				response_builder.setStatus(Status.OK);
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);

				response_builder.setStatus(Status.OK);
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void provisionDigitalPwmDevice(Gpio.ProvisionPwmOutputDeviceRequest request,
			StreamObserver<Response> responseObserver) {
		Logger.debug("Provision PWM output request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		try {
			if (device == null) {
				deviceFactory.provisionPwmOutputDevice(pin_info, request.getFrequency(), request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else if (device instanceof PwmOutputDeviceInterface) {
				((PwmOutputDeviceInterface) device).setValue(request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void provisionAnalogInputDevice(Gpio.ProvisionAnalogInputDeviceRequest request,
			StreamObserver<Response> responseObserver) {
		Logger.debug("Provision Analog input request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		try {
			if (device == null) {
				deviceFactory.provisionAnalogInputDevice(pin_info);

				response_builder.setStatus(Status.OK);
			} else if (device instanceof AnalogInputDeviceInterface) {
				response_builder.setStatus(Status.OK);
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void provisionAnalogOutputDevice(Gpio.ProvisionAnalogOutputDeviceRequest request,
			StreamObserver<Response> responseObserver) {
		Logger.debug("Provision Analog output request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		try {
			if (device == null) {
				deviceFactory.provisionAnalogOutputDevice(pin_info, request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else if (device instanceof AnalogOutputDeviceInterface) {
				((AnalogOutputDeviceInterface) device).setValue(request.getInitialValue());

				response_builder.setStatus(Status.OK);
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void digitalRead(Gpio.Identifier request, StreamObserver<BooleanResponse> responseObserver) {
		Logger.debug("GPIO digital read request");

		BooleanResponse.Builder response_builder = BooleanResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof GpioDigitalDeviceInterface) {
				try {
					response_builder.setData(((GpioDigitalDeviceInterface) device).getValue());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void digitalWrite(Gpio.BooleanMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO digital write request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof GpioDigitalOutputDeviceInterface) {
				try {
					((GpioDigitalOutputDeviceInterface) device).setValue(request.getValue());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void pwmRead(Gpio.Identifier request, StreamObserver<FloatResponse> responseObserver) {
		Logger.debug("GPIO PWM read request");

		FloatResponse.Builder response_builder = FloatResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof PwmOutputDeviceInterface) {
				try {
					response_builder.setData(((PwmOutputDeviceInterface) device).getValue());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void pwmWrite(Gpio.FloatMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO PWM write request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof PwmOutputDeviceInterface) {
				try {
					((PwmOutputDeviceInterface) device).setValue(request.getValue());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getPwmFrequency(Gpio.Identifier request, StreamObserver<IntegerResponse> responseObserver) {
		Logger.debug("GPIO get PWM frequency request");

		IntegerResponse.Builder response_builder = IntegerResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof PwmOutputDeviceInterface) {
				try {
					response_builder.setData(((PwmOutputDeviceInterface) device).getPwmFrequency());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void setPwmFrequency(Gpio.IntegerMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO set PWM frequency request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof PwmOutputDeviceInterface) {
				try {
					((PwmOutputDeviceInterface) device).setPwmFrequency(request.getFrequency());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void analogRead(Gpio.Identifier request, StreamObserver<FloatResponse> responseObserver) {
		Logger.debug("GPIO analog read request");

		FloatResponse.Builder response_builder = FloatResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof AnalogDeviceInterface) {
				try {
					response_builder.setData(((AnalogDeviceInterface) device).getValue());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void analogWrite(Gpio.FloatMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO analog write request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof AnalogOutputDeviceInterface) {
				try {
					((AnalogOutputDeviceInterface) device).setValue(request.getValue());
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void setOutput(Gpio.BooleanMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO set output request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				try {
					((GpioDigitalInputOutputDeviceInterface) device)
							.setMode(request.getValue() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);
					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid mode, device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void subscribe(Gpio.Identifier request, StreamObserver<Gpio.Event> responseObserver) {
		Logger.debug("GPIO subscribe request {}", Integer.valueOf(request.getGpio()));

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			Gpio.Event.Builder response_builder = Gpio.Event.newBuilder().setGpio(request.getGpio());

			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");

			responseObserver.onNext(response_builder.build());
			responseObserver.onCompleted();

			return;
		}

		BlockingQueue<DigitalInputEvent> queue = subscriberQueues.get(Integer.valueOf(request.getGpio()));
		// Is there already a subscriber?
		if (queue != null) {
			Logger.warn("Already a subscriber for gpio {}", Integer.valueOf(request.getGpio()));

			Gpio.Event.Builder response_builder = Gpio.Event.newBuilder().setGpio(request.getGpio());

			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Subscriber already present");

			responseObserver.onNext(response_builder.build());
			responseObserver.onCompleted();

			return;
		}

		try {
			if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				queue = new LinkedBlockingQueue<>();
				subscriberQueues.put(Integer.valueOf(request.getGpio()), queue);

				((GpioDigitalInputOutputDeviceInterface) device).setListener(queue::offer);

				while (true) {
					DigitalInputEvent event = queue.take();
					if (event.getGpio() == -1) {
						break;
					}

					responseObserver
							.onNext(Gpio.Event.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
									.setNanoTime(event.getNanoTime()).setValue(event.getValue()).build());
				}
			} else if (device instanceof GpioDigitalInputDeviceInterface) {
				queue = new LinkedBlockingQueue<>();
				subscriberQueues.put(Integer.valueOf(request.getGpio()), queue);

				((GpioDigitalInputDeviceInterface) device).setListener(queue::offer);

				while (true) {
					DigitalInputEvent event = queue.take();
					if (event.getGpio() == -1) {
						break;
					}

					responseObserver
							.onNext(Gpio.Event.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
									.setNanoTime(event.getNanoTime()).setValue(event.getValue()).build());
				}
			} else {
				Logger.warn("Device class {} for GPIO {} does not support event listeners", device.getClass().getName(),
						Integer.valueOf(request.getGpio()));

				Gpio.Event.Builder response_builder = Gpio.Event.newBuilder().setGpio(request.getGpio());

				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO does not support listeners");

				responseObserver.onNext(response_builder.build());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);

			Gpio.Event.Builder response_builder = Gpio.Event.newBuilder().setGpio(request.getGpio());

			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);

			responseObserver.onNext(response_builder.build());
		} catch (InterruptedException e) {
			Logger.error(e, "Error: {}", e);

			Gpio.Event.Builder response_builder = Gpio.Event.newBuilder().setGpio(request.getGpio());

			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Interrupted: " + e);

			responseObserver.onNext(response_builder.build());
		} finally {
			// Clean-up
			if (queue != null) {
				if (device instanceof GpioDigitalInputOutputDeviceInterface) {
					((GpioDigitalInputOutputDeviceInterface) device).removeListener();
				} else if (device instanceof GpioDigitalInputDeviceInterface) {
					((GpioDigitalInputDeviceInterface) device).removeListener();
				}

				subscriberQueues.remove(Integer.valueOf(request.getGpio()));
			}
		}

		responseObserver.onCompleted();
	}

	@Override
	public void unsubscribe(Gpio.Identifier request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO unsubscribe request {}", Integer.valueOf(request.getGpio()));

		responseObserver.onNext(unsubscribe(request.getGpio()));
		responseObserver.onCompleted();
	}

	private Response unsubscribe(int gpio) {
		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio);
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			Logger.warn("No subscriber for GPIO {}", Integer.valueOf(gpio));
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			BlockingQueue<DigitalInputEvent> queue = subscriberQueues.get(Integer.valueOf(gpio));
			if (queue == null) {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("No GPIO subscription found");
			} else {
				queue.offer(new DigitalInputEvent(-1, -1, -1, false));

				response_builder.setStatus(Status.OK);
			}
		}

		return response_builder.build();
	}

	@Override
	public void close(Gpio.Identifier request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO close request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof GpioDeviceInterface) {
				try {
					unsubscribe(request.getGpio());

					device.close();

					response_builder.setStatus(Status.OK);
				} catch (RuntimeIOException e) {
					Logger.error(e, "Error: {}", e);
					response_builder.setStatus(Status.ERROR);
					response_builder.setDetail("Runtime Error: " + e);
				}
			} else {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Invalid device class: " + device.getClass().getName());
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}
}
