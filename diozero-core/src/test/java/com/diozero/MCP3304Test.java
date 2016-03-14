package com.diozero;

/**
 * MCP3304 test using the test device factory
 */
public class MCP3304Test extends McpAdcTest {
	@Override
	protected McpAdc.Type getType() {
		return McpAdc.MCP3304;
	}
}
