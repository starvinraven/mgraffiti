<%@ page import="mgraffiti.Wall" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>mGraffiti</title>
		<script type="text/javascript" src="http://api.maps.ovi.com/jsl.js"></script>
		<r:require modules="jquery"/>
		<r:script>
		var walls = ${wallsJson};
		
		$(function() {
		
			loadWalls = function() {
				// TODO: ajax the walls JSON and render
			};

			renderWalls = function() {
				$.each(walls, function(i, wall) {
					//console.debug(i, wall);
					var item = $("#wallTemplate").clone();
					item.find('h2').text(wall.title);
					item.find('a').attr("href", wall.image.jpgUrl);
					item.find('img').attr("src", wall.image.jpgUrl);
					item.find('p:eq(0)').text("Created by "+wall.creatorName+" at "+wall.dateCreated)
					item.find('p:eq(1)').text("Last updated "+wall.lastUpdated)
					item.find('p:eq(2)').text("Layers: "+wall.image.numLayers)
					item.show();
					item.attr("id", "wall-"+wall.id)
					// console.debug(item);
					$("#walls").append(item);
				});
			};
		
			renderMaps = function() {
				console.debug("woo");
				
				$(".mapContainer:visible").each(function(i) {
					console.debug("hoo");
					var components = [
						new ovi.mapsapi.map.component.Behavior(),
						new ovi.mapsapi.map.component.Overview(),
						new ovi.mapsapi.map.component.RightClick(),
					];
					var div = $(this)[0];
					var center = [walls[i].location.lat, walls[i].location.lon]
					var map = new ovi.mapsapi.map.Display(div, {
						components: components,
						zoomLevel: 10,
						center: center
					});
					var marker = new ovi.mapsapi.map.StandardMarker(map.center);
					map.objects.add(marker);
				});
			};
			
			renderWalls();
			renderMaps();
			
		});
		</r:script>
	</head>
	<body>
		<div id='wallTemplate' class='wall' style="display:none;">
			<h2>title</h2>
			<p>Created by </p>
			<p>Last updated </p>
			<p>Layers: </p>
			<div class='wallInner'>
				<div class='mapContainer'>
				</div>
				<div class='imageContainer'>
					<a href='url'><img width='500'/></a>
				</div>
			</div>
		</div>
		
		<div style="margin: 50px;" id="walls">

		</div>
	</body>
</html>
