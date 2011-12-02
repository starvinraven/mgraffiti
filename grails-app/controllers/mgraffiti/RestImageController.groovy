package mgraffiti

import grails.converters.JSON
import groovyx.gpars.GParsPool

class RestImageController extends RestBaseController {

	def imageService
	
	def outputWebImage() {
		def wall = getOr404(Wall, params.id)
		response.contentType = 'image/jpeg'
		imageService.getFlattenedImage(wall, ImageTypes.JPG, response.outputStream)
	}
	
	def outputImage() {
		def wall = getOr404(Wall, params.id)
		ByteArrayOutputStream baos = new ByteArrayOutputStream() // <- probably required due to WEIRD grails bug?
		imageService.getFlattenedImage(wall, ImageTypes.PNG, baos)
		def bytes = baos.toByteArray()
		response.contentType = "image/png"
		response.contentLength = bytes.length
		response.outputStream << bytes
	}
	
	def putImage() {
		Wall wall = getOr404(Wall, params.id)
		def img = request.inputStream.bytes
		
		log.info "bytes uploaded: "+img?.length
		WallLayer wallLayer = new WallLayer()
		// wallLayer["fromIp"] = "123.45.67.89"
		if(!imageService.addLayer(wall, wallLayer, img)) {
			log.warn "error in validation: "+wallLayer.errors
			sendError("Invalid image", 400)
		} else {
			log.info "successfully saved layer: ${wallLayer} to wall ${wall} (${wall.layers})"
			final Thread t = Thread.start {
				// TODO: should have locking for concurrency..
				imageService.createFlattenedImage(wall, ImageTypes.PNG)
				imageService.createFlattenedImage(wall, ImageTypes.JPG)
			}
			render wall.toMap() as JSON
		}
	}
}
