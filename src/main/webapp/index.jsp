<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
				<button type="submit">Draw</button>
			</form>
		</header>
		<div id="graph">
		</div>
		<script type="text/javascript">
			$(window).on('load', function() {
				$.getJSON("rest/graph/list", function(data) {
					var $select = $('select[name="graphs"]');
					$.each(data, function(key, val) {
						$select.append($('<option>', {value:key, text:val}));
					});
				});
				$('form').on('submit', function(){
					var id = $(this).find('select[name="graphs"] option:selected').val();
					$('#graph').load('rest/graph/stdSvg', {graphId: id});
					return false;
				});
			});
		</script>
		<footer>
		</footer>
	</body>
</html>