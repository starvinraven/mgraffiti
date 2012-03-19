package mgraffiti

import grails.converters.JSON

class MainController {
	
	def mgraffitiDB

	/**
	 * List the walls by popularity - used by main index page ajax calls
	 * 
	 * @param perPage walls per page
	 * @param page the page number (0-indexed)
	 */
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

	/**
	 * Blank main page action
	 */
	def index() {
	}
	
	/**	
	 * Load paged list of walls using the native mongo driver
	 */
	private def getWallsNative(page, perPage) {
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
		
}
