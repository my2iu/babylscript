package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.babylscript.CustomTokenizerConfig;

public class CustomLanguageTest
{
    Scriptable scope;

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    String evalStringToString(String code)
    {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        try {
            return Context.toString(cx.evaluateString(scope, code, "<test>", 0, null));
        } finally {
            Context.exit();
        }
    }

    @Test
    public void startInNonEnglish() 
    {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        cx.setLanguageMode("fr");
        try {
            scope = cx.initStandardObjects();
            assertEquals("321", evalStringToString("si (vrai) 321; sinon 123;"));
        } finally {
            Context.exit();
        }
    }

    @Test
    public void changeAKeywordInTestLanguage() 
    {
        CustomTokenizerConfig config = new CustomTokenizerConfig();
        config.keywords.put("if", "si");
        config.keywords.put("else", "sinon");
        Context cx = Context.enter();
        cx.setCustomTokenizerConfig(config);
        cx.setOptimizationLevel(-1);
        try {
            scope = cx.initStandardObjects();
            assertEquals("321", evalStringToString("---test---si (true) 321; sinon 123;"));
        } finally {
            Context.exit();
        }
    }

    @Test
    public void runWithCustomObjectTranslations() 
    {
        Map<String, String> objects = new HashMap<>();
        objects.put("indexOf", "blahblah");
        Context cx = Context.enter();
        cx.setLanguageMode("test");
        cx.setOptimizationLevel(-1);
        try {
            scope = cx.initStandardObjects();
            cx.initCustomLanguageObjectTranslations(scope, objects);
            assertEquals("13", evalStringToString("---test---'hello'.blahblah('e') + '' + 'hello'.lastIndexOf('l')"));
        } finally {
            Context.exit();
        }
    }

    @Test
    public void startInTestLanguage1() 
    {
        Context cx = Context.enter();
        cx.setLanguageMode("test");
        cx.setOptimizationLevel(-1);
        try {
            scope = cx.initStandardObjects();
            assertEquals("5", evalStringToString("if (true) 5;"));
        } finally {
            Context.exit();
        }
    }

    @Test
    public void startInTestLanguage2() 
    {
        CustomTokenizerConfig config = new CustomTokenizerConfig();
        config.keywords.put("if", "si");
        config.keywords.put("else", "sinon");
        Context cx = Context.enter();
        cx.setLanguageMode("test");
        cx.setCustomTokenizerConfig(config);
        cx.setOptimizationLevel(-1);
        try {
            scope = cx.initStandardObjects();
            assertEquals("321", evalStringToString("si (true) 321; sinon 123;"));
        } finally {
            Context.exit();
        }
    }

}
