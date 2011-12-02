package mgraffiti

import org.bson.types.ObjectId

class Wall {
	
	def grailsApplication

    static constraints = {
		location(nullable: true)
		/*latitude(nullable: true, blank: true)
		longitude(nullable: true, blank: true)*/
		//creatorName(nullable: true, blank: true)
		nfcId(nullable: true, blank: true)
    }
	
	static mapping = {
		location geoIndex:true
	}
	
	static transients = ["imageUrl", "grailsApplication", "toMap", "webImageUrl"]
	static embedded = ['layers']
	
	ObjectId id
	String title
	List<Double> location // NOTE: order is latitude, longitude (x,y)
	Date lastUpdated
	Date dateCreated
	List<WallLayer> layers
	String creatorName
	String nfcId
	
	def getImageUrl() {
		return grailsApplication.config.grails.serverURL + "/rest/wall/${this.id.toString()}/image"
	}
	
	def getWebImageUrl() {
		return grailsApplication.config.grails.serverURL + "/rest/wall/${this.id.toString()}/webImage"
	}
	
	/*
	def setLatitude(def lat) {
		if(!location) {
			location = [lat, null]
		} else {
			location[1] = lat
		}
	}
	
	def setLongitude(def lon) {
		if(!location) {
			location = [null, lon]
		} else {
			location[0] = lon
		}
	}
	
	def getLatitude() {
		if(!location || location.size != 2) {
			null
		} else {
			location[1]
		}
	}
	
	def getLongitude() {
		if(!location || location.size != 2) {
			null
		} else {
			location[0]
		}
	}
	*/
	
	def toMap() {
		def map = [:]
		map.id = this.id.toString()
		map.creatorName = this.creatorName
		map.title = this.title
		map.nfcId = this.nfcId
		if(this.location && this.location.size() > 1) {
			map.location = [lat:this.location[1], lon: this.location[0]]
		}
		map.image = [:]
		map.image.url = this.imageUrl
		map.image.jpgUrl = this.webImageUrl
		// map.image.dimensions = this.imageDimensions
		map.image.numLayers = this.layers?.size() ?: 0
		map.lastUpdated = this.lastUpdated
		map.dateCreated = this.dateCreated
		map
	}
}
