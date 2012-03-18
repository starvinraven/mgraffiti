<%@ page import="mgraffiti.Wall" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>mGraffiti</title>
		<%--
		<script type="text/javascript" src="http://api.maps.ovi.com/jsl.js"></script>
		 --%>
		<r:require modules="wallList"/>
	</head>
	<body>
		<div id='wallTemplate' class='wall' style="display:none;">
			<h2>title</h2>
			<p>Created by </p>
			<p>Last updated </p>
			<p>Layers: </p>
			<p class="pop">Popularity: </p>
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
