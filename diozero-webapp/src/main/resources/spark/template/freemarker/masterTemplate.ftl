<#--
 #%L
 Organisation: diozero
 Project:      Device I/O Zero - Web application
 Filename:     masterTemplate.ftl  
 
 This file is part of the diozero project. More information about this project
 can be found at http://www.diozero.com/
 %%
 Copyright (C) 2016 - 2020 diozero
 %%
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 #L%
-->
<#macro masterTemplate title="Welcome">
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<link rel="stylesheet" href="/css/style.css">
	<title>${boardInfo.name} Control</title>
</head>
<body>
	<header>
		<nav>
			<a href="/home">Home</a>
		</nav>
	</header>
	<main>
		<h1>${boardInfo.name} Control</h1>
<#nested />
		
<#if boardInfo??>
		<h2>Board Info</h2>
		<ul>
			<li>Make: ${boardInfo.make}</li>
			<li>Model: ${boardInfo.model}</li>
			<li>Memory: ${boardInfo.memory} MB</li>
			<#if (boardInfo.ADCs?size > 0)><li>ADC vRef: ${boardInfo.adcVRef}</li></#if>
		</ul>
		<h2>Pinout</h2>
	<#list boardInfo.headers?keys as header>
		<h3>Header: ${header}</h3>
		<table>
			<tbody>
		<#list boardInfo.headers[header]?values as pin>
				<#if pin?is_odd_item><tr></#if>
			<#if pin.pwmOutputSupported><#assign td_class = "pwm"/><#elseif pin.digitalOutputSupported><#assign td_class = "gpio"/><#elseif pin.name == "3.3V" || pin.name == "5V"><#assign td_class = "power"/><#elseif pin.name == "GND"><#assign td_class = "ground"/><#else><#assign td_class = "other"/></#if>
					<#if pin?is_even_item><td class="pin">${pin.pinNumber}</td></#if>
					<td class="${td_class}"><#if pin.pwmOutputSupported><a href="/pwm/${pin.pwmNum}"><#elseif pin.digitalOutputSupported><a href="/gpio/${pin.deviceNumber}"></#if>${pin.name}<#if pin.deviceNumber != -1> (<#if pin.pwmOutputSupported>PWM #${pin.pwmNum} | GPIO #${pin.deviceNumber}<#elseif pin.analogInputSupported>ADC #<#elseif pin.analogOutputSupported>DAC #<#elseif pin.digitalOutputSupported>GPIO #${pin.deviceNumber}</#if>)</#if><#if pin.pwmOutputSupported || pin.digitalOutputSupported></a></#if></td>
					<td class="${td_class}">
			<#if pin.modes?seq_contains("PWM_OUTPUT")>
						<form method="get" action="/pwm/${pin.pwmNum}"><input id="pwm${pin.pwmNum}" name="val" type="range" min="0" max="1" step="0.01" onchange="document.getElementById('pwmVal${pin.pwmNum}').value = document.getElementById('pwm${pin.pwmNum}').value"/><input id="pwmVal${pin.pwmNum}" type="text" size="3" readonly="readonly"/><input type="submit" value="Set"/></form>
			<#elseif pin.modes?seq_contains("DIGITAL_OUTPUT")>
						<a href="/gpio/${pin.deviceNumber}/on">On</a> | 
						<a href="/gpio/${pin.deviceNumber}/off">Off</a> | 
						<a href="/gpio/${pin.deviceNumber}/toggle">Toggle</a>
			</#if>
					</td>
					<#if pin?is_odd_item><td class="pin">${pin.pinNumber}</td></#if>
				<#if (! pin?is_odd_item) || pin?is_last></tr><#else><td class="centre"></td></#if>
		</#list>
			</tbody>
		</table>
	</#list>
<#else>
		<h2>No Board Info</h2>
</#if>
	</main>
	<footer>${boardInfo.name} Control &mdash; A <a href="http://sparkjava.com/">Spark Framework</a> application using <a href="http://www.diozero.com/">diozero</a></footer>
</body>
</html>
</#macro>
