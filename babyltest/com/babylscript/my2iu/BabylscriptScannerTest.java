package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class BabylscriptScannerTest
{
   Scriptable scope;

   @Before
   public void setUp() throws Exception
   {
      Context cx = Context.enter();
      cx.setOptimizationLevel(-1);
      try {
         scope = cx.initStandardObjects();
      } finally {
         Context.exit();
      }
   }

   @After
   public void tearDown() throws Exception
   {
   }

   String evalStringToString(String code)
   {
      Context cx = Context.enter();
      cx.setOptimizationLevel(-1);
      cx.setLocale(Locale.ENGLISH);
      try {
         return Context.toString(cx.evaluateString(scope, code, "<test>", 0, null));
      } finally {
         Context.exit();
      }
   }
   
   @Test
   public void basic1() 
   {
      assertEquals(evalStringToString("---fr------en---null;"), "null");
   }

   @Test
   public void basic2() 
   {
      assertEquals(evalStringToString("---fr---fonction a(b){ retourner b * 2; }; a(3);"), "6");
   }

   @Test
   public void basic3() 
   {
      assertEquals(evalStringToString("---fr---fonction a(b){ ---en--- if (b == 3) ---fr--- retourner b * 2; sinon ---en--- return 8; }; a(3);"), "6");
   }

   @Test
   public void basic4() 
   {
      assertEquals(evalStringToString("---fr---«allo»"), "allo");
   }
   
   @Test
   public void basic5() 
   {
      assertEquals("5", evalStringToString("---fr---var \u00catre = 5; Être;"));
   }

   @Test
   public void basic6() 
   {
      assertEquals(evalStringToString("---ro--- funcție î() {întoarce „hello”;} î();"), "hello");
   }

   @Test
   public void basic7() 
   {
      assertEquals(evalStringToString("---ro--- „hello”.lungime"), "5");
   }

   @Test
   public void basic8() 
   {
      assertEquals("8.25", evalStringToString("---ar---\n\u062f\u0627\u0644\u0629 \u0623\u064a_\u0634\u0626 (\u0623\u060c\u0628\u200e)\n{\n\u0627\u0631\u062c\u0639 \"\u0623\u0647\u0644\u0627\".\u0637\u0648\u0644 + (1.25 + 3)\u061b\n\u200e}\n\u200e \u0623\u064a_\u0634\u0626(0\u060c0)\u061b"));
   }

   @Test
   public void basic9() 
   {
      assertEquals(evalStringToString("Math['\u0639\u0631\u0628\u064a':'\u0628\u0627\u064a']"), evalStringToString("Math['ar':'\u0628\u0627\u064a']"));
   }

   @Test
   public void normalization0() 
   {
      assertEquals("5", evalStringToString("abba = 5; a\uff42\uff42a;"));
   }

   @Test
   public void normalization1() 
   {
      assertEquals("\uff42", evalStringToString("a = \"\uff42\"; a;"));
   }

   @Test
   public void normalization2() 
   {
      assertEquals("hello", evalStringToString("a = ---zh--- \uff62hello\u300d; ---en---a;"));
   }

   @Test
   public void numbers0() 
   {
      assertEquals("3", evalStringToString("---hi--- \u0969"));
   }

   @Test
   public void numbers1() 
   {
      assertEquals("3.25", evalStringToString("---hi--- \u0969.\u0968\u096b"));
   }

   @Test
   public void numbers2() 
   {
      assertEquals("1.25", evalStringToString("---hi--- 1.25"));
   }

   @Test
   public void numbers3() 
   {
      assertEquals("1.25", evalStringToString("---ar--- \u0661\u066B\u0662\u0665"));
   }
   
   @Test
   public void inconsistentQuotations()
   {
      assertEquals("hi", evalStringToString("\u2018hi\u2019"));
      assertEquals("hi", evalStringToString("\u201Dhi\u201C"));
      assertEquals("hi", evalStringToString("\u201Chi\u201D"));
      assertEquals(" hi", evalStringToString("\u2019 hi\u2018"));
   }
}
