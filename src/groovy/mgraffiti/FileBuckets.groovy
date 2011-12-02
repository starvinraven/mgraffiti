package mgraffiti

enum FileBuckets {
	WALL_LAYERS("wallLayers"),
	WALL_FLATTENED("wallFlattened"),
	DEFAULT_IMAGES("defaultImages")

	String collectionName
	
	public FileBuckets(String collectionName) {
		this.collectionName = collectionName
	}

	public String toString() {
		collectionName
	}
	
}