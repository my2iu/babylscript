package org.mozilla.javascript.babylscript;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TokenStream;
import org.mozilla.javascript.TokenStream.LanguageMode;
import org.mozilla.javascript.babylscript.gen.Keywords;
import org.mozilla.javascript.babylscript.gen.Objects;

/**
 * This class holds all the translated names used in the JavaScript standard
 * library. By putting everything in this central location, it's easier to
 * to make a translation, hopefully.
 */

public class TranslatedNameBindings
{
    public static Map<String, String[]> EquivalentLanguageNames;  // read-only
    static {
        EquivalentLanguageNames = new HashMap<String, String[]>();
        // The equivalent language name mappings must always show ALL the equivalent names and in the same order
        EquivalentLanguageNames.put("ar", new String[] {"ar", "\u0639\u0631\u0628\u064a"});
        EquivalentLanguageNames.put("\u0639\u0631\u0628\u064a", new String[] {"ar", "\u0639\u0631\u0628\u064a"});

        EquivalentLanguageNames.put("zh", new String[] {"zh", "\u4e2d\u6587", "\u7b80\u4f53"});
        EquivalentLanguageNames.put("\u4e2d\u6587", new String[] {"zh", "\u4e2d\u6587", "\u7b80\u4f53"});
        EquivalentLanguageNames.put("\u7b80\u4f53", new String[] {"zh", "\u4e2d\u6587", "\u7b80\u4f53"});

        EquivalentLanguageNames.put("hi", new String[] {"hi", "\u0939\u093f"});
        EquivalentLanguageNames.put("\u0939\u093f", new String[] {"hi", "\u0939\u093f"});

        EquivalentLanguageNames.put("ja", new String[] {"ja", "\u65e5\u672c\u8a9e"});
        EquivalentLanguageNames.put("\u65e5\u672c\u8a9e", new String[] {"ja", "\u65e5\u672c\u8a9e"});

        EquivalentLanguageNames.put("ru", new String[] {"ru", "\u0440\u0443"});
        EquivalentLanguageNames.put("\u0440\u0443", new String[] {"ru", "\u0440\u0443"});

        EquivalentLanguageNames.put("bn", new String[] {"bn", "\u09AC\u09BE\u0982"});
        EquivalentLanguageNames.put("\u09AC\u09BE\u0982", new String[] {"bn", "\u09AC\u09BE\u0982"});
        
        EquivalentLanguageNames.put("ko", new String[] {"ko", "\uD55C\uAD6D\uC5B4"});
        EquivalentLanguageNames.put("\uD55C\uAD6D\uC5B4", new String[] {"ko", "\uD55C\uAD6D\uC5B4"});

        EquivalentLanguageNames.put("id", new String[] {"id", "ms"});
        EquivalentLanguageNames.put("ms", new String[] {"id", "ms"});

        EquivalentLanguageNames = Collections.unmodifiableMap(EquivalentLanguageNames);
    }

