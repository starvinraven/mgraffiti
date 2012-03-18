package mgraffiti

import grails.converters.JSON

class RestImageController extends RestBaseController {

	// TODO: smarter cache headers
	
	def imageService
	
	/**
	 * Send web (.jpg) flattened image by id
	 */
	def outputWebImage() {
		cache false
		def wall = getOr404(Wall, params.id)
		response.contentType = 'image/jpeg'
		imageService.getFlattenedImage(wall, ImageTypes.JPG, response.outputStream)
	}
	
	/**
	 * Send mobile client (.png) flattened image by id
	 */
	def outputImage() {
		cache false
		def wall = getOr404(Wall, params.id)
		ByteArrayOutputStream baos = new ByteArrayOutputStream() // <- probably required due to WEIRD grails bug?
		imageService.getFlattenedImage(wall, ImageTypes.PNG, baos)
		def bytes = baos.toByteArray()
		response.contentType = "image/png"
		response.contentLength = bytes.length
		response.outputStream << bytes
	}
	
	/**
	 * Store wall layer image sent by client (must be .png)
	 */
	def putImage() {
		Wall wall = getOr404(Wall, params.id)
		def img = request.inputStream.bytes
		
		log.info "bytes uploaded: "+img?.length
		WallLayer wallLayer = new WallLayer()
		if(!imageService.addLayer(wall, wallLayer, img)) {
			log.warn "error in validation: "+wallLayer.errors
			sendError("Invalid image", 400)
		} else {
			log.info "successfully saved layer: ${wallLayer} to wall ${wall} (${wall.layers})"
			render wall.toMap() as JSON
		}
	}
}
