<#--
 #%L
 Device I/O Zero - Web application
 %%
 Copyright (C) 2016 - 2017 mattjlewis
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
	<title>GPIO Control</title>
</head>
<body>
	<header>
	</header>
	<nav>
		<a href="/gpio">GPIO Control</a>
	</nav>
	<main>
		<h1>GPIO Control</h1>
		<#nested />
		
<#if boardInfo??>
		<h2>Board Info</h2>
		<ul>
			<li>Make: ${boardInfo.make}</li>
			<li>Model: ${boardInfo.model}</li>
			<li>Memory: ${boardInfo.memory}</li>
			<li>Library Path: ${boardInfo.libraryPath}</li>
			<li>ADC vRef: ${boardInfo.adcVRef}</li>
		</ul>
		<h3>GPIOs</h3>
		<table>
			<thead>
				<tr><th>Name</th><th>Header</th><th>Pin #</th><th>Device Id #</th><th>Control</th><th></th></tr>
			</thead>
			<tbody>
		<#list boardInfo.headerPins as header>
			<#list header?values as pin>
				<tr>
					<td>${pin.name}</td>
					<td>${pin.header}</td>
					<td>${pin.pinNumber}</td>
					<td><#if pin.deviceNumber == -1>N/A<#else><#if pin.pwmOutputSupported>PWM #<#elseif pin.analogInputSupported>ADC #<#elseif pin.analogOutputSupported>DAC #<#elseif pin.digitalOutputSupported>GPIO #${pin.deviceNumber}<#else>??? #</#if></#if></td>
					<td>
				<#if pin.modes?seq_contains("PWM_OUTPUT")>
						<form method="get" action="/pwm/${pin.pwmNum}"><input id="pwm${pin.pwmNum}" name="val" type="range" min="0" max="1" step="0.01" onchange="document.getElementById('pwmVal${pin.pwmNum}').value = document.getElementById('pwm${pin.pwmNum}').value"/><input id="pwmVal${pin.pwmNum}" type="text" size="3" readonly="readonly"/><input type="submit" value="Set"/></form>
				<#elseif pin.modes?seq_contains("DIGITAL_OUTPUT")>
						<a href="/gpio/on/${pin.deviceNumber}">On</a> / 
						<a href="/gpio/off/${pin.deviceNumber}">Off</a> / 
						<a href="/gpio/toggle/${pin.deviceNumber}">Toggle</a>
				</#if>
					</td>
				</tr>
			</#list>
		</#list>
			</tbody>
		</table>
<#else>
		<h2>No Board Info</h2>
</#if>
	</main>
	<footer>GPIO Control &mdash; A Spark Application</footer>
</body>
</html>
</#macro>
