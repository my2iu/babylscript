package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class BabylscriptEsperantoTest
{
   Scriptable scope;

   @Before
   public void setUp() throws Exception
   {
      Context cx = Context.enter();
      cx.setLocale(Locale.ENGLISH);
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
      cx.setLocale(Locale.ENGLISH);
      cx.setOptimizationLevel(-1);
      try {
         return Context.toString(cx.evaluateString(scope, code, "<test>", 0, null));
      } finally {
         Context.exit();
      }
   }
   
   @Test
   public void basic1() 
   {
      assertEquals("22.2", evalStringToString("---eo---22,2"));
   }
   
   @Test
   public void basic2() 
   {
      assertEquals("5", evalStringToString("---eo--- variabla a = 2; se (vera) a += 3; alie a += 1; a;"));
   }
   
}
