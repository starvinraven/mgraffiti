package mgraffiti

import grails.converters.JSON

class MainController {

	def index() {
		log.info("index")
		
		def page = params.page as Integer ?: 0
		def perPage = Math.min(10, params.perPage as Integer ?: 5)
		
		def c = Wall.createCriteria()
		def walls = c.list {
			ne("location", null)
			firstResult(page*perPage)
			maxResults(perPage)
			order("lastUpdated", "desc")
		}
		// log.info("walls: "+walls.collect { it.dump() })
		
		[ wallsJson: walls*.toMap() as JSON ]
	}
}