    public static Map<String, Map<String, String>> keywordResourceMap = new HashMap<String, Map<String, String>>();
    public static Map<String, Map<String, String>> langResourceMap = new HashMap<String, Map<String, String>>();
    static {
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.en), arrayToMap(Objects.en));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.eo), arrayToMap(Objects.eo));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.fr), arrayToMap(Objects.fr));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.ro), arrayToMap(Objects.ro));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.pt), arrayToMap(Objects.pt));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.ar), arrayToMap(Objects.ar));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.zh), arrayToMap(Objects.zh));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.hi), arrayToMap(Objects.hi));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.es), arrayToMap(Objects.es));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.ja), arrayToMap(Objects.ja));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.de), arrayToMap(Objects.de));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.ru), arrayToMap(Objects.ru));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.bn), arrayToMap(Objects.bn));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.ko), arrayToMap(Objects.ko));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.tr), arrayToMap(Objects.tr));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.id), arrayToMap(Objects.id));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.it), arrayToMap(Objects.it));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.sw), arrayToMap(Objects.sw));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.nl), arrayToMap(Objects.nl));
        langResourceMap.put(TokenStream.languageModeToString(LanguageMode.pl), arrayToMap(Objects.pl));

        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.en), arrayToMap(Keywords.en));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.eo), arrayToMap(Keywords.eo));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.fr), arrayToMap(Keywords.fr));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.ro), arrayToMap(Keywords.ro));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.pt), arrayToMap(Keywords.pt));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.ar), arrayToMap(Keywords.ar));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.zh), arrayToMap(Keywords.zh));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.hi), arrayToMap(Keywords.hi));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.es), arrayToMap(Keywords.es));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.ja), arrayToMap(Keywords.ja));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.de), arrayToMap(Keywords.de));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.ru), arrayToMap(Keywords.ru));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.bn), arrayToMap(Keywords.bn));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.ko), arrayToMap(Keywords.ko));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.tr), arrayToMap(Keywords.tr));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.id), arrayToMap(Keywords.id));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.it), arrayToMap(Keywords.it));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.sw), arrayToMap(Keywords.sw));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.nl), arrayToMap(Keywords.nl));
        keywordResourceMap.put(TokenStream.languageModeToString(LanguageMode.pl), arrayToMap(Keywords.pl));
    }
    
    private static void fillTranslationsFromResourceBundle(Scriptable obj, String lang, Map<String, String> res, String[] english)
    {
        for (String key: english)
            obj.putTranslatedName(lang, res.get(key), obj, key);
    }


    public static void initStandardTranslations(Scriptable scope)
    {
        for (Map.Entry<String, Map<String, String>> entry: langResourceMap.entrySet())
        {
            configureAllTranslations(scope, entry.getKey(), entry.getValue());
        }
    }

    public static void initCustomTranslation(Scriptable scope, Map<String, String> translations)
    {
        String lang = "test";
        Map<String, String> map = propertiesToMap(translations, langResourceMap.get("en"));
        configureAllTranslations(scope, lang, map);
    }
    
    public static void configureAllTranslations(Scriptable scope, String lang, Map<String, String> res)
    {
        ScriptableObject.clearTranslations(scope, lang);
        configureGlobalScopeTranslations(scope, lang, res);
        Scriptable obj = ScriptableObject.getFunctionPrototype(scope);
        configureBaseFunctionPrototypeTranslations(obj, lang, res);
        obj = ScriptableObject.getObjectPrototype(scope);
        ScriptableObject.clearTranslations(obj, lang);
        configureObjectPrototypeTranslations(obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Error");
        ScriptableObject.clearTranslations(obj, lang);
        configureErrorPrototypeTranslations(obj, lang, res);
        obj = (Scriptable)scope.get("Array", scope);
        ScriptableObject.clearTranslations(obj, lang);
        configureArrayConstructorTranslations((Function)obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Array");
        ScriptableObject.clearTranslations(obj, lang);
        configureArrayPrototypeTranslations(obj, lang, res);
        obj = (Scriptable)scope.get("String", scope);
        ScriptableObject.clearTranslations(obj, lang);
        configureStringConstructorTranslations((Function)obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "String");
        ScriptableObject.clearTranslations(obj, lang);
        configureStringPrototypeTranslations(obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Boolean");
        ScriptableObject.clearTranslations(obj, lang);
        configureBooleanPrototypeTranslations(obj, lang, res);
        obj = (Scriptable)scope.get("Number", scope);
        ScriptableObject.clearTranslations(obj, lang);
        configureNumberConstructurTranslations((Function)obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Number");
        ScriptableObject.clearTranslations(obj, lang);
        configureNumberPrototypeTranslations(obj, lang, res);
        obj = (Scriptable)scope.get("Date", scope);
        ScriptableObject.clearTranslations(obj, lang);
        configureDateConstructorTranslations((Function)obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Date");
        ScriptableObject.clearTranslations(obj, lang);
        configureDatePrototypeTranslations(obj, lang, res);
        obj = (Scriptable)scope.get("Math", scope);
        ScriptableObject.clearTranslations(obj, lang);
        configureMathTranslations(obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Call");
        ScriptableObject.clearTranslations(obj, lang);
        configureCallPrototypeTranslations(obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Script");
        ScriptableObject.clearTranslations(obj, lang);
        configureScriptPrototypeTranslations(obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "Iterator");
        ScriptableObject.clearTranslations(obj, lang);
        configureIteratorPrototypeTranslations(obj, lang, res);
        obj = ScriptableObject.getClassPrototype(scope, "RegExp");
        if (obj != null)
        {
            ScriptableObject.clearTranslations(obj, lang);
            configureRegExpPrototypeTranslations(obj, lang, res);
        }
//        configureRegExpMatchesPrototypeTranslations(ScriptableObject.getClassPrototype(scope, "Object"), lang, res);
    }
    
    // TODO: the activation object also has translations
    // +callee
    // +length
    // +caller

    public static String [] GlobalScopeNames = new String[] {
            // TODO: arguments object needs special handling because the compiler 
            // identifies it and treats it specially
            "arguments",
            
            "alert",
            "Array",
            "Boolean",
            "Call",
            "ConversionError",
            "Date",
            "decodeURI",
            "decodeURIComponent",
            "encodeURI",
            "encodeURIComponent",
            "Error",
            "escape",
            "eval",
            "EvalError",
            "Function",
            "Infinity",
            "InternalError",
            "isFinite",
            "isNaN",
            "isXMLName",
            "Iterator",
            "JavaException",
            "LIBRARY_SCOPE",
            "Math",
            "NaN",
            "Number",
            "Object",
            "parseFloat",
            "parseInt",
            "RangeError",
            "ReferenceError",
            "RegExp",
            "Script",
            "StopIteration",
            "String",
            "SyntaxError",
            "TypeError",
            "undefined",
            "unescape",
            "uneval",
            "URIError",
            "With",
    };
    protected static void configureGlobalScopeTranslations(Scriptable scope, String lang, Map<String, String> res)
    {
        Scriptable obj = scope;
        String [] names = GlobalScopeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] BaseFunctionPrototypeNames = new String[] {
            "name",
            "arity",
            "length",
            "prototype",

            // TODO: Things like arguments are usually handled specially
            "arguments",
    //      "constructor",
            "toString",
            "toSource",
            "apply",
            "call",
    };
    protected static void configureBaseFunctionPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = BaseFunctionPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] ObjectPrototypeNames = new String[] {
            "constructor",
            "toString",
            "toLocaleString",
            "valueOf",
            "hasOwnProperty",
            "propertyIsEnumerable",
            "isPrototypeOf",
            "toSource",
            "__defineGetter__",
            "__defineSetter__",
            "__lookupGetter__",
            "__lookupSetter__",
    }; 
    protected static void configureObjectPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = ObjectPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] ErrorPrototypeNames = new String[] {
            "name",
            "message",
            "fileName",
            "lineNumber",
    };
    protected static void configureErrorPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = ErrorPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] ArrayConstructorNames = new String[] {
            "join",
            "reverse",
            "sort",
            "push",
            "pop",
            "shift",
            "unshift",
            "splice",
            "concat",
            "slice",
            "indexOf",
            "lastIndexOf",
            "every",
            "filter",
            "forEach",
            "map",
            "some",
    };
    protected static void configureArrayConstructorTranslations(Function obj, String lang, Map<String, String> res)
    {
    	String [] names = ArrayConstructorNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] ArrayPrototypeNames = new String[] {
            "length",
            "join",
            "reverse",
            "sort",
            "push",
            "pop",
            "shift",
            "unshift",
            "splice",
            "concat",
            "slice",
            "indexOf",
            "lastIndexOf",
            "every",
            "filter",
            "forEach",
            "map",
            "some",
    };
    protected static void configureArrayPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = ArrayPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] StringConstructorNames = new String[] {
            "fromCharCode",

            // The methods below are defined for the constructor in Rhino, but they
            // make no sense there, and they don't seem to be consistently defined,
            // and I don't think they are defined any more in modern versions of
            // JavaScript
            "charAt",
            "charCodeAt",
            "indexOf",
            "lastIndexOf",
            "split",
            "substring",
            "toLowerCase",
            "toUpperCase",
            "substr",
            "concat",
            "slice",
            "equalsIgnoreCase",
            "match",
            "search",
            "replace",
            "localeCompare",
            "toLocaleLowerCase",
    };

    protected static void configureStringConstructorTranslations(Function obj, String lang, Map<String, String> res)
    {
    	String [] names = StringConstructorNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] StringPrototypeNames = new String[] {
            "length",
            "valueOf",
            "charAt",
            "charCodeAt",
            "indexOf",
            "lastIndexOf",
            "split",
            "substring",
            "toLowerCase",
            "toUpperCase",
            "substr",
            "concat",
            "slice",
            "bold",
            "italics",
            "fixed",
            "strike",
            "small",
            "big",
            "blink",
            "sup",
            "sub",
            "fontsize",
            "fontcolor",
            "link",
            "anchor",
            "equals",
            "equalsIgnoreCase",
            "match",
            "search",
            "replace",
            "localeCompare",
            "toLocaleLowerCase",
            "toLocaleUpperCase",
    };
    protected static void configureStringPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = StringPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] BooleanPrototypeNames = new String[] {
            "valueOf",
    };
    protected static void configureBooleanPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = BooleanPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] NumberConstructorNames = new String[] {
            "NaN",
            "POSITIVE_INFINITY",
            "NEGATIVE_INFINITY",
            "MAX_VALUE",
            "MIN_VALUE",
    };
    protected static void configureNumberConstructurTranslations(Function obj, String lang, Map<String, String> res)
    {
    	String [] names = NumberConstructorNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] NumberPrototypeNames = new String[] {
            "valueOf",
            "toFixed",
            "toExponential",
            "toPrecision",
    };
    protected static void configureNumberPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = NumberPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] DateConstructorNames = new String[] {
            "now",
            "parse",
            "UTC",
    };
    protected static void configureDateConstructorTranslations(Function obj, String lang, Map<String, String> res)
    {
    	String [] names = DateConstructorNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] DatePrototypeNames = new String[] {
            "toTimeString",
            "toDateString",
            "toLocaleTimeString",
            "toLocaleDateString",
            "toUTCString",
            "valueOf",
            "getTime",
            "getYear",
            "getFullYear",
            "getUTCFullYear",
            "getMonth",
            "getUTCMonth",
            "getDate",
            "getUTCDate",
            "getDay",
            "getUTCDay",
            "getHours",
            "getUTCHours",
            "getMinutes",
            "getUTCMinutes",
            "getSeconds",
            "getUTCSeconds",
            "getMilliseconds",
            "getUTCMilliseconds",
            "getTimezoneOffset",
            "setTime",
            "setMilliseconds",
            "setUTCMilliseconds",
            "setSeconds",
            "setUTCSeconds",
            "setMinutes",
            "setUTCMinutes",
            "setHours",
            "setUTCHours",
            "setDate",
            "setUTCDate",
            "setMonth",
            "setUTCMonth",
            "setFullYear",
            "setUTCFullYear",
            "setYear",
    };
    protected static void configureDatePrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = DatePrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] MathNames = new String[] {
            "abs",
            "acos",
            "asin",
            "atan",
            "atan2",
            "ceil",
            "cos",
            "exp",
            "floor",
            "log",
            "max",
            "min",
            "pow",
            "random",
            "round",
            "sin",
            "sqrt",
            "tan",
            "E",
            "PI",
            "LN10",
            "LN2",
            "LOG2E",
            "LOG10E",
            "SQRT1_2",
            "SQRT2",
    };
    protected static void configureMathTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = MathNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    // TODO: arguments are usually handled specially
    public static String [] CallPrototypeNames = new String[] {
            "arguments",
    };
    protected static void configureCallPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = CallPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] ScriptPrototypeNames = new String[] {
            "exec",
            "compile",
    };
    protected static void configureScriptPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = ScriptPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] IteratorPrototypeNames = new String[] {
            "next",
            "__iterator__",
    };
    protected static void configureIteratorPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = IteratorPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] RegExpPrototypeNames = new String[] {
            // TODO: Ignoring RegExp constructor translations for now
            "lastIndex",
            "source",
            "global",
            "ignoreCase",
            "multiline",
            "compile",
            "exec",
            "test",
            "prefix",
    };
    protected static void configureRegExpPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = RegExpPrototypeNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }

    public static String [] RegExpMatchesNames = new String[] {
            // TODO: JavaScript might not have a prototype for its match objects (uses Array objects)
            "index",
            "input",
    };
    protected static void configureRegExpMatchesPrototypeTranslations(Scriptable obj, String lang, Map<String, String> res)
    {
    	String [] names = RegExpMatchesNames;
        fillTranslationsFromResourceBundle(obj, lang, res, names);
    }
    
    static Map<String, String> getKeywordMap(String language)
    {
        if (keywordResourceMap.containsKey(language))
            return keywordResourceMap.get(language);
        else
            return keywordResourceMap.get("en");
    }

    static Map<String, String> propertiesToMap(Map<String, String> translations, Map<String, String> base)
    {
        Map<String, String> map = new HashMap<String, String>();
        for (String key: base.keySet())
        {
            if (translations.containsKey(key))
                map.put(key, translations.get(key));
            else
                map.put(key, key);
        }
        return map;
    }
    
    static Map<String, String> arrayToMap(String [] arr)
    {
        Map<String, String> map = new HashMap<String, String>();
        for (int n = 0; n < arr.length; n+= 2)
            map.put(arr[n], arr[n+1]);
        return map;
    }
}
