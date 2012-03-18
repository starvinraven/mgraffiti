package mgraffiti

import geb.spock.GebReportingSpec
import spock.lang.*

class HtmlSpec extends GebReportingSpec {
	def "there is an index page"() {
		when:
		to IndexPage
		//go ""
		then:
		// title == "mGraffiti"
		at IndexPage
	}
}
