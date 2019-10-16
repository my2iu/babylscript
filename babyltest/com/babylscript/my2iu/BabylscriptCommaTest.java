package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class BabylscriptCommaTest
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
      assertEquals("321", evalStringToString("var a, b, c; a = ((b = '2', c = '1'), '3'); a + b + c; "));
   }
   
   @Test
   public void basic2() 
   {
      assertEquals("32", evalStringToString("var a = { b: '3', c: '2'}; a.b + a.c;"));
   }
   
   @Test
   public void basic3() 
   {
      assertEquals("3", evalStringToString("var a = [1, 2, 3]; a[2]"));
   }

   @Test
   public void basic4() 
   {
      assertEquals("6", evalStringToString("function a(a, b, c) {return a+b+c;} a(1, 2, 3);"));
   }
   
   @Test
   public void semi1() 
   {
      assertEquals("32", evalStringToString("var a = { b: '3'; c: '2'}; a.b + a.c;"));
   }
   
   @Test
   public void semi2() 
   {
      assertEquals("3", evalStringToString("var a = [1; 2; 3]; a[2]"));
   }

   @Test
   public void semi3() 
   {
      assertEquals("6", evalStringToString("function a(a; b; c) {return a+b+c;} a(1; 2; 3);"));
   }

   @Test
   public void commadecimal1() 
   {
      assertEquals("137.31", evalStringToString("var a = [1.1; .2; 3., 0xf, 1.1e2, 010, 1e-2]; a[0] + a[1] + a[2] + a[3] + a[4] + a[5] + a[6]"));
   }

   @Test
   public void commadecimal2() 
   {
      assertEquals("4.2", evalStringToString("---fr---var a = [1,0; ,2; 3,]; a[0] + a[1] + a[2]"));
   }

   @Test
   public void commadecimal3() 
   {
      assertEquals("137.31", evalStringToString("---fr---var a = [1,1; ,2; 3,; 0xf; 1,1e2; 010; 1e-2]; a[0] + a[1] + a[2] + a[3] + a[4] + a[5] + a[6]"));
   }
   
   @Test
   public void commadecimal4() 
   {
      assertEquals("7", evalStringToString("---fr---var a = [1,5 , 2,5; 3,0 ]; a[0] + a[1] + a[2]"));
   }
}
