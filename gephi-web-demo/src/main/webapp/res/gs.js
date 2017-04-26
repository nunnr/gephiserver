$(window).on('load', function() {
	if ("innerWidth" in window) {
		var getWindowWidth = function() {
			return window.innerWidth;
		};
	}
	else {
		var getWindowWidth = function() {
			return document.documentElement.offsetWidth;
		};
	}
	if ("innerHeight" in window) {
		var getWindowHeight = function() {
			return window.innerHeight;
		};
	}
	else {
		var getWindowHeight = function() {
			return document.documentElement.offsetHeight;
		};
	}
	$.getJSON("/gephi-server/rest/graph/list", function(data) {
		var $select = $('select[name="graphs"]');
		$.each(data, function(key, val) {
			$select.append($('<option>', {value:key, text:val}));
		});
	});
	var graph = $('#graph');
	var getSvg = function() {
		return graph.find('svg').get(0);
	};
	var svgPanZoomCreate = function(svg) {
		if (svg) {
			graph.finish();
			var height = getWindowHeight();
			height -= $('header').outerHeight(true);
			height -= graph.css('padding-top').replace('px', '');
			height -= graph.css('padding-bottom').replace('px', '');
			height -= $('footer').outerHeight(true);
			$(svg).removeAttr('width').removeAttr('height').css({
				'width': '100%',
				'height': height + 'px'
			});
			graph.fadeIn(function(){
				svgPanZoom(svg, {
					controlIconsEnabled: true
				});
			});
		}
	};
	var svgPanZoomDestroy = function(svg) {
		if (svg) {
			graph.finish();
			svgPanZoom(svg).destroy();
			graph.fadeOut();
		}
	};
	var pollAsyncResult = function(asyncUuid, timeout) {
		setTimeout(function() {
			$.ajax({
				url: '/gephi-server/rest/graph/getSvgAsyncResult'
				, method: 'POST'
				, data: {uuid: asyncUuid}
			})
			.done(function(data, textStatus, jqXHR) {
				if (data) {
					var svg = $(data).children('svg').get(0);
					graph.empty().append(svg);
					svgPanZoomCreate(svg);
				}
				else if (timeout < 64000) {
					pollAsyncResult(asyncUuid, timeout * 2);
				}
				else {
					graph.text('Timed out');
				}
			})
			.fail(function(jqXHR, textStatus, errorThrown) {
				graph.text('Failed');
			});
		}, timeout);
	};
	$('form').on('submit', function() {
		svgPanZoomDestroy(getSvg());
		var id = $(this).find('select[name="graphs"] option:selected').val();
		var async = $(this).find('input[name="async"]').prop('checked');
		if (async) {
			$.ajax({
				url: '/gephi-server/rest/graph/stdSvgAsync'
				, method: 'POST'
				, data: {graphId: id}
			})
			.done(function(data, textStatus, jqXHR) {
				pollAsyncResult(data, 1000);
			})
			.fail(function(jqXHR, textStatus, errorThrown) {
				graph.text('Failed');
			});
		}
		else {
			graph.load('/gephi-server/rest/graph/stdSvg', {graphId: id}, function(response, status, xhr){
				if (status != 'error') {
					svgPanZoomCreate(getSvg());
				}
				else {
					graph.text('Failed');
				}
			});
		}
		return false;
	});
});