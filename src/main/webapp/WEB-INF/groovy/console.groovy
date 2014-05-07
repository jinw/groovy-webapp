// Process a console form to evaluate a scriptText using a scriptEngine in a JVM.
import javax.script.*

def form() {
	// Get all the script engine names available by inspecting the classpath
	factory = new ScriptEngineManager()
	scriptEngineNames = factory.getEngineFactories().collect{ fac ->
		if (fac.languageName.toLowerCase() == 'ecmascript')
			'JavaScript'
		else
			fac.languageName
	}
	scriptEngineNames.sort()

	// Process Form
	scriptText = params["scriptText"] ?: ''
	scriptEngineName = params["scriptEngineName"] ?: 'Groovy'
	scriptingOutput = null
	webOutResult = null
	if (scriptText.trim() != '') {
		scriptEngine = factory.getEngineByName(scriptEngineName)
		if (scriptEngine == null)
			throw new RuntimeException("Failed to find ScriptEngine " + scriptEngineName)

		outStream = new ByteArrayOutputStream()
		PrintWriter webOut = new PrintWriter(outStream)

		// Script engine binding variables.
		Bindings bindings = scriptEngine.createBindings()
		bindings.put("request", request)
		bindings.put("response", response)
		bindings.put("out", out)
		bindings.put("session", session)
		bindings.put("application", application)
		bindings.put("context", application) // for convenience, we will alias this.
		bindings.put("params", params)
		bindings.put("headers", headers)

		bindings.put("scriptEngine", scriptEngine)
		bindings.put("webout", webOut)

		// Run the scriptText
		try {
			scriptingOutput = scriptEngine.eval(scriptText, bindings)
		} catch (Exception e) {
			throw new RuntimeException("Failed execute scriptText.", e)
		} finally {
			webOut.close()
			webOutResult = outStream.toString()
		}
	}

	// build select tag
	scriptEngineNamesSelectTag = new StringBuilder('''<select name="scriptEngineName">''')
	scriptEngineNames.each { engineName ->
		if (scriptEngineName == engineName)
			scriptEngineNamesSelectTag << "<option selected=\"true\">$engineName</option>"
		else
			scriptEngineNamesSelectTag << "<option>$engineName</option>"
	}
	scriptEngineNamesSelectTag << '''</select>'''

	result = [
		scriptEngineNamesSelectTag : scriptEngineNamesSelectTag,
		scriptEngineName : scriptEngineName,
		scriptText : scriptText,
		scriptingOutput : scriptingOutput ?: '',
		webOutResult : webOutResult ?: ''
	]
	return result
}

request['data'] = [form : form()]
forward("console.gt")
