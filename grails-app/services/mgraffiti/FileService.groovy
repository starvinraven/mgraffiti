package mgraffiti

import org.bson.types.ObjectId
import com.mongodb.gridfs.GridFS
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSInputFile
import com.mongodb.BasicDBObject

class FileService {
	
	static transactional = false
	
	def imageService
	def mongo
	
	def delete(FileBuckets bucket, ObjectId id) {
		GridFS gridFs = getGridFs(bucket)
		gridFs.remove(id)
	}
	
	/**
	 * Save an image file into GridFS and given bucket
	 * @param bucket 
	 * @param data  
	 * @return the created file
	 */
	GridFSInputFile saveImage(FileBuckets bucket, byte[] bytes, String filename = null) {
		GridFS gridFs = getGridFs(bucket)
		def dimensions = imageService.getImageDimensions(new ByteArrayInputStream(bytes))
		GridFSInputFile inputFile = gridFs.createFile(bytes) // could be File, InputStream, byte[]...
		if(filename) {
			inputFile.filename = filename
		}
		inputFile.metaData = new BasicDBObject(dimensions: dimensions)
		inputFile.save()
		log.info("Saved file ${inputFile} in bucket ${bucket}")
		inputFile
	}
	
	/**
	 * Get a file by its GridFS name in given bucket. Note: returns a single file (unique file names)
	 * 
	 * @param bucket Bucket where file is to be looked for
	 * @param name Name of the file
	 * @return the file, or null if none found
	 */
	GridFSDBFile findFileByName(FileBuckets bucket, String name) {
		GridFS gridFs = getGridFs(bucket)
		gridFs.findOne(name)
	}
	
	/**
	 * Get a file by its GridFS id.
	 * 
	 * @param bucket
	 * @param id
	 * @return the file, or null if none found
	 */
	GridFSDBFile getFile(FileBuckets bucket, ObjectId id) {
		GridFS gridFs = getGridFs(bucket)
		gridFs.find(id)
	}
	
	private GridFS getGridFs(FileBuckets bucket) {
		def mgraffitiDB = getDB()
		new GridFS(mgraffitiDB, bucket.toString())
	}
	
	def getDB() {
		mongo.getDB("mgraffiti")
	}
	
}
