class UrlMappings {

	static mappings = {
		"/rest/wall/"(controller: 'restWall') {
			action = [POST: 'create', GET: "list"]
		}
		"/rest/wall/$id/webImage"(controller: 'restImage', action: 'outputWebImage')
		"/rest/wall/$id/image"(controller: 'restImage') {
			action = [GET: 'outputImage', PUT: 'putImage', POST: 'putImage']
		}
		"/"(controller:"main", action: "index")
		"500"(view:'/error')
	}
}
