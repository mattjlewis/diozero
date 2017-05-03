<#import "masterTemplate.ftl" as layout />

<@layout.masterTemplate title="GPIO">
	<#if output??>
<h2>GPIO ${output.gpio}</h2>
<p>On? ${output.isOn()?c}</p>
<p>Turn <#if output.isOn()><a href="/gpio/off/${output.gpio}">off</a><#else><a href="/gpio/on/${output.gpio}">on</a></#if></p>
<p><a href="/gpio/toggle/${output.gpio}">Toggle</a></p>
	<#else>
<h2>No GPIO</h2>
	</#if>

</@layout.masterTemplate>
