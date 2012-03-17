package mgraffiti

import org.joda.time.DateTime
import org.joda.time.Duration

class PopularityJob {

	def concurrent = false
	
	static triggers = {
		simple name: 'popularityTrigger', startDelay: 60000, repeatInterval: 30000
	}

	def execute() {
		Wall.withNewSession() {
			generateWallPopularity()
		}
	}
	
	def generateWallPopularity() {
		log.info("Generating wall popularity...")
		def count = 0
		def startTime = System.currentTimeMillis()
		Wall.list().each { Wall wall ->
			def now = new DateTime()
			def popularity = 0
			wall.layers.each { WallLayer layer ->
				long ageHours = new Duration(new DateTime(layer.dateCreated), now).getMillis()/(1000*60*60)
				popularity += calculatePopularityForAge(ageHours)
			}
			wall.popularity = popularity
			wall.save(flush: true)
			count++
		}
		log.info("Generated wall popularity for ${count} walls in ${System.currentTimeMillis() - startTime} ms")
	}
	
	/**
	 * Calculates popularity score for a given age.
	 *
	 * Formula: score = 1000 - 1000 * ageHours/(24*7)
	 *
	 * (zero hours -> 1000 points, linearly down to 24*7 hours -> 0 points)
	 *
	 * @param ageMillis
	 * @return
	 */
	def calculatePopularityForAge(long ageHours) {
		Math.min(1000.0, Math.max(1000 - (1000 * ageHours/(24*7.0)), 0.0))
	}

}
