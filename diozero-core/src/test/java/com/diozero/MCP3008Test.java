package com.diozero;

/**
 * MCP3008 test using the test device factory
 */
public class MCP3008Test extends McpAdcTest {
	@Override
	protected McpAdc.Type getType() {
		return McpAdc.MCP3008;
	}
}
