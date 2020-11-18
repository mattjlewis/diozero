package com.diozero.imu.mqtt;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU Visualiser
 * Filename:     MqttListener.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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


import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.diozero.devices.imu.MqttConstants;
import com.interactivemesh.jfx.importer.tds.TdsModelImporter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MqttListener extends Application implements MqttCallback, MqttConstants {
	private static final int QUAT_SCALER = 0;
	private static final int QUAT_X = 1;
	private static final int QUAT_Y = 2;
	private static final int QUAT_Z = 3;

	private static String mqttServer;
	
	private MqttClient mqttClient;
	private Shape3D testObject;
	private AnchorPane root = new AnchorPane();

	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].startsWith("--" + MQTT_SERVER_OPTION + "=")) {
				mqttServer = args[0].substring(MQTT_SERVER_OPTION.length() + 3);
			}
		}
		
		if (mqttServer == null) {
			System.out.println("Error: Usage MqttListener --" + MQTT_SERVER_OPTION + "=<MQTT Server URL>");
			System.exit(2);
		}
		
		Application.launch(args);
	}
	
	public MqttListener() {
		try {
			mqttClient = new MqttClient(mqttServer, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.setCallback(this);
			MqttConnectOptions con_opts = new MqttConnectOptions();
			con_opts.setCleanSession(true);
			mqttClient.connect(con_opts);
			System.out.println("Connected to '" + mqttServer + "'");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable t) {
		System.out.println("connectionLost(" + t + ")");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		System.out.println("deliveryComplete(" + token.getMessageId() + ", " + Arrays.toString(token.getTopics()) + ")");
	}

	@SuppressWarnings("boxing")
	@Override
	public void messageArrived(String topic, MqttMessage message) {
		System.out.println("messageArrived(" + topic + ")");
		ByteBuffer buffer = ByteBuffer.wrap(message.getPayload());
		double[] compass = new double[] { buffer.getDouble(), buffer.getDouble(), buffer.getDouble() };
		double[] accel = new double[] { buffer.getDouble(), buffer.getDouble(), buffer.getDouble() };
		double[] gyro = new double[] { buffer.getDouble(), buffer.getDouble(), buffer.getDouble() };
		double[] quat = new double[] { buffer.getDouble(), buffer.getDouble(), buffer.getDouble(), buffer.getDouble() };
		double[] ypr = new double[] { buffer.getDouble(), buffer.getDouble(), buffer.getDouble() };
		double w = quat[QUAT_SCALER];
		double x = quat[QUAT_X];
		double y = quat[QUAT_Y];
		double z = quat[QUAT_Z];
		
		System.out.format("Got IMU data: compass=[%f, %f, %f], accel=[%f, %f, %f], "
				+ "gyro=[%f, %f, %f], quat=[%f, %f, %f, %f], ypr=[%f, %f, %f]%n",
				compass[0], compass[1], compass[2], accel[0], accel[1], accel[2],
				gyro[0], gyro[1], gyro[2], quat[0], quat[1], quat[2], quat[3], ypr[0], ypr[1], ypr[2]);
		
		//Rotate rx = new Rotate(Math.toDegrees(ypr[0]), Rotate.X_AXIS);
		//Rotate ry = new Rotate(Math.toDegrees(ypr[1]), Rotate.Y_AXIS);
		//Rotate rz = new Rotate(Math.toDegrees(ypr[2]), Rotate.Z_AXIS);
		
		double[] idt = { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 };
		Affine matrix = new Affine(idt, MatrixType.MT_3D_3x4, 0);
		// http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q54
		matrix.setMxx(1 - (2*y*y + 2*z*z));	matrix.setMxy(2*x*y + 2*z*w);		matrix.setMxz(2*x*z - 2*y*w);
		matrix.setMyx(2*x*y - 2*z*w);		matrix.setMyy(1 - (2*x*x + 2*z*z));	matrix.setMyz(2*y*z + 2*x*w);
		matrix.setMzx(2*x*z + 2*y*w);		matrix.setMzy(2*y*z - 2*x*w);		matrix.setMzz(1 - (2*x*x + 2*y*y));
		
		if (! Platform.isFxApplicationThread()) {
			Platform.runLater(() -> {
					testObject.getTransforms().setAll(matrix);
					//testObject.getTransforms().clear();
					//testObject.getTransforms().addAll(rx, ry, rz);
				}
			);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setResizable(false);
		Scene scene = new Scene(root, 1024, 800, true);
		
		// Create and position camera
		Camera camera = new PerspectiveCamera();
		camera.getTransforms().addAll(
				new Rotate(0, Rotate.Y_AXIS),
				new Rotate(0, Rotate.X_AXIS),
				new Translate(-500, -425, 1200));
		scene.setCamera(camera);
		scene.setFill(Paint.valueOf(Color.BLACK.toString()));
		
		// Box
		testObject = new Cylinder(10, 50);
		testObject.setMaterial(new PhongMaterial(Color.RED));
		testObject.getTransforms().addAll(new Translate(50, 0, 0));
		
		TdsModelImporter model_importer = new TdsModelImporter();
		model_importer.read(getClass().getResource("/models/SpaceLaunchSystem.3DS"));
		Node[] nodes = model_importer.getImport();
		model_importer.close();
		Group rocket = new Group(nodes);
		rocket.getTransforms().addAll(new Translate(0, 25, 0));
 
		// Build the Scene Graph
		root.getChildren().addAll(testObject, rocket);

		primaryStage.setScene(scene);
		primaryStage.show();
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				System.out.println(event);
				if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
					System.exit(0);
				}
			}
		});
		
		mqttClient.subscribe(MQTT_TOPIC_IMU + "/#");
	}
}
