package mgraffiti

import javax.imageio.ImageIO
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import com.mongodb.gridfs.GridFSDBFile
import java.awt.image.BufferedImage
import java.awt.Graphics
import java.io.InputStream;

class ImageService {

	static transactional = false
	def fileService

	private static final DEFAULT_WALL_BACKGROUND_IMAGE_NAME = "wallbackground.png"
	private static final DEFAULT_BLANK_BACKGROUND_IMAGE_NAME = "blankbackground.png"

	/**
	 * Write a wall's flattened image (creating it, if necessary) to outputStream.
	 * 
	 * @param wall
	 * @param type
	 * @param outputStream
	 */
	void getFlattenedImage(Wall wall, ImageTypes type, OutputStream outputStream) {
		def fileName = getFileName(wall, type)
		log.info("get flattened ${fileName}")
		def file = fileService.findFileByName(FileBuckets.WALL_FLATTENED, fileName)
		if(!file) {
			log.info("file does not exist, creating")
			createFlattenedImage(wall, type)
			file = fileService.findFileByName(FileBuckets.WALL_FLATTENED, fileName)
			if(!file) {
				log.error("could not create file!")
				return
			}
		} else {
			log.info("using existing file")
		}
		def bytes = file.writeTo(outputStream)
		log.info("outputted image (${type}) for wall ${wall} - wrote ${bytes} bytes")
	}

	/**
	 * Create flattened wall image (using all layers) and write to database. Overwrites existing flattened image if exists.
	 * 
	 * Note: some transactionality would be nice.
	 * 
	 * @param wall
	 * @param type
	 * @return
	 */
	void createFlattenedImage(Wall wall, ImageTypes type) {
		def images = []
		if(type.insertDefaultBackground) {
			images << getDefaultBackgroundImage()
		} else {
			images << getBlankBackgroundImage()
		}
		images += getLayersForWall(wall)
		def fileName = getFileName(wall, type)
		def existingFlattenedImageId = fileService.findFileByName(FileBuckets.WALL_FLATTENED, fileName)?.id
		def baos = new ByteArrayOutputStream()
		combineImages(images, baos, type)
		if(existingFlattenedImageId) {
			log.info("deleting existing flattened image for wall ${wall} with type ${type} and fileName ${fileName}")
			fileService.delete(FileBuckets.WALL_FLATTENED, existingFlattenedImageId)
		}
		fileService.saveImage(FileBuckets.WALL_FLATTENED, baos.toByteArray(), fileName, [wallId: wall.id])
	}

	/**
	 * Combine a bunch of image files
	 * 
	 * @param imageFiles A list of image files (MongoDB GridFSDBFile instances)
	 * @param outputStream Where to write the resulting image
	 * @param type The ImageType to output
	 * @return
	 */
	def combineImages(def imageFiles, OutputStream outputStream, ImageTypes type) {
		log.info ("Combining images with lengths: ${imageFiles.collect{it?.length}} for type ${type}")
		def startTime = System.currentTimeMillis()

		//BufferedImage combined = new BufferedImage(imageFiles[0].metadata.dimensions.width, imageFiles[0].metadata.dimensions.height, outputType == "PNG" ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB)
		BufferedImage combined = new BufferedImage(ImageTypes.DEFAULT_SIZE[0], ImageTypes.DEFAULT_SIZE[1], type.format == "PNG" ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB)
		Graphics g = combined.getGraphics()
		imageFiles.each {
			if(it?.inputStream) {
				g.drawImage(ImageIO.read(it.inputStream), 0, 0, null)
			} else {
				log.warn("error: invalid image file: ${it}")
			}
		}
		ImageIO.write(combined, type.format, outputStream)
		log.info "Combined ${imageFiles?.size()?:0} images in ${System.currentTimeMillis()-startTime} ms, type ${type}"
	}

	String getFileName(Wall wall, ImageTypes type) {
		wall.id.toString() + "." + (type.format.toLowerCase())
	}

	/**
	 * Get the default background image (currently a tile wall .PNG)
	 * @return The image file
	 */
	GridFSDBFile getDefaultBackgroundImage() {
		def getImage = { fileService.findFileByName(FileBuckets.DEFAULT_IMAGES, DEFAULT_WALL_BACKGROUND_IMAGE_NAME) }
		def image = getImage()
		if(!image) {
			loadDefaultWallBackgroundImage()
			image = getImage()
		}
		image
	}

	/**
	 * Get the blank background image (currently a blank transparent .PNG)
	 * @return
	 */
	GridFSDBFile getBlankBackgroundImage() {
		def getImage = { fileService.findFileByName(FileBuckets.DEFAULT_IMAGES, DEFAULT_BLANK_BACKGROUND_IMAGE_NAME) }
		def image = getImage()
		if(!image) {
			loadDefaultBlankBackgroundImage()
			image = getImage()
		}
		image
	}

