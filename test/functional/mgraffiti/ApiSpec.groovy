package mgraffiti

import spock.lang.*
import grails.plugin.spock.IntegrationSpec

@Stepwise
class ApiSpec extends BaseApiSpec {

	def "List walls"() {
		when: "getting a list of all walls"
		def response = client.get(path: "rest/wall/")
		def data = response.data
		
		then: "there are some walls"
		println "got ${data}"
		response.status == 200
		data?.size() > 0
	}
	
}
