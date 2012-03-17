package mgraffiti

import grails.converters.JSON

class MainController {
	
	def mgraffitiDB

	def walls() {
		def page = params.page as Integer ?: 0
		def perPage = Math.min(20, params.perPage as Integer ?: 10)
		def first = page*perPage
		def (walls, totalCount) = getWallsNative(page, perPage)

		def map = [
			walls: walls*.toMap(), 
			pageInfo: [
				results: walls.size(),
				totalCount: totalCount, 
				firstResult: first, 
				hasPrevious: first > 0, 
				hasNext: first+walls.size() < totalCount,
				currentPage: page
			]
		]
		render map as JSON
	}
	
	def getWallsNative(page, perPage) {
		def first = page*perPage
		def mongoWalls = mgraffitiDB.wall
			.find(location: ['$ne':null])
			.limit(perPage)
			.skip(first)
			.sort(popularity:-1, lastUpdated:-1)
		def walls = mongoWalls.collect {
			Wall.get(it._id)
		}
		def totalCount = mgraffitiDB.wall.count(location: ['$ne':null])
		[walls, totalCount]
	}
	
	/**
	 * Criteria queries broken!
	 * 
	 * @deprecated
	 * @param page
	 * @param perPage
	 * @return
	 */
	def getWallsWithCriteria(page, perPage) {
		def first = page*perPage
		
		def c = Wall.createCriteria()
		def walls = c.list {
			ne("location", null)
			firstResult(first)
			maxResults(perPage)
			//order("lastUpdated", "desc")
			order("popularity", "desc")
		}

		// Wall.countByLocationNotNull.. doesn't work with mongo
		c = Wall.createCriteria()
		def totalCount = c.count {
			ne("location", null)
		}
		[walls, totalCount]
	}
	
	def index() {}
}