	/**
	 * Import default background image to database from filesystem
	 * 
	 */
	def loadDefaultWallBackgroundImage() {
		// when starting, assure we have the background images in database - if not, add
		if(!fileService.findFileByName(FileBuckets.DEFAULT_IMAGES, DEFAULT_WALL_BACKGROUND_IMAGE_NAME)) {
			// wall image (brick background)
			log.warn("No default background wall image in DB - adding")
			Resource resource = new ClassPathResource("resources/"+DEFAULT_WALL_BACKGROUND_IMAGE_NAME)
			File file = resource.getFile()
			assert file.exists()
			fileService.saveImage(FileBuckets.DEFAULT_IMAGES, file.bytes, DEFAULT_WALL_BACKGROUND_IMAGE_NAME)
		}
	}

	/**
	 * Import default blank background image to database from filesystem	
	 *
	 */
	def loadDefaultBlankBackgroundImage() {
		if(!fileService.findFileByName(FileBuckets.DEFAULT_IMAGES, DEFAULT_BLANK_BACKGROUND_IMAGE_NAME)) {
			// transparent bg image
			log.warn("No default blank background image in DB - adding")
			Resource resource = new ClassPathResource("resources/"+DEFAULT_BLANK_BACKGROUND_IMAGE_NAME)
			File file = resource.getFile()
			assert file.exists()
			fileService.saveImage(FileBuckets.DEFAULT_IMAGES, file.bytes, DEFAULT_BLANK_BACKGROUND_IMAGE_NAME)
		}
	}
	
	/**
	 * Add a new layer image to wall
	 * 
	 * @param wall The wall to add the layer to
	 * @param layer The wall layer domain object
	 * @param bytes The image (transparent .PNG)
	 * 
	 * @return the updated wall object, or false if invalid layer
	 */
	def addLayer(Wall wall, WallLayer layer, byte[] bytes) {
		if(!validateWallLayer(wall, layer, bytes)) {
			return false
		}
		def file = fileService.saveImage(FileBuckets.WALL_LAYERS, bytes, null, [wallId: wall.id])
		log.info "file id: ${file.id.toString()}"
		layer.layerImageId = file.id.toString()
		layer.dateCreated = new Date()
		log.info "saving layer: ${layer} to wall ${wall}"
		wall.layers.add(layer)
		def ret = wall.save()
		createFlattenedImagesAsync(wall)
		return ret
	}
	
	/**
	 * Create flattened images (all types) asynchronously
	 * 
	 * TODO: logic for supporting future types automatically
	 * 
	 * @param wall
	 * @return
	 */
	def createFlattenedImagesAsync(Wall wall) {
		// PNG must be created synchronously, since it will be returned in "add layer" request...
		createFlattenedImage(wall, ImageTypes.PNG)
		// web version can be created in bg
		final Thread t = Thread.start {
			// TODO: should have wall level locking for concurrency..
			createFlattenedImage(wall, ImageTypes.JPG)
		}
	}

	/**
	 * Check that the layer being added to wall is valid
	 * 
	 * @param wall The wall object
	 * @param wallImageInstance The WallImage being added
	 * @param imageBytes The image being added, as byte array
	 * @return
	 */
	def validateWallLayer(wall, wallImageInstance, imageBytes) {
		if(!wall || !wallImageInstance) {
			wallImageInstance.errors.reject("mgraffiti.image.invalidWall", "Invalid wall (${wall}) or image (${wallImageInstance}) uploaded")
			return false
		}

		if(!imageBytes || imageBytes.length < 10) {
			wallImageInstance.errors.reject("mgraffiti.image.notfound", "No image found")
			return false
		}

		if(!isPng(imageBytes)) {
			wallImageInstance.errors.reject("mgraffiti.image.invalid", "Image not valid PNG!")
			return false
		}

		def thisDimensions = getImageDimensions(new ByteArrayInputStream(imageBytes), true)
		def wallDimensions = getBlankBackgroundImage().metaData.dimensions

		if(thisDimensions != wallDimensions) {
			wallImageInstance.errors.reject("virtualtoiletwall.image.invalidDimensions", "Image must have same dimensions as wall background (${wallDimensions} vs ${thisDimensions})")
			return false
		}
		return true
	}

	/**
	 * Get all layer images for wall
	 * 
	 * @param wall The wall whose images are wanted
	 * @return List of GridFSDBFile instances
	 */
	def getLayersForWall(Wall wall) {
		def list = []
		wall.layers?.each {
			list << fileService.getFile(FileBuckets.WALL_LAYERS, new org.bson.types.ObjectId(it.layerImageId))
		}
		list
	}

	/**
	 * Get dimensions for an image
	 * 
	 * @param inputStream The image file (at least .JPG, .PNG supported)
	 * @return map with keys width, height
	 */
	def getImageDimensions(def inputStream, def close = false) {
		try {
			def img = ImageIO.read(inputStream)
			return [width: img?.width, height: img?.height]
		} finally {
			if (close) {
				inputStream?.close()
			}
		}
	}

	/**
	 * Check if image is a valid PNG file, based on file header
	 * 
	 * @param bytes The PNG file as byte array
	 * @return true or false
	 */
	def isPng(byte[] bytes) {
		//def expected = [0x89, 0x50, 0x4e, 0x47]
		def expected = [-119, 80, 78, 71]
		return (bytes.size() > 4 && bytes[0..3] == expected)
	}
	
}
