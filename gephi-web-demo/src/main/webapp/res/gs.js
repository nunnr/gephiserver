$(window).on('load', function() {
	var getWindowWidth = "innerWidth" in window ?
			function() { return window.innerWidth; }
			: function() { return document.documentElement.offsetWidth; };
	var getWindowHeight = "innerHeight" in window ?
			function() { return window.innerHeight; }
			: function() { return document.documentElement.offsetHeight; };
	$.getJSON("/gephi-server/rest/graph/list", function(data) {
		var $select = $('select[name="graphs"]');
		$.each(data, function(key, val) {
			$select.append($('<option>', {value:key, text:val}));
		});
	});
	var $graph = $('#graph');
	var getSvg = function() {
		return $graph.find('svg').get(0);
	};
	var graphHeight = function() {
		var height = getWindowHeight();
		height -= $('header').outerHeight(true);
		height -= $graph.css('padding-top').replace('px', '');
		height -= $graph.css('padding-bottom').replace('px', '');
		height -= $('footer').outerHeight(true);
		$graph.css({
			'height': height + 'px'
		});
	};
	var svgPanZoomCreate = function(svg) {
		if (svg) {
			$graph.finish();
			graphHeight();
			$graph.fadeIn(function() {
				svgPanZoom(svg, {
					controlIconsEnabled: true
				});
			});
		}
	};
	var svgPanZoomDestroy = function(svg) {
		if (svg) {
			$graph.finish();
			svgPanZoom(svg).destroy();
			$graph.fadeOut();
		}
	};
	var pollAsyncResult = function(asyncUuid, timeout) {
		setTimeout(function() {
			$.ajax({
				url: '/gephi-server/rest/graph/getSvgAsyncResult'
				, method: 'POST'
				, data: {uuid: asyncUuid}
				, headers: {Accept: 'application/svg+xml'}
			})
			.done(function(data, textStatus, jqXHR) {
				if (data) {
					var svg = $(data).children('svg').get(0);
					$(svg).removeAttr('width').removeAttr('height');
					$graph.empty().append(svg);
					svgPanZoomCreate(svg);
				}
				else if (timeout < 64000) {
					pollAsyncResult(asyncUuid, timeout * 2);
				}
				else {
					$graph.text('Timed out');
				}
			})
			.fail(function(jqXHR, textStatus, errorThrown) {
				$graph.text('Failed');
			});
		}, timeout);
	};
	$('form').on('submit', function() {
		svgPanZoomDestroy(getSvg());
		var id = $(this).find('select[name="graphs"] option:selected').val();
		var url = '/gephi-server/rest/graph/';
		var async = $(this).find('input[name="async"]').prop('checked');
		if (async) {
			url += 'stdSvgAsync';
			var fncDone = function(data, textStatus, jqXHR) {
				pollAsyncResult(data, 1000);
			};
		}
		else {
			url += 'stdSvg';
			var fncDone = function(data, textStatus, jqXHR) {
				if (data) {
					var svg = $(data).children('svg').get(0);
					$(svg).removeAttr('width').removeAttr('height');
					$graph.empty().append(svg);
					svgPanZoomCreate(svg);
				}
				else {
					$graph.text('Failed');
				}
			};
		}
		$.ajax({
			url: url
			, method: 'POST'
			, data: {graphId: id}
		})
		.done(fncDone)
		.fail(function(jqXHR, textStatus, errorThrown) {
			$graph.text('Failed');
		});
		return false;
	});
	$(window).on('resize', function() {
		graphHeight();
		var panZoom = getSvg();
		if (panZoom) {
			panZoom.resize();
			panZoom.fit();
			panZoom.center();
		}
	});
});