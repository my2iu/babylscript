babyltest = false;
try {
	if (babyl) babyltest = true;
} catch (e) {}

if (!babyltest) {
	// Check for ES5 features needed for this implementation of Babylscript
	if (!Object.getPrototypeOf || !Object.defineProperty)
		alert('Your version of JavaScript is too old and does not support the ES5 features needed to run Babylscript.');
	
	// Insert a translation mapping into every JavaScript object
	Object.defineProperty(Object.prototype, 'babylscript_translations', {value: {}});

	// Wrap the global object so that we can localize them (NOTE: we use "this" as
	// the global object, so it needs to change if we put this code in a function)
	//
	if (!this.window) window = this;   // Rhino has no window object
	var babylroot = window;
	babylroot.babylscript_translations = {};  // Can't defineProperty() on the root object for some reason

	// Function thats adds a translation mapping to an object (and to 
	// any objects along the prototype chain)
	//
	var babylwrap = function(obj) {
		if (obj != null && !obj.hasOwnProperty('babylscript_translations'))
		{
			var objCreator = function() {};
			objCreator.prototype = babylwrap(Object.getPrototypeOf(obj)).babylscript_translations;
			var trans = new objCreator(); 
			Object.defineProperty(obj, 'babylscript_translations', {value:trans});
		} 
		return obj;
	};

	// Code that does the lookup of translated names
	//
	function babyllookup(babylobj, lang, name)
	{
		// Browser host objects might not inherit from Object, so they
		// might not have translation mappings
		if (babylobj && babylobj.babylscript_translations)
			return babylobj.babylscript_translations[lang + '->' + name] || name;
		else
			return name;
	}
	function babylreverselookup(babylobj, lang, name)
	{
		// Browser host objects might not inherit from Object, so they
		// might not have translation mappings
		if (babylobj.babylscript_translations)
			return babylobj.babylscript_translations[lang + '<-' + name] || name;
		else
			return name;
	}
	
	// Various helper functions
	//
	var babyl = {};
	(function() {
		var langRemapTable = {
$$LANG_REMAP$$
		};
		babyl.addTranslation = function(obj, lang, from, to) {
			lang = babyl.langRemap(lang);
			babylwrap(obj).babylscript_translations[lang + '->' + from] = to;
			// It might be better to calculate the reverse translations
			// lazily since they're rarely used?
			babylwrap(obj).babylscript_translations[lang + '<-' + to] = from;
			return to;
		};
		babyl.delTranslation = function(obj, lang, from) {
			lang = babyl.langRemap(lang);
			var wrapped = babylwrap(obj);
			if (wrapped.babylscript_translations.hasOwnProperty(lang + '->' + from)) {
				var to = wrapped.babylscript_translations[lang + '->' + from];
				delete wrapped.babylscript_translations[lang + '->' + from];
				delete wrapped.babylscript_translations[lang + '<-' + to];
			}
		};
		babyl.getTranslation = function(obj, lang, from) {
			lang = babyl.langRemap(lang);
			return babylwrap(obj).babylscript_translations[lang + '->' + from];
		};
		babyl.getReverseTranslation = function(obj, lang, to) {
			lang = babyl.langRemap(lang);
			return babylwrap(obj).babylscript_translations[lang + '<-' + to];
		};
		babyl.langRemap = function(lang) {
			var newlang = langRemapTable[lang];
			if (newlang) return newlang;
			return lang;
		};
	})();

	// The translation tables for the standard library
$$TRANSLATIONS$$
}
