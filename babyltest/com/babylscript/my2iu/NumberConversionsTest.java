package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Token;

public class NumberConversionsTest
{
   Scriptable scope;

   Locale none = Locale.ENGLISH;
   Locale en = Locale.ENGLISH;
   Locale fr = Locale.FRENCH;
   Locale ar = new Locale("ar");
   Locale hi = new Locale("hi");
   
   @Before
   public void setUp() throws Exception
   {
      Context cx = Context.enter();
      cx.setOptimizationLevel(-1);
      cx.setLocale(Locale.ENGLISH);
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

   String evalStringToString(Locale loc, String code)
   {
      Context cx = Context.enter();
      cx.setLocale(loc);
      cx.setOptimizationLevel(-1);
      try {
         return Context.toString(cx.evaluateString(scope, code, "<test>", 0, null));
      } finally {
         Context.exit();
      }
   }

   @Test
   public void toNumber1() 
   {
      assertEquals("1.2", evalStringToString(none, "Number('1.2');"));
   }

   @Test
   public void toNumber2() 
   {
      assertEquals("1", evalStringToString(none, "Number('1');"));
   }

   @Test
   public void toNumber3() 
   {
      assertEquals("3", evalStringToString(none, "a = []; a[2] = 3; a['2'];"));
   }

   @Test
   public void toNumber4() 
   {
      assertEquals("3", evalStringToString(none, "a = 1; b = 1.5; if (b == '1.5') a = 3; a;"));
   }

   @Test
   public void toNumber5() 
   {
      assertEquals("2.5", evalStringToString(none, "a = '1.5'; a++; a;"));
   }

   @Test
   public void toNumber6() 
   {
      assertEquals("2.5", evalStringToString(none, "a = '3.5' - 1;"));
   }

   @Test
   public void toNumber7() 
   {
      assertEquals("2.5", evalStringToString(none, "a = 2; if (a < '3') a = 2.5; a;"));
   }

   @Test
   public void toNumber8() 
   {
      assertEquals("1,2", evalStringToString(fr, "Number('1,2');"));
   }

   @Test
   public void toNumber9() 
   {
      assertEquals("1", evalStringToString(fr, "Number('1');"));
   }

   @Test
   public void toNumber10() 
   {
      assertEquals("3", evalStringToString(fr, "a = []; a[2] = 3; a['2'];"));
   }

   @Test
   public void toNumber11() 
   {
      assertEquals("3", evalStringToString(fr, "a = 1; b = 1.5; if (b == '1,5') a = 3; a;"));
   }

   @Test
   public void toNumber12() 
   {
      assertEquals("2,5", evalStringToString(fr, "a = '1,5'; a++; a;"));
   }

   @Test
   public void toNumber13() 
   {
      assertEquals("2,5", evalStringToString(fr, "a = '3,5' - 1;"));
   }

   @Test
   public void toNumber14() 
   {
      assertEquals("2,5", evalStringToString(fr, "a = 2; if (a < '3') a = 2.5; a;"));
   }

   @Test
   public void toNumber15() 
   {
      // Java seems to want to use Western digits for Devanagari numbers.
      // Perhaps they're fixed up during rendering 
      assertEquals("1.2", evalStringToString(hi, "Number('\u0967.\u0968');"));
   }

   @Test
   public void toString1() 
   {
      assertEquals("1", evalStringToString(none, "(1).toString();"));
   }

   @Test
   public void toString2() 
   {
      assertEquals("1.5", evalStringToString(none, "(1.5).toString();"));
   }

   @Test
   public void toString3() 
   {
      assertEquals("1.5", evalStringToString(none, "'' + 1.5;"));
   }

   @Test
   public void toString4() 
   {
      assertEquals("1", evalStringToString(none, "a= {}; a['2.5'] = 1; a[2.5];"));
   }

   @Test
   public void toString5() 
   {
      assertEquals("1", evalStringToString(fr, "(1).toString();"));
   }

   @Test
   public void toString6() 
   {
      assertEquals("1,5", evalStringToString(fr, "(1.5).toString();"));
   }

   @Test
   public void toString7() 
   {
      assertEquals("1,5", evalStringToString(fr, "'' + 1.5;"));
   }

   @Test
   public void toString8() 
   {
      assertEquals("1", evalStringToString(fr, "a= {}; a['2,5'] = 1; a[2.5];"));
   }

   @Test
   public void toString9() 
   {
      // Numbers for Arabic countries are stored internally using western 
      // (Arabic) numerals because some Arab countries use western
      // numerals (they're only *rendered* using different digits) 
      assertEquals("1", evalStringToString(ar, "---ar---a= {}; a['2.5'] = 1; a[\u0662\u066B\u0665];"));
   }

   // Rhino seems to cast things to numbers unnecessarily, which is 
   // normally fine because integers and their string representations
   // are normally bijective, but that's not the case in Babylscript
   @Test
   public void getSetElem0()
   {
       assertEquals("3", evalStringToString(en, "a= {}; a[2] = 3; a['\u0662'] = 1; a['2']"));
   }

   @Test
   public void getSetElem1()
   {
       assertEquals("1", evalStringToString(en, "a= []; a[2] = 3; a['\u0662'] = 1; a['2']"));
   }
}
