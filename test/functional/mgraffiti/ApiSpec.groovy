package mgraffiti

import spock.lang.*
import grails.plugin.spock.IntegrationSpec

@Stepwise
class ApiSpec extends BaseApiSpec {

	def "Create a bunch of walls"() {
		
	}
	
	def "List all walls, see that created ones are included"() {
		
	}
	
	def "Get wall by nfcId"() {
		
	}
	
	def "Get wall by location"() {
		
	}
	
	def "Add layer to wall"() {
		
	}
	
	def "Check that layer is available"() {
		
	}

	/*	
	def "List walls"() {
		when: "getting a list of all walls"
		def response = client.get(path: "rest/wall/")
		def data = response.data
		
		then: "there are some walls"
		println "got ${data}"
		response.status == 200
		data?.size() > 0
	}
	*/
	
}
