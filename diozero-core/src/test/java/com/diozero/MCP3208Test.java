package com.diozero;

/**
 * MCP3208 test using the test device factory
 */
public class MCP3208Test extends McpAdcTest {
	@Override
	protected McpAdc.Type getType() {
		return McpAdc.MCP3208;
	}
}
