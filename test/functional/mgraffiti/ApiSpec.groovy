package mgraffiti

import spock.lang.*
import grails.plugin.spock.IntegrationSpec
import static groovyx.net.http.ContentType.*
import org.bson.types.ObjectId
import com.mongodb.BasicDBObject

@Stepwise
class ApiSpec extends BaseApiSpec {

	@Shared
	def testWalls = [
		[ title: "fin", 		lat: 60.10,	lon: 25.25,	nfcId: null,		creator: "spock" ],
		[ title: "bolivia", 	lat: -16.9, lon: -64.5,	nfcId: "testNfc1",	creator: "spock" ],
		[ title: "with nfc",	lat: null,	lon: null,	nfcId: "testNfc2",	creator: "spock" ],
		[ title: "4th", 		lat: 61.12, lon: 26.99,	nfcId: null,		creator: "spock" ]
	]

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

	def "Get walls by nfcId"() {
		when: "Loading a (single) wall by its NFC tag"
		def resp = client.get(path: "rest/wall", query: [nfcId: nfcWall.nfcId])

		then: "We get the correct wall"
		resp.status == 200
		resp.contentType == JSON.toString()
		wallsFoundInResponse([nfcWall], [resp.data])

		where:
		nfcWall << testWalls.findAll { it.nfcId }
	}

	def "Get walls by location"() {
		given: "Two walls close to each other"
		def nearbyWalls = [testWalls[0], testWalls[3]]
		def notNearbyWalls = [testWalls[1]]
		
		when: "Loading walls near an area where two walls were created"
		def resp = client.get(path: "rest/wall", query: [lat: 60.5, lon: 26, radius: 200])
		
		then: "The correct walls are included in response"
		resp.status == 200
		resp.contentType == JSON.toString()
		wallsFoundInResponse(nearbyWalls, resp.data)
		
		and: "The incorrect walls aren't included in response"
		wallsNotFoundInResponse(notNearbyWalls, resp.data)
		
	}

	/*
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

	private def wallsNotFoundInResponse(def myWalls, def theirWalls) {
		wallsFoundInResponse(myWalls, theirWalls, true)
	}
	
	/**
	* Verify that all (local) walls in myWalls are found in theirWalls (remote, via api)
	* @param myWalls walls from the testWalls list, added to database previously
	* @param theirWalls the JSON response from API
	* @param if true, reverse functionality, e.g.if any in myWalls is found in theirWalls, return false 
	*/
	private def wallsFoundInResponse(def myWalls, def theirWalls, def reverse = false) {
		def i = 0
		myWalls.every { testWall ->
			println "looking for ${testWall} in ${theirWalls}"
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
				return reverse ? true : false
			} else {
				return reverse ? false : true
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
