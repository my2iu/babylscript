package com.babylscript.gen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;

public class PropertiesToArrayGenerator
{
    Map<String, String> loadPropertyStringsFile(String path) throws IOException
    {
        HashMap<String, String> toReturn = new HashMap<String, String>();
        FileInputStream in = null; 
        try {
            in = new FileInputStream(path);
        } catch (FileNotFoundException e)
        {
            return toReturn;
        }
        PropertyResourceBundle props = new PropertyResourceBundle(in);
        in.close();
        Enumeration<String> keys = props.getKeys();
        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();
            toReturn.put(key, props.getString(key));
        }
        return toReturn;
    }
    
    public void doit(String inPrefix, String []langs, String destDir, String pkg, String className) throws IOException
    {
        Map<String, String> base = loadPropertyStringsFile(inPrefix + ".properties");
        
        FileOutputStream outFile = new FileOutputStream(destDir + "/" + className + ".java");
        PrintStream out = new PrintStream(outFile, false, "UTF-8");
        
        out.println("package " + pkg + ";");
        out.println("public class " + className + " {");
        
        for (String lang: langs)
        {
            Map<String, String> prop = loadPropertyStringsFile(inPrefix + "_" + lang + ".properties");
            Map<String, String> merged = new HashMap<String, String>(base);
            merged.putAll(prop);
            
            out.println("public final static String [] " + lang + " = {");
            for (Map.Entry<String, String> mapping: merged.entrySet())
            {
                out.println("\"" + mapping.getKey() + "\", \"" + mapping.getValue() + "\",");
            }
            out.println("};");
        }
        
        out.println("}");
        out.close();
    }
    
    public static void main(String[] args) throws IOException
    {
        String destDir = "src/org/mozilla/javascript/babylscript/gen";
        String pkg = "org.mozilla.javascript.babylscript.gen";
        
        String [] langs = {"ar", "de", "en", "eo", "es", "fr", "hi", "ja", "pt", "ro", "ru", "zh", "bn", "ko", "tr", "id", "it", "sw", "nl", "pl"};
        new PropertiesToArrayGenerator().doit("src/org/mozilla/javascript/babylscript/resources/Keywords",
                langs, destDir, pkg, "Keywords");
        new PropertiesToArrayGenerator().doit("src/org/mozilla/javascript/babylscript/resources/Objects", 
                langs, destDir, pkg, "Objects");
    }

}
