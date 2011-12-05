<%@ page import="mgraffiti.Wall" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>mGraffiti</title>
		<script type="text/javascript" src="http://api.maps.ovi.com/jsl.js"></script>
		<r:require modules="jquery"/>
		<r:script>
		var MGraffiti = {

			walls: undefined,
			pageInfo: undefined,
			
			init: function() {
				MGraffiti.loadWalls(0);
				$("#linkFirst").click(function() {
					MGraffiti.loadWalls(0);
				});
				$("#linkPrev").click(function() {
					MGraffiti.loadWalls(MGraffiti.pageInfo.currentPage - 1);
				});
				$("#linkNext").click(function() {
					MGraffiti.loadWalls(MGraffiti.pageInfo.currentPage + 1);
				});
			},
			
			loadWalls: function(page) {
				var query = page ? ("?page="+page) : ""
				$.getJSON('walls'+query, function(data) {
					MGraffiti.walls = data.walls;
					MGraffiti.pageInfo = data.pageInfo;
					MGraffiti.renderPaging();
					MGraffiti.renderWalls();
				});
			},
		
			renderWalls: function(page) {
				$("#walls").empty();
				$.each(MGraffiti.walls, function(i, wall) {
					var item = $("#wallTemplate").clone();
					item.find('h2').text(wall.title);
					item.find('a').attr("href", wall.image.jpgUrl);
					item.find('img').attr("src", wall.image.jpgUrl);
					item.find('p:eq(0)').text("Created by "+wall.creatorName+" at "+wall.dateCreated)
					item.find('p:eq(1)').text("Last updated "+wall.lastUpdated)
					item.find('p:eq(2)').text("Layers: "+wall.image.numLayers)
					item.show();
					item.attr("id", "wall-"+wall.id)
					$("#walls").append(item);
				});
				MGraffiti.renderMaps();
			},
		
			renderMaps: function() {
				$(".mapContainer:visible").each(function(i) {
					var components = [
						new ovi.mapsapi.map.component.Behavior(),
						new ovi.mapsapi.map.component.Overview(),
						new ovi.mapsapi.map.component.RightClick(),
					];
					var div = $(this)[0];
					var center = [MGraffiti.walls[i].location.lat, MGraffiti.walls[i].location.lon]
					var map = new ovi.mapsapi.map.Display(div, {
						components: components,
						zoomLevel: 10,
						center: center
					});
					var marker = new ovi.mapsapi.map.StandardMarker(map.center);
					map.objects.add(marker);
				});
			},
			
			renderPaging: function() {
				console.log("paging");
				var span = $("#pagingText");
				var pageInfo = MGraffiti.pageInfo;
				if(pageInfo.firstResult > 0) {
					$("#linkFirst").show();
					$("#linkPrev").show();				
				} else {
					$("#linkFirst").hide();
					$("#linkPrev").hide();				
				}
				if(pageInfo.hasNext) {
					$("#linkNext").show();
				} else {
					$("#linkNext").hide();
				}
				var firstResult = pageInfo.firstResult+1
				var lastResult = firstResult + MGraffiti.walls.length - 1
				$("#currentPage").text("Displaying walls "+firstResult+" to "+lastResult+" of "+pageInfo.totalCount).show();
			}
			
		}; // var MGraffiti

		$(function() {
			$(MGraffiti.init)
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
		
		<div style="margin: 20px 50px;" id="walls">

		</div>
		<div class="paging" style="margin: 0px 50px">
			<span id="pagingText">
				<a id="linkFirst">&lt;&lt; First</a>
				<a id="linkPrev">&lt; Previous</a>
				<span id="currentPage"></span>
				<a id="linkNext">Next &gt;</a>
			</span>
		</div>
	</body>
</html>
