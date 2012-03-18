grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
	def gebVersion = "0.6.3"
	def seleniumVersion = "2.0rc3"
	
    repositories {
        inherits true // Whether to inherit repository definitions from plugins
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()

        // uncomment these to enable remote dependency resolution from public Maven repositories
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.16'
		compile 'org.codehaus.gpars:gpars:0.12'
		compile("org.seleniumhq.selenium:selenium-htmlunit-driver:$seleniumVersion") {
			exclude "xml-apis"
		}
		compile("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
		compile("org.seleniumhq.selenium:selenium-firefox-driver:$seleniumVersion")
		compile "org.codehaus.geb:geb-spock:$gebVersion"
    }

    plugins {
		// exclusively mongo:
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.7"
        runtime ":resources:1.1.6"
		compile ":mongodb:1.0.0.RC4"
		compile ":rest:0.7"
		compile ":joda-time:1.4"
		//compile ":quartz:1.0-RC1" // no worky
		compile ":quartz:0.4.2"
		compile ":codenarc:0.16.1"
		compile ":spock:0.6"
		compile ":geb:$gebVersion"

        build ":tomcat:$grailsVersion"
    }
}
