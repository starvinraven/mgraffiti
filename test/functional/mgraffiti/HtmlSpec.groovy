package mgraffiti

import geb.spock.GebReportingSpec
import spock.lang.*

class HtmlSpec extends GebReportingSpec {
	def "there is an index page and walls are listed in descending popularity order"() {
		given:
		to IndexPage
		expect:
		at IndexPage
		and: "there are walls"
		walls.size() > 0
		and: "walls are sorted by popularity in descending order"
		wallsOrderedByDescPopularity(popularities)
	}
	
	private def wallsOrderedByDescPopularity(popularities) {
		// walk through popularities, see that the order is descending
		for(i in (1 ..< popularities?.size())) {
			def popPrevious = popularities[i-1]
			def popThis = popularities[i]  
			if(popPrevious < popThis) {
				println "popularity ${i-1}: ${popPrevious}, this ${popThis}" 
				return false 
			}
		}
		true
	}
		
}
