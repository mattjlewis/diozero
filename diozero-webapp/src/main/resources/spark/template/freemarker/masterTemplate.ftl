<#macro masterTemplate title="Welcome">
<!DOCTYPE html>
<html>
<head>
	<title>GPIO Control</title>
	<link rel="stylesheet" type="text/css" href="/css/style.css">
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
			<thead><tr><th>Name</th><th>Header</th><th>Pin #</th><th>GPIO #</th><th></th><th></th><th></th></tr></thead>
			<tbody>
		<#list boardInfo.pins as pin>
				<tr>
					<td>${pin.name}</td>
					<td>${pin.header}</td>
					<td>${pin.pinNumber}</td>
					<td>${pin.deviceNumber}</td>
					<td><a href="/gpio/on/${pin.deviceNumber}">on</a></td>
					<td><a href="/gpio/off/${pin.deviceNumber}">off</a></td>
					<td><a href="/gpio/toggle/${pin.deviceNumber}">toggle</a></td>
				</tr>
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
