<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
	<head>
		<%@ include file="/WEB-INF/jsp/head.jsp" %>
		<title>Unavailable</title>
	</head>
	<body>
		<h1>Requested Resource Unavailable</h1>
		<p><c:out value="${exception.getMessage()}" /></p>
	</body>
</html>