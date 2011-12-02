package mgraffiti

enum ImageTypes {
	PNG("PNG", false),
	JPG("JPG", true),
	JPG_SMALL("JPG", true, [750, 300])

	String format
	List scaleTo = null
	boolean insertDefaultBackground
	public static final defaultSize = [2000, 800] 

	public ImageTypes(format, insertDefaultBackground, scaleTo = null) {
		this.format = format
		this.insertDefaultBackground = insertDefaultBackground
		this.scaleTo = scaleTo
	}
}