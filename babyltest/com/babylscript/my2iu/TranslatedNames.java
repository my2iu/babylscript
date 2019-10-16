package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Token;

public class TranslatedNames
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
   public void getelem() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['b'];"), "2");
   }

   @Test
   public void getelem2() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; a['b'];"), "2");
   }

   @Test
   public void getelem3() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; ---fr--- a['b'];"), "2");
   }

   @Test
   public void getelem4() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; ---fr--- a['t'];"), "2");
   }

   @Test
   public void getelem5() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; a['t'];"), "undefined");
   }

   @Test
   public void getelem6() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; a['z'] = ---fr--- a['t']; a['z'];"), "2");
   }

   @Test
   public void getelem7() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = function() {return 2;}; a['b']();"));
   }

   @Test
   public void getelem8() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = 1; a['b']++; a.b;"));
   }

   @Test
   public void getelem9() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = 1; a['fr':'t'] = 'b'; ---fr--- a['t']++; ---en--- a.b;"));
   }

   @Test
   public void getelem10() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = function() {return 2;}; a['fr':'t'] = 'b'; ---fr--- a['t']();"));
   }

   @Test
   public void setelem() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['b'] = 2; a['b'];"), "2");
   }

   @Test
   public void setelem2() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; a['b'] = 2; a['b'];"), "2");
   }

   @Test
   public void setelem3() 
   {
      assertEquals(evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; ---fr--- a['b'] = 2; a['b'];"), "2");
   }

   @Test
   public void setelem4() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; ---fr--- a['t'] = 2; a['b'];"));
   }

   @Test
   public void setelem5() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; a['b'] = 2; a['t'] = 3; a['b'];"));
   }

   @Test
   public void setelem6() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = 0; a['b'] += 2; a['b'];"));
   }

   @Test
   public void getprop() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = 2; a.b;"));
   }

   @Test
   public void getprop2() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; a.b;"));
   }

   @Test
   public void getprop3() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; ---fr--- a.b;"));
   }

   @Test
   public void getprop4() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; ---fr--- a.t;"));
   }

   @Test
   public void getprop5() 
   {
      assertEquals("undefined", evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; a.t;"));
   }

   @Test
   public void getprop6() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = 2; a['fr':'t'] = 'b'; a['z'] = ---fr--- a.t; a.z;"));
   }

   @Test
   public void getprop7() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = function() {return 2;}; a.b();"));
   }

   @Test
   public void getprop8() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = 1; a.b++; a.b;"));
   }

   @Test
   public void getprop9() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = 1; a['fr':'t'] = 'b'; ---fr--- a.t++; ---en--- a.b;"));
   }

   @Test
   public void getprop10() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = function() {return 2;}; a['fr':'t'] = 'b'; ---fr--- a.t();"));
   }

   @Test
   public void setprop() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a.b = 2; a.b;"));
   }

   @Test
   public void setprop2() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; a.b = 2; a.b;"));
   }

   @Test
   public void setprop3() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; ---fr--- a.b = 2; a['b'];"));
   }

   @Test
   public void setprop4() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; ---fr--- a.t = 2; a['b'];"));
   }

   @Test
   public void setprop5() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['fr':'t'] = 'b'; a.b = 2; a.t = 3; a.b;"));
   }

   @Test
   public void setprop6() 
   {
      assertEquals("2", evalStringToString("var a = new Object(); a['b'] = 0; a.b += 2; a['b'];"));
   }

   @Test
   public void name() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 2; window.a;"));
   }

   @Test
   public void name2() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 2; window['fr':'t'] = 'a'; ---fr--- window.t;"));
   }

   @Test
   public void name3() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 2; window['fr':'t'] = 'a'; ---fr--- t;"));
   }

   @Test
   public void name4() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 2; window['fr':'t'] = 'a'; ---fr--- a;"));
   }

   @Test
   public void name5() 
   {
      // name and this
      assertEquals("2", evalStringToString("window = this; var a = function(){return 2;}; window['fr':'t'] = 'a'; ---fr--- t();"));
   }

   @Test
   public void name6() 
   {
      // name inc/dec
      assertEquals("2", evalStringToString("window = this; var a = 1; window['fr':'t'] = 'a'; ---fr--- t++; a;"));
   }

   @Test
   public void setname() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 1; window['fr':'t'] = 'a'; ---fr--- t=2; a;"));
   }

   @Test
   public void setname2() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 1; window['fr':'t'] = 'a'; ---fr--- t=2; a;"));
   }

   @Test
   public void setname3() 
   {
      assertEquals("2", evalStringToString("window = this; var a = 1;  window['fr':'t'] = 'a'; function set() {---fr--- t = 2};---en--- set(); a;"));
   }

   @Test
   public void setname4() 
   {
      assertEquals("2", evalStringToString("window = this; window['fr':'t'] = 'a'; ---fr--- t = 2; ---en---t;"));
   }

   @Test
   public void setname5() 
   {
      assertEquals("2", evalStringToString("window = this; a = 2; ---fr---t = (window['fr':'t'] = 'a'); a;"));
   }

   @Test
   public void setname6() 
   {
      assertEquals("2", evalStringToString("window = this; a = 2; t = 3; ---fr---t = (window['fr':'t'] = 'a'); a;"));
   }

   @Test 
   public void enumerateKeys()
   {
       assertEquals("babylscript",
               evalStringToString("a={ba:0,byl:1,script:2}; a['fr':'go'] = 'ba'; var b = ''; for (var key in a) b += key; b; "));
   }

   @Test 
   public void enumerateKeys2()
   {
       assertEquals("gobylscript",
               evalStringToString("a={ba:0,byl:1,script:2}; a['fr':'go'] = 'ba'; var b = ''; ---fr---pour (---en---var key in a) b += key; b; "));
   }

   @Test 
   public void delete()
   {
       assertEquals("1undefined",
               evalStringToString("a={ba:'1'}; a['fr':'b'] = 'ba'; c = a.ba; delete a.ba; c +=a.ba; c"));
   }

   @Test 
   public void delete2()
   {
       assertEquals("1undefined",
               evalStringToString("a={ba:'1'}; a['fr':'b'] = 'ba'; c = a.ba; delete  a---fr---.b---en---; c +=a.ba; c"));
   }

   @Test 
   public void delete3()
   {
       assertEquals("1undefined",
               evalStringToString("a={ba:'1'}; a['fr':'b'] = 'ba'; c = a.ba; delete ---fr--- a['b']---en---; c +=a.ba; c"));
   }

   @Test 
   public void delete4()
   {
       assertEquals("1undefined",
               evalStringToString("a={ba:'1'}; a['fr':'b'] = 'ba'; c = a.ba; delete ---fr--- a['ba']---en---; c +=a.ba; c"));
   }

   @Test 
   public void delete5()
   {
       assertEquals("no",
               evalStringToString("a=1; this['fr':'b'] = 'a'; delete ---fr--- b ---en---; c=2;  try {c+= a; } catch (e){c='no';};c;"));
   }

   @Test 
   public void delete6()
   {
       assertEquals("3",
               evalStringToString("function k() {} function d() {var a=1; delete a; c=2;  k(); try {c+= a; } catch (e){c='no';};return c;} e=d();e"));
   }

   @Test 
   public void delete7()
   {
       assertEquals("1undefined",
               evalStringToString("a={a:'1'}; a['fr':'b'] = 'a'; c = ---fr---a.b---en---; delete a['fr':'b']; c += ---fr---a.b---en---;c;"));
   }

   @Test 
   public void delete8()
   {
       assertEquals("11",
               evalStringToString("a={a:'1'}; a['fr':'b'] = 'a'; c = ---fr---a.b---en---; c += ---fr---a.b---en---;c;"));
   }

}
