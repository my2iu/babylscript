package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Token;

public class StdLibTranslations
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
      try {
         return Context.toString(cx.evaluateString(scope, code, "<test>", 0, null));
      } finally {
         Context.exit();
      }
   }
   
   @Test
   public void stdlib1() 
   {
      assertEquals("2", evalStringToString("new ---fr---Object();---en--- 2;"));
   }

   @Test
   public void stdlib2() 
   {
      assertEquals("2", evalStringToString("new ---fr---Objet();---en--- 2;"));
   }

   @Test
   public void stdlib3() 
   {
      assertEquals("a", evalStringToString("function a() {}; ---fr---a.nom;"));
   }

   @Test
   public void stdlib4() 
   {
      assertEquals("Function", evalStringToString("Function---fr---.nom"));
   }

   @Test
   public void stdlib5() 
   {
      assertEquals("Function", evalStringToString("Function---fr---.constructeur.nom"));
   }

   @Test
   public void stdlib6() 
   {
      assertEquals("Object", evalStringToString("new Object()---fr---.constructeur.nom"));
   }

   @Test
   public void stdlib7() 
   {
      assertEquals("Function", evalStringToString("new Function()---fr---.constructeur.nom"));
   }

   @Test
   public void stdlib8() 
   {
      assertEquals("ReferenceError", evalStringToString("var err; try { z++; } catch (e) {err=e;} ---fr---err.nom;"));
   }

   @Test
   public void stdlib9() 
   {
      assertEquals("filter", evalStringToString("Array---fr---.filtrer.nom"));
   }

   @Test
   public void stdlib10() 
   {
      assertEquals("false", evalStringToString("(/match/)---fr---.multiligne;"));
   }


   @Test
   public void stdlib11() 
   {
      assertEquals("0", evalStringToString("'hello'---fr---.indiceDe('h');"));
   }

}
