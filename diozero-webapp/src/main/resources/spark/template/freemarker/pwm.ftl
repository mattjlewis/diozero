<#--
 #%L
 Organisation: diozero
 Project:      Device I/O Zero - Web application
 Filename:     pwm.ftl  
 
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
<#import "masterTemplate.ftl" as layout />

<@layout.masterTemplate title="PWM">
<#if output??>
	<#assign pin = boardInfo.getByPwmNumber(output.pwmNum)>
<h2>PWM #${pin.pwmNum} (${pin.name})</h2>
<p>Value: ${output.value}</p>
<p><form method="get" action="/pwm/${pin.pwmNum}"><input id="pwm" name="val" type="range" min="0" max="1" step="0.01" value="${output.value}" onchange="document.getElementById('pwmVal').value = document.getElementById('pwm').value"/><input id="pwmVal" type="text" size="3" readonly="readonly"/><input type="submit" value="Set"/></form></p>
<p>Supported modes:</p>
<ul>
	<#list pin.modes as mode><li>${mode}</li></#list>
</ul>
<#else>
<h2>No PWM</h2>
</#if>

</@layout.masterTemplate>
