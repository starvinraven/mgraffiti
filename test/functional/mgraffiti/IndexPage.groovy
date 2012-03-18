package mgraffiti

import geb.Page

class IndexPage extends Page {
	
	static url = ""

	static at = { 
		title ==~ /mGraffiti/ 
	}

	static content = {
		popularities { $("p.pop") }
		popularity { i -> popularities[i] }
		walls { $("div.wall") }
	}

}
