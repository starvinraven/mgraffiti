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
					item.find('p:eq(0)').text("Created by "+wall.creatorName+" at "+wall.dateCreated);
					item.find('p:eq(1)').text("Last updated "+wall.lastUpdated);
					item.find('p:eq(2)').text("Layers: "+wall.image.numLayers);
					item.find('p:eq(3)').text("Popularity: "+ Math.round(wall.popularity));
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
				//console.log("paging");
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
		