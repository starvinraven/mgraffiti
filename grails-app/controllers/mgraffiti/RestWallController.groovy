package mgraffiti

import grails.converters.JSON

class RestWallController extends RestBaseController {

	def fileService
	def imageService
	def wallService

	def list() {
		cache false
		def result = null
		
		if(params.id) {
			result = byId(params.id)
		} else if(params.lat && params.lon) {
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

	private List byLocation(lat, lon, radius) {
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
	
	private def byId(id) {
		Wall wall = Wall.get(new org.bson.types.ObjectId(id))
		if(!wall) {
			sendError("No wall found with id '${id}'", 404)
			null
		} else {
			wall.toMap()
		}
	}

	private def byNfc(id) {
		Wall wall = Wall.findByNfcId(id)
		if(!wall) {
			sendError("No wall found with nfcId '${id}'", 404)
			null
		} else {
			wall.toMap()
		}
	}

	def create() {
		// {"title": "test", "creatorName": "dudeman12", location: {"lat":12.132, "lon":13.456}, "nfcId": "nfcTagGoesHere"}
		log.info("creating wall")
		def data = request.JSON
		if((!data?.location && !data?.nfcId) || !data?.title || !data.creatorName) {
			sendError("Invalid data")
			return
		} 

		def (success, returnValue) = wallService.create(data)
		if(!success) {
			sendError(returnValue)	
		} else {
			response.status = 201
			render returnValue.toMap() as JSON
		}
	}
}


