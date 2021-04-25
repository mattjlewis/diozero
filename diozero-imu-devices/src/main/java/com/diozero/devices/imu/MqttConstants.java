package com.diozero.devices.imu;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     MqttConstants.java
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


public interface MqttConstants {
	static final String MQTT_SERVER_OPTION = "mqttServer";
	static final String MQTT_TOPIC_IMU = "IMU/MPU9150";
	static final String MQTT_TOPIC_COMPASS = MQTT_TOPIC_IMU + "/" + "Compass";	
	static final String MQTT_TOPIC_ACCEL = MQTT_TOPIC_IMU + "/" + "Accel";	
	static final String MQTT_TOPIC_GYRO = MQTT_TOPIC_IMU + "/" + "Gyro";	
	static final String MQTT_TOPIC_QUAT = MQTT_TOPIC_IMU + "/" + "Quat";	
	static final String MQTT_TOPIC_EULER_ANGLE = MQTT_TOPIC_IMU + "/" + "EulerAngle";
	
	/*
	 * Quality of Service 0 - indicates that a message should be delivered at most once (zero or one times).
	 * The message will not be persisted to disk, and will not be acknowledged across the network. This QoS
	 * is the fastest, but should only be used for messages which are not valuable - note that if the server
	 * cannot process the message (for example, there is an authorization problem), then an
	 * MqttCallback.deliveryComplete(IMqttDeliveryToken). Also known as "fire and forget".
	 */
	static final int MQTT_QOS_AT_MOST_ONCE = 0;	
	/*
	 * Quality of Service 1 - indicates that a message should be delivered at least once (one or more times).
	 * The message can only be delivered safely if it can be persisted, so the application must supply a means
	 * of persistence using MqttConnectOptions. If a persistence mechanism is not specified, the message will
	 * not be delivered in the event of a client failure. The message will be acknowledged across the network.
	 * This is the default QoS.
	 */
	static final int MQTT_QOS_AT_LEAST_ONCE = 1;	
	/*
	 * Quality of Service 2 - indicates that a message should be delivered once. The message will be persisted
	 * to disk, and will be subject to a two-phase acknowledgement across the network. The message can only be
	 * delivered safely if it can be persisted, so the application must supply a means of persistence using
	 * MqttConnectOptions. If a persistence mechanism is not specified, the message will not be delivered in
	 * the event of a client failure.
	 * If persistence is not configured, QoS 1 and 2 messages will still be delivered in the event of a
	 * network or server problem as the client will hold state in memory. If the MQTT client is shutdown or
	 * fails and persistence is not configured then delivery of QoS 1 and 2 messages can not be maintained as
	 * client-side state will be lost.
	 */
	static final int MQTT_QOS_ONCE = 2;
}
