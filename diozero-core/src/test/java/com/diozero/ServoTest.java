package com.diozero;

import org.junit.Assert;
import org.junit.Test;

import com.diozero.util.ServoUtil;

public class ServoTest {
	private static final double TOWERPRO_SG5010_MIN_MS = 1;
	private static final double TOWERPRO_SG5010_MAX_MS = 2;
	private static final double TOWERPRO_SG90_MIN_MS = 0.5;
	private static final double TOWERPRO_SG90_MAX_MS = 2.4;
	
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		int bits = 12;
		int range = (int)Math.pow(2, bits);
		double ms_per_bit = ServoUtil.calcPulseMsPerBit(60, range);
		
		double on = TOWERPRO_SG5010_MIN_MS;
		int servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG5010 Min: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 246);
		
		on = TOWERPRO_SG5010_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG5010 Max: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 492);
		
		on = TOWERPRO_SG90_MIN_MS;
		servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG90 Min: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 123);
		
		on = TOWERPRO_SG90_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 590);
	}
}
