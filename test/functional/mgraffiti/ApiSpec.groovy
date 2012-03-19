package mgraffiti

import spock.lang.*
import grails.plugin.spock.IntegrationSpec
import static groovyx.net.http.ContentType.*
import org.bson.types.ObjectId
import com.mongodb.BasicDBObject
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.apache.commons.codec.digest.DigestUtils


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
	def invalidWalls = [
		[ title: null, 			lat: 60.10,	lon: 25.25,	nfcId: null,		creator: "spock" ],
		[ title: "i suck", 		lat: null,	lon: null,	nfcId: null,		creator: "spock" ],
	]

	@Shared
	def createdWallData = []

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
		createdWallData << [id: wallId, url: resp.data.image.url, jpgUrl: resp.data.image.jpgUrl]
		wallId ==~ /\w+/
		println "created wall with id ${wallId}"

		cleanup: "Add created IDs to wall list"
		createdWallData.eachWithIndex { d, i ->
			testWalls[i] += d
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

	def "Try to create invalid walls"() {
		when: "Creating a wall with invalid params"
		def body = createPostBody(postParams)
		def resp = client.post(
				path: "rest/wall",
				body: body
				)

		then: "Request is unsuccessful, and we get a 400 response with some description"
		resp.status == 400
		resp.contentType == JSON.toString()

		where: "Invalid wall data"
		postParams << invalidWalls
	}

	def "Walls without layers are the same as the background images"() {
		given: "The default wall backgrounds"
		File backgroundPngFile = loadResourceFile("resources/image.png")
		File backgroundJpgFile = loadResourceFile("resources/webImage.jpeg")

		when: "When loading a created wall by its ID"
		def pngResp = client.get(uri: createdWall.url, contentType: BINARY)
		def jpgResp = client.get(uri: createdWall.jpgUrl, contentType: BINARY)

		then: "The flattened images match the background"
		[pngResp, jpgResp]*.status == [200, 200]
		pngResp.contentType == "image/png"
		jpgResp.contentType == "image/jpeg"
		compareFiles(pngResp.data, backgroundPngFile)
		compareFiles(jpgResp.data, backgroundJpgFile)

		where: "The created walls are used"
		createdWall << testWalls
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
		testWalls.each { wall ->
			deleteWall(wall.id)
		}
	}

	private compareFiles(InputStream is, File file) {
		def fis = new FileInputStream(file)
		def hashA = DigestUtils.md5Hex(is)
		def hashB = DigestUtils.md5Hex(fis)
		fis.close()
		println "hashes: ${hashA} vs ${hashB}"
		hashA == hashB
	}

	private File loadResourceFile(def fileName) {
		Resource resource = new ClassPathResource(fileName)
		resource.getFile()
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
			def apiWall = theirWalls.find { it.id == testWall.id }
			def matches = [
				jsonEquals(apiWall?.title, testWall.title),
				jsonEquals(apiWall?.location?.lat, testWall.lat),
				jsonEquals(apiWall?.location?.lon, testWall.lon),
				jsonEquals(apiWall?.nfcId, testWall.nfcId),
				jsonEquals(apiWall?.creatorName, testWall.creator)
			]
			if(!matches.every { it }) {
				// println "walls not equal: ${apiWall} vs ${testWall}"
				// println "matches: ${matches}"
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
}
