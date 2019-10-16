package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Token;

public class BabylscriptTest
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
   public void basic() 
   {
      assertEquals(evalStringToString("1+1"), "2");
   }

   @Test
   public void basic2() 
   {
      assertEquals(evalStringToString("var b = 3; b--; b *= 2; b+1;"), "5");
   }

   @Test
   public void basic3() 
   {
      assertEquals(evalStringToString("function mul2(a) { return a * 2; }; mul2(10);"), "20");
   }
   
   @Test
   public void basic4() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['en':'jump'] = 'wow';"), "wow");
   }

   @Test
   public void basic5() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['en':'jump'] = 'wow'; a['en':'jump'];"), "wow");
   }

   @Test
   public void bindsetname() 
   {
      assertEquals(evalStringToString("a = 5;"), "5");
   }

   @Test
   public void name() 
   {
      assertEquals(evalStringToString("a = 5; b = a;"), "5");
   }

   @Test
   public void getfunctionarg() 
   {
      assertEquals(evalStringToString("function f(a,b) { return a+b; } f(2,3);"), "5");
   }

   @Test
   public void getfunctionargandname() 
   {
      assertEquals(evalStringToString("var c = 3; function f(a) { return a+c; } f(2);"), "5");
   }

   @Test
   public void closurenamebindnameandNameAndThis() 
   {
      // Shows a name/bindname/setname for local variables and non-local scope variables
      // Shows a NAME_AND_THIS and VALUE_AND_THIS
      assertEquals(evalStringToString("function f(a) {var c = 2; return function() {return a + c;}} f(3)();"), "5");
   }

   @Test
   public void setprop() 
   {
      // Shows a name/bindname/setname for local variables and non-local scope variables
      assertEquals(evalStringToString("var a = new Object(); a.go = 5;"), "5");
   }

   @Test
   public void getprop() 
   {
      // Shows a name/bindname/setname for local variables and non-local scope variables
      assertEquals(evalStringToString("var a = new Object(); a.go = 2; a.go+3;"), "5");
   }

   @Test
   public void deleteprop() 
   {
      assertEquals(evalStringToString("a=3; delete a; "), "true");
   }

   @Test
   public void deleteprop2() 
   {
      assertEquals(evalStringToString("a=new Object(); a['go'] = 5; delete a['go']; "), "true");
   }

   @Test
   public void deleteprop3() 
   {
      assertEquals(evalStringToString("a=new Object(); a['go'] = 5; delete a.go; "), "true");
   }

   @Test 
   public void enumerateKeys()
   {
       assertEquals("012", 
           evalStringToString("a=[1,2,3]; var b = ''; for (var key in a) b += key; b; "));
   }

   @Test 
   public void enumerateKeys2()
   {
       assertEquals("babylscript",
           evalStringToString("a={ba:0,byl:1,script:2}; var b = ''; for (var key in a) b += key; b; "));
   }

   @Test
   public void traceSetElem() 
   {
//      Token.printICode = true;
//      Token.printNames = true;
//      evalStringToString("var a=new Object(); delete a.b;");
//      evalStringToString("var a=new Object(); for (var b in a) java.lang.System.out.println(a[b]);");
//
////      evalStringToString("var a = new Object(); java.lang.System.out.println(toString(a.prototype)); java.lang.System.out.println(toString(a['prototype']));");
////      evalStringToString("var a = new Object(); a['prototype'] = 5; java.lang.System.out.println(toString(a.prototype)); java.lang.System.out.println(toString(a['prototype']));");
//
////      assertEquals(evalStringToString("var a = new Object(); a['test'] = 'wow'; a.test2 = 'wow';"), "wow");
//      
//      Token.printICode = false;
//      Token.printNames = false;
   }

}
