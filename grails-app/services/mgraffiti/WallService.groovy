package mgraffiti

class WallService {

	static transactional = false

	def imageService
	def fileService

	/**
	 * Create a wall and generate the flattened image (= background)
	 * 
	 * @param wall
	 * @return the saved Wall object or false if save unsuccessful
	 */
	def create(Wall wall) {
		def ret = wall.save()
		imageService.createFlattenedImagesAsync(wall)
		ret
	}

	/**
	 * Find walls near given coordinates
	 * 
	 * @param lat The latitude near which to search
	 * @param lon The longitude near which to search
	 * @param maxDistanceKm Maximum search distance in km
	 * @return List of map objects with keys distance (distance to search center) and obj (the Wall domain object)
	 */
	def findWallsNear(lat, lon, maxDistanceKm) {
		def earthRadiusKm = 6378

		def db = fileService.getDB()
		def coords = [lon as Double, lat as Double]
		maxDistanceKm = maxDistanceKm?.isDouble() ? Double.parseDouble(maxDistanceKm) : 100

		def command = [
					geoNear: "wall",
					near: coords,
					num: 10,
					spherical: true,
					maxDistance: (maxDistanceKm/earthRadiusKm) as Double
				]

		log.info "command: ${command}"
		def c = db.command(command)
		def r = c?.results

		def list = r.collect {
			[
						distance: (it.dis*earthRadiusKm),
						obj: Wall.get(it.obj._id.toString()) // TOOD: prefetch with getAll
					]
		}
		list
	}
}
