package mgraffiti

import spock.lang.*
import grails.plugin.spock.IntegrationSpec
import static groovyx.net.http.ContentType.*
import org.bson.types.ObjectId
import com.mongodb.BasicDBObject

@Stepwise
class ApiSpec extends BaseApiSpec {
	/*
	@Shared
	def testWalls = [
		title: 	[	"fin",		"bolivia",	"with nfc",	"4th"	],
		lat:	[	60.10,		-16.9,		null,		63.12	],
		lon:	[	25.25,		-64.5,		null,		26.99	],
		nfcId:	[	null,		"testNfc1",	"testNfc2",	null	],
		creator:[	"spock",	"spock",	"spock",	"spock"	],
		ids:	[] // will be generated and added when creating walls
	]
	*/
	@Shared
	def testWalls = [
		[ title: "fin", 		lat: 60.10,	lon: 25.25,	nfcId: null,		creator: "spock" ],
		[ title: "bolivia", 	lat: -16.9, lon: -64.5,	nfcId: "testNfc1",	creator: "spock" ],
		[ title: "with nfc",	lat: null,	lon: null,	nfcId: "testNfc2",	creator: "spock" ],
		[ title: "4th", 		lat: 63.12, lon: 26.99,	nfcId: null,		creator: "spock" ]
	]
	
	@Shared
	def fileService
	
	@Shared
	def createdWallIds = [] 
	
	def "Create a bunch of walls"() {
		when: "Creating a wall"
		def body = createPostBody(postParams)
		def resp = client.post(
			path: "rest/wall",
			body: body
		)
		
		then: "Request is successful, and we get the wall object in return"
		resp.status == 201
		resp.contentType == JSON.toString()
		def wallId = resp.data.id
		createdWallIds << wallId
		wallId ==~ /\w+/
		println "created wall with id ${wallId}"

		cleanup: "Add created IDs to wall list"
		createdWallIds.eachWithIndex { id, i ->
			testWalls[i]['id'] = id
		}
		
		where: "Test wall data"
		postParams << testWalls
		
	}
	
	def "List all walls, see that created ones are included"() {
		when: "When listing all walls"
		def resp = client.get(path: "rest/wall")
		
		then: "The walls created should be found with the correct data"
		resp.status == 200
		resp.contentType == JSON.toString()
		wallsFoundInResponse(testWalls, resp.data)
	}

	/*
	def "Get wall by nfcId"() {

	}

	def "Get wall by location"() {

	}

	def "Add layer to wall"() {

	}

	def "Check that layer is available"() {

	}

	*/
	
	/**
	 * Perform cleanup - delete created walls (mongodb: no transactions, no rollback)
	 * 
	 */
	def cleanupSpec() {
		createdWallIds.each { wallId ->
			deleteWall(wallId)
		}
	}

	/**
	 * Verify that the given walls are in provided list
	 * @param myWalls the testWalls list, added to database previously
	 * @param theirWalls the JSON response from API
	 * @return
	 */
	private def wallsFoundInResponse(def myWalls, def theirWalls) {
		def i = 0
		createdWallIds.every { createdId ->
			def testWall = myWalls[i++]
			def apiWall = theirWalls.find { it.id == testWall.id }
			def matches = [
				jsonEquals(apiWall?.title, testWall.title),
				jsonEquals(apiWall?.location?.lat, testWall.lat),
				jsonEquals(apiWall?.location?.lon, testWall.lon),
				jsonEquals(apiWall?.nfcId, testWall.nfcId),
				jsonEquals(apiWall?.creatorName, testWall.creator)
			]
			if(!matches.every { it }) {
				println "walls not equal: ${apiWall} vs ${testWall}"
				println "matches: ${matches}"
				false
			} else {
				true
			}
		}
	}
	
	/**
	 * Compare a net.sf.json element with a groovy native object - handles null jsons 
	 * @return
	 */
	private def jsonEquals(json, other) {
		json == other || (json instanceof net.sf.json.JSONNull && other == null)
	}
	
	/**
	 * Removes wall and all its images
	 * @param wallId
	 * @return
	 */
	private def deleteWall(def wallId) {
		println "deleting wall with id ${wallId}"
		def mongo = mongoDb
		def flattenedGridFs = getGridFs(FileBuckets.WALL_FLATTENED.collectionName)
		def objId = new ObjectId(wallId)
		assert mongo.wall.remove(_id: objId)?.getN() == 1
		BasicDBObject query = new BasicDBObject("metadata.wallId", objId)
		println "deleting images for wall: ${query}"
		flattenedGridFs.remove(query)
	}

	private def createPostBody(postParams) {
		def body = [title: postParams.title, creatorName: postParams.creator]
		if(postParams.nfcId) {
			body.nfcId = postParams.nfcId
		}
		if(postParams.lat && postParams.lon) {
			body.location = [lat: postParams.lat, lon: postParams.lon]
		}
		body
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
