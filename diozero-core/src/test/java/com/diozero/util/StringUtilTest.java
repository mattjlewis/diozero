package com.diozero.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringUtilTest {

	@Test
	void testUnquote() {
		assertNull(StringUtil.unquote(null));
		assertEquals(StringUtil.unquote(""), "");
		assertEquals(StringUtil.unquote("17"), "17");
		assertEquals(StringUtil.unquote("3\"7"), "3\"7");
		assertEquals(StringUtil.unquote("\"17 (Beefy Miracle)\""), "17 (Beefy Miracle)");
	}
}
