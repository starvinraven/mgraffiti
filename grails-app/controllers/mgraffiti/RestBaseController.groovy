package mgraffiti

import grails.converters.JSON

abstract class RestBaseController {

	protected def sendError(def reason = "Error", def code = 400) {
		def map = ["error" : [code:code, reason: reason]]
		response.status = code
		log.warn "${map}"
		render map as JSON
	}

	protected def getOr404(Class clazz, def id) {
		log.info("getor404 ${clazz} ${id}")
		def o
		if(id instanceof String) {
			o = clazz.get(new org.bson.types.ObjectId(id))
		} else {
			o = clazz.get(id)
		}
		if(!o) {
			sendError("Object for class ${clazz.name} id ${id} not found", 404)
		} else {
			return o
		}
	}
}	

