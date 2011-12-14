package mgraffiti

import javax.imageio.ImageIO
import org.springframework.beans.factory.InitializingBean
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
	 * Actually write the flattened image for a wall to outputStream
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

	def createFlattenedImage(Wall wall, ImageTypes type) {
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
		fileService.saveImage(FileBuckets.WALL_FLATTENED, baos.toByteArray(), fileName)
	}

	def combineImages(def imageFiles, OutputStream outputStream, ImageTypes type) {
		log.info ("Combining images with lengths: ${imageFiles.collect{it?.length}} for type ${type}")
		def startTime = System.currentTimeMillis()

		//BufferedImage combined = new BufferedImage(imageFiles[0].metadata.dimensions.width, imageFiles[0].metadata.dimensions.height, outputType == "PNG" ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB)
		BufferedImage combined = new BufferedImage(ImageTypes.defaultSize[0], ImageTypes.defaultSize[1], type.format == "PNG" ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB)
		Graphics g = combined.getGraphics()
		imageFiles.each {
			if(it) {
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

	GridFSDBFile getDefaultBackgroundImage() {
		def getImage = { fileService.findFileByName(FileBuckets.DEFAULT_IMAGES, DEFAULT_WALL_BACKGROUND_IMAGE_NAME) }
		def image = getImage()
		if(!image) {
			loadDefaultWallBackgroundImage()
			image = getImage()
		}
		image
	}

	GridFSDBFile getBlankBackgroundImage() {
		def getImage = { fileService.findFileByName(FileBuckets.DEFAULT_IMAGES, DEFAULT_BLANK_BACKGROUND_IMAGE_NAME) }
		def image = getImage()
		if(!image) {
			loadDefaultBlankBackgroundImage()
			image = getImage()
		}
		image
	}

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
	
	def addLayer(Wall wall, WallLayer layer, byte[] bytes) {
		if(!validateWallLayer(wall, layer, bytes)) {
			return false
		}
		def file = fileService.saveImage(FileBuckets.WALL_LAYERS, bytes)
		log.info "file id: ${file.id.toString()}"
		layer.layerImageId = file.id.toString()
		layer.dateCreated = new Date()
		log.info "saving layer: ${layer} to wall ${wall}"
		//wall.lastUpdated = new Date()
		wall.layers.add(layer)
		def ret = wall.save()
		createFlattenedImagesAsync(wall)
		return ret
	}
	
	def createFlattenedImagesAsync(Wall wall) {
		// png for client synchronously, since client will get confused otherwise...
		createFlattenedImage(wall, ImageTypes.PNG)
		// web version can be created in bg
		final Thread t = Thread.start {
			// TODO: should have locking for concurrency..
			createFlattenedImage(wall, ImageTypes.JPG)
		}
	}

	def validateWallLayer(wall, wallImageInstance, imageBytes) {
		if(!wall || !wallImageInstance) {
			wallImageInstance.errors.reject("mgraffiti.image.invalidWall", "Invalid wall (${wall}) or image (${wallImageInstance}) uploaded")
			return false
		}

		if(!imageBytes || imageBytes.length < 10) {
			wallImageInstance.errors.reject("mgraffiti.image.notfound", "No image found")
			return false
		}

		//if(!isPng(new ByteArrayInputStream(imageBytes), true)) {
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

	def getLayersForWall(Wall wall) {
		def list = []
		wall.layers?.each {
			list << fileService.getFile(FileBuckets.WALL_LAYERS, new org.bson.types.ObjectId(it.layerImageId))
		}
		list
	}

	def getImageDimensions(def inputStream, def close = false) {
		try {
			def img = ImageIO.read(inputStream)
			return [width: img?.width, height: img?.height]
		} finally {
			if(close) {
				inputStream?.close()
			}
		}
	}

	def isPng(InputStream is, boolean close = false) {
		try {
			def expected = [0x89, 0x50, 0x4e, 0x47]
			def i = 0
			def mismatch = expected.find {
				is.read() != expected[i++]
			}
			return !mismatch
		} catch(Exception e) {
			log.error("Error reading stream!")
			return false
		} finally {
			if(close) {
				is.close()
			}
		}
	}
	
	def isPng(byte[] bytes) {
		//def expected = [0x89, 0x50, 0x4e, 0x47]
		def expected = [-119, 80, 78, 71]
		return (bytes.size() > 4 && bytes[0..3] == expected)
	}
	
}
