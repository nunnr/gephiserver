<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
	<head>
		<%@ include file="/WEB-INF/jsp/head.jsp" %>
		<title>GephiServer</title>
	</head>
	<body>
		<header>
			<h1>Gephi Server</h1>
			<form method="get" action="">
				<label>
					Select graph:
					<select name="graphs"></select>
				</label>
				<label>
					Async
					<input name="async" type="checkbox" value="1">
				</label>
				<button type="submit">Draw</button>
			</form>
		</header>
		<div id="graph"></div>
		<footer>
			<p>GephiServer - 2017</p>
		</footer>
	</body>
</html>