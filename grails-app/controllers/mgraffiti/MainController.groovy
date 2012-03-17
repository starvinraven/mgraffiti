package mgraffiti

import grails.converters.JSON

class MainController {

	def walls() {
		def page = params.page as Integer ?: 0
		def perPage = Math.min(10, params.perPage as Integer ?: 5)
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

		def firstResult = page*perPage
		
		def map = [
			walls: walls*.toMap(), 
			pageInfo: [
				results: walls.size(),
				totalCount: totalCount, 
				firstResult: page*perPage, 
				hasPrevious: first > 0, 
				hasNext: first+walls.size() < totalCount,
				currentPage: page
			]
		]
		render map as JSON
	}
	
	def index() {}
}
