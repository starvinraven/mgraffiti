package mgraffiti

import org.bson.types.ObjectId

class Wall {
	
	def grailsApplication

	static mapWith = "mongo"
	
    static constraints = {
		location(nullable: true)
		nfcId(nullable: true, blank: true)
    }
	
	static mapping = {
		location geoIndex:true
	}
	
	static transients = ["imageDimensions", "imageUrl", "grailsApplication", "toMap", "webImageUrl"]
	static embedded = ['layers']
	
	ObjectId id
	String title
	List<Double> location // NOTE: order is longitude, latitude (x,y)
	Date lastUpdated
	Date dateCreated
	List<WallLayer> layers
	String creatorName
	String nfcId
	Double popularity
	
	def getImageUrl() {
		return grailsApplication.config.grails.serverURL + "/rest/wall/${this.id.toString()}/image"
	}
	
	def getWebImageUrl() {
		return grailsApplication.config.grails.serverURL + "/rest/wall/${this.id.toString()}/webImage"
	}
	
	def getImageDimensions() {
		// literal for now, since backgrounds are preset
		[width: 2000, height: 800]
	}
	
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
		map.image.dimensions = this.imageDimensions
		map.image.numLayers = this.layers?.size() ?: 0
		map.lastUpdated = this.lastUpdated
		map.dateCreated = this.dateCreated
		map.popularity = this.popularity
		map
	}
}
