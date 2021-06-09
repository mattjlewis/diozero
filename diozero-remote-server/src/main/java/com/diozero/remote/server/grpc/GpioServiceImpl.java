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
import com.diozero.remote.message.protobuf.Gpio;
import com.diozero.remote.message.protobuf.GpioServiceGrpc;
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
	public void digitalRead(Gpio.DigitalReadRequest request,
			StreamObserver<Gpio.DigitalReadResponse> responseObserver) {
		Logger.debug("GPIO digital read request");

		Gpio.DigitalReadResponse.Builder response_builder = Gpio.DigitalReadResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof GpioDigitalDeviceInterface) {
				try {
					response_builder.setValue(((GpioDigitalDeviceInterface) device).getValue());
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
	public void digitalWrite(Gpio.DigitalWriteRequest request, StreamObserver<Response> responseObserver) {
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
	public void pwmRead(Gpio.PwmReadRequest request, StreamObserver<Gpio.PwmReadResponse> responseObserver) {
		Logger.debug("GPIO PWM read request");

		Gpio.PwmReadResponse.Builder response_builder = Gpio.PwmReadResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof PwmOutputDeviceInterface) {
				try {
					response_builder.setValue(((PwmOutputDeviceInterface) device).getValue());
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
	public void pwmWrite(Gpio.PwmWriteRequest request, StreamObserver<Response> responseObserver) {
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
	public void getPwmFrequency(Gpio.GetPwmFrequencyRequest request,
			StreamObserver<Gpio.GetPwmFrequencyResponse> responseObserver) {
		Logger.debug("GPIO get PWM frequency request");

		Gpio.GetPwmFrequencyResponse.Builder response_builder = Gpio.GetPwmFrequencyResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof PwmOutputDeviceInterface) {
				try {
					response_builder.setFrequency(((PwmOutputDeviceInterface) device).getPwmFrequency());
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
	public void setPwmFrequency(Gpio.SetPwmFrequencyRequest request, StreamObserver<Response> responseObserver) {
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
	public void analogRead(Gpio.AnalogReadRequest request, StreamObserver<Gpio.AnalogReadResponse> responseObserver) {
		Logger.debug("GPIO analog read request");

		Gpio.AnalogReadResponse.Builder response_builder = Gpio.AnalogReadResponse.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			if (device instanceof AnalogDeviceInterface) {
				try {
					response_builder.setValue(((AnalogDeviceInterface) device).getValue());
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
	public void analogWrite(Gpio.AnalogWriteRequest request, StreamObserver<Response> responseObserver) {
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
	public void setOutput(Gpio.SetOutputRequest request, StreamObserver<Response> responseObserver) {
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
							.setMode(request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);
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
	public void subscribe(Gpio.SubscribeRequest request, StreamObserver<Gpio.Notification> responseObserver) {
		Logger.debug("GPIO subscribe request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			Gpio.Notification.Builder response_builder = Gpio.Notification.newBuilder().setGpio(request.getGpio());

			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");

			responseObserver.onNext(response_builder.build());
			responseObserver.onCompleted();

			return;
		}

		BlockingQueue<DigitalInputEvent> queue = subscriberQueues.get(Integer.valueOf(request.getGpio()));
		// Is there already a subscriber?
		if (queue != null) {
			Gpio.Notification.Builder response_builder = Gpio.Notification.newBuilder().setGpio(request.getGpio());

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

					responseObserver.onNext(
							Gpio.Notification.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
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

					responseObserver.onNext(
							Gpio.Notification.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
									.setNanoTime(event.getNanoTime()).setValue(event.getValue()).build());
				}
			} else {
				Gpio.Notification.Builder response_builder = Gpio.Notification.newBuilder().setGpio(request.getGpio());

				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("GPIO does not support listeners");

				responseObserver.onNext(response_builder.build());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);

			Gpio.Notification.Builder response_builder = Gpio.Notification.newBuilder().setGpio(request.getGpio());

			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Runtime Error: " + e);

			responseObserver.onNext(response_builder.build());
		} catch (InterruptedException e) {
			Logger.error(e, "Error: {}", e);

			Gpio.Notification.Builder response_builder = Gpio.Notification.newBuilder().setGpio(request.getGpio());

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
	public void unsubscribe(Gpio.SubscribeRequest request, StreamObserver<Response> responseObserver) {
		Logger.debug("GPIO unsubscribe request");

		Response.Builder response_builder = Response.newBuilder();

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("GPIO not provisioned");
		} else {
			BlockingQueue<DigitalInputEvent> queue = subscriberQueues.get(Integer.valueOf(request.getGpio()));
			if (queue == null) {
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("No GPIO subscription found");
			} else {
				queue.offer(new DigitalInputEvent(-1, -1, -1, false));

				response_builder.setStatus(Status.OK);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void close(Gpio.CloseRequest request, StreamObserver<Response> responseObserver) {
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
					// TODO Locate the event queue for this device and notify it to stop events

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
