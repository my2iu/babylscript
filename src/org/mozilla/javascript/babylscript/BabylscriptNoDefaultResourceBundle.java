package org.mozilla.javascript.babylscript;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * The regular ResourceBundle uses the default locale when
 * creating the resource bundle. We want deterministic behaviour,
 * so we've made our own version of ResourceBundle for Babylscript
 * that does not use the default locale when constructing the
 * bundle and which uses its own caching system.
 */

@Deprecated public class BabylscriptNoDefaultResourceBundle 
{
//    static HashMap<String, PropertyResourceBundle> cache = 
//            new HashMap<String, PropertyResourceBundle>();
//    
//    /**
//     * Figure out the chain of property file names that we need
//     * to use to construct the resource bundle
//     */
//    private static ArrayList<String> getBundleHierarchy(String fileName, Locale locale)
//    {
//        ArrayList<String> nameHierarchy = new ArrayList<String>();
//        
//        nameHierarchy.add(fileName);
//        if (locale == null) return nameHierarchy;
//        if (locale.getLanguage() == null || locale.getLanguage().length() == 0)
//            return nameHierarchy;
//        
//        fileName += "_" + locale.getLanguage();
//        nameHierarchy.add(fileName);
//        if (locale.getCountry() == null || locale.getCountry().length() == 0)
//            return nameHierarchy;
//
//        fileName += "_" + locale.getCountry();
//        nameHierarchy.add(fileName);
//        if (locale.getVariant() == null || locale.getVariant().length() == 0)
//            return nameHierarchy;
//
//        fileName += "_" + locale.getVariant();
//        return nameHierarchy;
//    }
//
//    public static ResourceBundle getBundle(String fileName, Locale locale)
//    {
//        return getBundle(fileName, locale, BabylscriptNoDefaultResourceBundle.class.getClassLoader());
//    }
//
//    public static ResourceBundle getBundle(String fileName, Locale locale, ClassLoader loader)
//    {
//        ArrayList<String> nameHierarchy = getBundleHierarchy(fileName, locale);
//        
//        // See if we have the whole thing in the cache
//        if (cache.containsKey(nameHierarchy.get(nameHierarchy.size() - 1)))
//            return cache.get(nameHierarchy.get(nameHierarchy.size() - 1));
//        
//        // Construct the resource bundle then
//        PropertyResourceBundle last = null;
//        for (String name: nameHierarchy)
//        {
//            // See if we already have this version in the cache
//            if (cache.containsKey(name))
//            {
//                last = cache.get(name);
//                continue;
//            }
//            
//            // Load the file into the cache
//            PropertyResourceBundle newBundle = null;
//            try {
//                newBundle = loadPropertyFile(name, last, loader);
//            } catch (IOException e) {}
//            if (newBundle == null) return last;
//            cache.put(name, newBundle);
//            last = newBundle;
//        }
//        
//        return last;
//    }
//    
//    private static PropertyResourceBundle loadPropertyFile(String fileName, ResourceBundle parent, ClassLoader cl) throws IOException
//    {
//        InputStream in = cl.getResourceAsStream(fileName + ".properties");
//        if (in == null) return null;
//        return new PropertyResourceBundleWithParent(in, parent);
//    }
//    
//    public static class PropertyResourceBundleWithParent extends PropertyResourceBundle
//    {
//        public PropertyResourceBundleWithParent(InputStream stream, ResourceBundle parent)
//                throws IOException {
//            super(stream);
//            if (parent != null)
//                setParent(parent);
//        }
//    }
}
