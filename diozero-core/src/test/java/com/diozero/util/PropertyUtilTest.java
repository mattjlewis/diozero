package com.diozero.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("static-method")
public class PropertyUtilTest {
	@Before
	public void setup() {
		System.getProperties().put("test1", "test1-prop");
		System.getProperties().put("test3", "test3-prop");
	}
	
	@Test
	public void testStringProperty() {
		String key = "test1";
		String default_val = "test1-notset";
		Assert.assertEquals("test1-prop", PropertyUtil.getProperty(key, default_val));

		key = "test2";
		default_val = "test2-notset";
		Assert.assertEquals("test2-env", PropertyUtil.getProperty(key, default_val));

		key = "test3";
		default_val = "test3-notset";
		Assert.assertEquals("test3-prop", PropertyUtil.getProperty(key, default_val));
	}
}
