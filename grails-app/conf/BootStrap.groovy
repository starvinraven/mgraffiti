import groovyx.net.http.RESTClient
import groovyx.net.http.ContentType
import mgraffiti.*

class BootStrap {

	def grailsApplication
	Random random
	
	def init = { servletContext ->
		/*
		if(Wall.count() == 0) {
			random = new Random(1)
			10.times {
				createRandomWall()
			}
		}
		*/
	}
	
	def destroy = {
	}
	
	def createRandomWall() {
		// {"title": "test", "creatorName": "dudeman12", location: {"lat":12.132, "lon":13.456}, "nfcId": "nfcTagGoesHere"}
		def wallData = [
			title: "wall"+random.nextInt(1000),
			creatorName: "guyman"+random.nextInt(100),
			location: [lat: random.nextFloat() * 100 - 50, lon: random.nextFloat() * 100 -50],
			nfcId: random.nextInt(10000)
		]
		log.info("posting: ${wallData} to ${grailsApplication.config.grails.serverURL}")
		client.post(path: 'rest/wall', body: wallData, contentType: 'application/json')
	}
	
	def getClient() {
		def client = new RESTClient(grailsApplication.config.grails.serverURL + "/", ContentType.JSON)
	}
}
