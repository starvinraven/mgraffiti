package mgraffiti

import grails.converters.JSON

class RestWallController extends RestBaseController {

	def fileService
	def imageService
	def wallService

	def list() {
		cache false
		def result = null
		if(params.lat && params.lon) {
			result = byLocation(
				params.lat.replace(",", "."), 
				params.lon.replace(",", "."), 
				params.radius?.replace(",", ".")
			)
		} else if(params.nfcId) {
			result = byNfc(params.nfcId)
		} else {
			result = Wall.list()?.collect { it.toMap() }
		}
		if(result != null) {
			render result as JSON
		}
	}

	List byLocation(lat, lon, radius) {
		def result = wallService.findWallsNear(lat, lon, radius)
		def list = []
		log.info "bylocation got ${result?.size()} results"
		result.each {
			def map = it.obj.toMap()
			map.location += [distanceKm: it.distance]
			list << map
		}
		list
	}

	def byNfc(id) {
		Wall wall = Wall.findByNfcId(id)
		if(!wall) {
			sendError("No wall found with nfcId '${id}'", 404)
			return null
		} else {
			return wall.toMap()
		}
	}

	def create() {
		// {"title": "test", "creatorName": "dudeman12", location: {"lat":12.132, "lon":13.456}, "nfcId": "nfcTagGoesHere"}
		def data = request.JSON
		if((!data?.location && !data?.nfcId) || !data?.title || !data.creatorName) {
			sendError("Invalid data")
			return
		} else {
			log.info "creating new wall: ${data}"
		}
		Wall wall = new Wall(title: data.title, creatorName: data.creatorName)
		if(data?.location?.lat && data?.location?.lon) {
			wall.location = [data.location.lon as Double, data.location.lat as Double]
		}
		if(data.nfcId) {
			if(Wall.findByNfcId(data.nfcId)) {
				sendError("Wall with NFC tag ${data.nfcId} exists already!")
				return
			}
			wall.nfcId = data.nfcId
		}
		log.info("saving: ${wall.dump()}")
		if(!wallService.create(wall)) {
			sendError("Error saving wall ${wall.errors}")	
		} else {
			response.status = 201
			render wall.toMap() as JSON
		}
	}
}


