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
	$.getJSON("rest/graph/list", function(data) {
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
			var height = getWindowHeight();
			height -= $('header').outerHeight(true);
			height -= graph.css('padding-top').replace('px', '');
			height -= graph.css('padding-bottom').replace('px', '');
			height -= $('footer').outerHeight(true);
			$(svg).attr('width', '').attr('height', '').css({
				'width': '100%',
				'height': height + 'px'
			});
			svgPanZoom(svg, {
				controlIconsEnabled: true
			});
		}
	};
	var svgPanZoomDestroy = function() {
		var svg = getSvg();
		if (svg) {
			svgPanZoom(svg).destroy();
		}
	};
	var pollAsyncResult = function(asyncUuid, timeout) {
		setTimeout(function() {
			$.ajax({
				url: 'rest/graph/getSvgAsyncResult'
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
		svgPanZoomDestroy();
		var id = $(this).find('select[name="graphs"] option:selected').val();
		var async = $(this).find('input[name="async"]').prop('checked');
		if (async) {
			graph.html('<div class="spin">A</div>');
			$.ajax({
				url: 'rest/graph/stdSvgAsync'
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
			graph.load('rest/graph/stdSvg', {graphId: id}, function(response, status, xhr){
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