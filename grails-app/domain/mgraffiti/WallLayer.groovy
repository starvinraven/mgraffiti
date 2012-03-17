package mgraffiti

import org.bson.types.ObjectId

class WallLayer {

	static mapWith = "mongo"

    static constraints = {
    }
	
	Date dateCreated
	String layerImageId

	public String toString() {
		"${layerImageId} (created ${dateCreated})"
	}
}
