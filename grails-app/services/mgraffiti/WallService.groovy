package mgraffiti

class WallService {

	static transactional = false

	def imageService
	def fileService

	def create(Wall wall) {
		wall.save()
	}

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
