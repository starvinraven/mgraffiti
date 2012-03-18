modules = {
    application {
        resource url:'js/application.js'
    }
	wallList {
		dependsOn "jquery, oviMaps"
		resource url:'js/wallList.js'	
	}
	oviMaps {
		resource url:'http://api.maps.ovi.com/jsl.js'
	}
}