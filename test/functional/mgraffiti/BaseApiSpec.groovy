package mgraffiti


import groovyx.net.http.RESTClient
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import groovyx.net.http.URIBuilder
//import grails.plugin.spock.IntegrationSpec
import spock.lang.*
import grails.plugin.spock.IntegrationSpec
import grails.plugin.spock.*

abstract class BaseApiSpec extends IntegrationSpec {

	static RESTClient client

	def setupSpec() {
		def serverUrl = getServerUrl()
		System.out.println("Server URL set to [ ${serverUrl} ]")

		client = new RESTClient(serverUrl+"/", ContentType.JSON)
		assert client != null
		client.handler.failure = { it } // Overrides the default handler, which would throw exception in case of failure
	}

	/**
	 * Get the local serverURL and configured port for REST client. Grails doesn't seem to
	 * be capable of altering the serverURL in case the server.port is given as an environment
	 * variable (e.g. "grails test-app c-Dserver.port=1234 :spock")
	 */
	private def getServerUrl() {
		def serverPort = (System.getProperty("server.port") ?: 8080) as int
		def ub = new URIBuilder(ConfigurationHolder.config.grails.serverURL)
		ub.port = serverPort
		ub.toString()
	}
}
