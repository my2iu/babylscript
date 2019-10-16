package com.babylscript.my2iu;


import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class BabylscriptToJSTest
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

   String compileToJS(String code) 
   {
      Context cx = Context.enter();
      cx.setLocale(Locale.ENGLISH);
      cx.setOptimizationLevel(-1);
      String result = "";
      try {
         result = cx.compileStringToJS(code, null, 0, true);
      } finally {
         Context.exit();
      }
      return result;
   }
   String compileToJSNoHeaders(String code) 
   {
      Context cx = Context.enter();
      cx.setLocale(Locale.ENGLISH);
      cx.setOptimizationLevel(-1);
      String result = "";
      try {
         result = cx.compileStringToJS(code, null, 0, false);
      } finally {
         Context.exit();
      }
      return result;
   }
   String evalStringToString(String code)
   {
      String js = compileToJS(code);
      Context cx = Context.enter();
      cx.setLocale(Locale.ENGLISH);
      cx.setOptimizationLevel(-1);
      try {
         return Context.toString(cx.evaluateString(scope, js, "<test>", 0, null));
      } finally {
         Context.exit();
      }
   }

   String evalES5StringToString(String code)
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
      assertEquals("32;\n", compileToJSNoHeaders("32"));
   }

   @Test
   public void basic2() 
   {
      assertEquals("32", evalStringToString("32"));
   }

   @Test
   public void basic3() 
   {
      assertEquals("'hello';\n", compileToJSNoHeaders("'hello'"));
   }
   
   @Test
   public void simpleOperators1() 
   {
      assertEquals("24", evalStringToString("16+8"));
   }

   @Test
   public void simpleOperators2() 
   {
      assertEquals("5", evalStringToString("15%10"));
   }

   @Test
   public void simpleOperators3() 
   {
      assertEquals("15", evalStringToString("'1' + 5"));
   }

   @Test
   public void simpleOperators4() 
   {
      assertEquals("-5", evalStringToString("- 5"));
   }

   @Test
   public void simpleOperators5() 
   {
      assertEquals("-6", evalStringToString("~5"));
   }

   @Test
   public void variables1() 
   {
      assertEquals("babylroot[babyllookup(babylroot,'en','a')];\n", compileToJSNoHeaders("a"));
   }

   @Test
   public void variables2() 
   {
      assertEquals("2", evalStringToString("a = 2"));
   }

   @Test
   public void variables3() 
   {
      assertEquals("5", evalStringToString("a = 2; a += 3; a;"));
   }

   @Test
   public void variables4() 
   {
      assertEquals("babylroot[babyllookup(babylroot,'en','a')];\n", compileToJSNoHeaders("var a;"));
   }

   @Test
   public void variables5() 
   {
      assertEquals("babylroot[babyllookup(babylroot,'en','a')],babylroot[babyllookup(babylroot,'en','b')];\n", compileToJSNoHeaders("var a, b;"));
   }

   @Test
   public void variables6() 
   {
      assertEquals("babylroot[babyllookup(babylroot,'en','a')]=2,babylroot[babyllookup(babylroot,'en','b')];\n", compileToJSNoHeaders("var a=2, b;"));
   }

   @Test
   public void variables7() 
   {
      assertEquals("2", evalStringToString("var a; a = 2; a"));
   }

   @Test
   public void statements1() 
   {
      assertEquals("5", evalStringToString("a = 2\n b=3\n a+b"));
   }

   @Test
   public void statements2() 
   {
      assertEquals("5", evalStringToString("a = 3; if (true) a = a + 2; a;"));
   }

   @Test
   public void statements3() 
   {
      assertEquals("5", evalStringToString("a = 3; if (a > 0) a = 5; else a = -2; a;"));
   }

   @Test
   public void statements4() 
   {
      assertEquals("-2", evalStringToString("a = 3; if (a > 5) a = 5; else if (a > 0) a = -2; else a = -5; a;"));
   }

   @Test
   public void statements5() 
   {
      assertEquals("-1", evalStringToString("a = 3; while (a > 0) a = a - 2; a;"));
   }

   @Test
   public void statements6() 
   {
      assertEquals("5", evalStringToString("var b = 2; for (a = 0; a < 3; a++) b++; b;"));
   }

   @Test
   public void statements7() 
   {
      assertEquals("8", evalStringToString("var b = 2; for (var a = 0,b=5; a < 3; a++) b++; b;"));
   }

   @Test
   public void statements8() 
   {
      assertEquals("8", evalStringToString("var b = 2; for (;;) { b++; if (b > 7) break; } b;"));
   }

   @Test
   public void statements9() 
   {
      assertEquals("8", evalStringToString("b = 3; try { throw b; } catch(e) {b = e+5;} b;"));
   }

   @Test
   public void function1() 
   {
      assertEquals("2", evalStringToString("function a() { return 2;} a();"));
   }

   @Test
   public void function2() 
   {
      assertEquals("2", evalStringToString("function a(b) { return b;} a(2);"));
   }

   @Test
   public void function3() 
   {
      assertEquals("15", evalStringToString("function a(b, c) { return b+b+c;} a(3,9);"));
   }

   @Test
   public void function4() 
   {
      assertEquals("5", evalStringToString("function a() { function b() {}; b.c = 5; return b;} a().c;"));
   }

   @Test
   public void function5() 
   {
      assertEquals("5", evalStringToString("function a() { return arguments[0];} a(5);"));
   }

   @Test
   public void dot1() 
   {
      assertEquals("5", evalStringToString("a = new Object(); a.b = 5; a.b;"));
   }

   @Test
   public void dot2() 
   {
      assertEquals("true", evalStringToString("a = new Object(); ---fr--- a.constructeur == Objet;"));
   }

   @Test
   public void lookup1() 
   {
      assertEquals("5", evalStringToString("a = new Object(); a.b = 5; a['b']"));
   }

   @Test
   public void arrayliteral1() 
   {
      assertEquals("5", evalStringToString("a = [22,23,,5]; a[3]"));
   }

   @Test
   public void objectliteral1() 
   {
      assertEquals("5", evalStringToString("a = { get b() { return 2;}, c:3};a.b+ a['c']"));
   }

   @Test
   public void delete1() 
   {
      assertEquals("5", evalStringToString("a = 3; delete a; b = 2; if (!a) b = 5; b;"));
   }

   @Test
   public void forin1() 
   {
      assertEquals("abc", evalStringToString("a = {a:1, b:2, c:3}; str=''; for (d in a) str += d; str;"));
   }

   @Test
   public void forin2() 
   {
       assertEquals("abc", evalStringToString("a = {a:1, b:2, c:3}; str=''; for (var d in a) str += d; str;"));
   }

   @Test
   public void forin3() 
   {
       assertEquals("abk", evalStringToString("a = {a:1, b:2, c:3}; a['fr':'k'] = 'c'; str=''; ---fr---pour (var d dans a) str += d; str;"));
   }

   @Test
   public void forin4() 
   {
       assertEquals("abk", evalStringToString("a = {a:1, b:2, c:3}; a['fr':'k'] = 'c'; str=''; ---fr---pour (d dans a) str += d; str;"));
   }

   @Test
   public void forin5() 
   {
       // This case isn't handled properly yet because we don't translate complex iterators
       assertEquals("abc", evalStringToString("a = {a:1, b:2, c:3}; z = []; a['fr':'k'] = 'c'; str=''; ---fr---pour (z[0] dans a) str += z[0]; str;"));
   }
   
   @Test
   public void switch1() 
   {
      assertEquals("9", evalStringToString("a=3; switch(a) { case 0: b=2; break; case 3: b = 5; case 5: default: b = 9; break;} b;"));
   }

   @Test
   public void trans1() 
   {
      assertEquals("3", evalStringToString("a={b: 2, c:3}; a['fr':'b'] = 'c'; ---fr--- a.b;"));
   }

   @Test
   public void trans2() 
   {
      assertEquals("2", evalStringToString("a={b: 2, c:3}; a['fr':'b'] = 'c'; delete a['fr':'b']; ---fr--- a.b;"));
   }

   @Test
   public void trans3() 
   {
      assertEquals("3", evalStringToString("a={b: 2, c:3}; a['fr':'b'] = a['pt':'b'] = 'c'; ---fr--- a.b;"));
   }


   // 
   // Some corner cases
   //
   
   @Test
   public void lookupNonConstantWithIncrement1() 
   {
      assertEquals("6", evalStringToString("var str = ''; function a() {str += 'a'; return str;} var b ={a:5, aa:10}; b[a()]++; b.a;"));
// SOLUTION INVOLVES SOMETHING LIKE THIS:      
//   var tmp0;
//
//   d = {
//   a : 'a'
//   };
//   function readA(k)
//   {
//    return k.a; 
//   }
//   var a = (tmp0=d)[readA(tmp0)];
//   alert(a);
   }
   
   @Test
   public void lookupNonConstantWithIncrement2() 
   {
      assertEquals("6", evalStringToString("var str = ''; function a() {str += 'a'; return str;} var next = false; function c() { next = !next; return next ? b: {a:20,aa:30};} var b ={a:5, aa:10};  c()[a()]++; b.a;"));
   }

   @Test
   public void assignAddWithTypeChange() 
   {
      assertEquals("3", evalStringToString("var str = 2; str += 'hi';  ---fr--- str.longueur"));
   }

   @Test
   public void scoping1() 
   {
      assertEquals("6", evalStringToString("var c = 1; function a() {  var c = 5; function b() {c++; return c;} return b; } var result = a()();"));
   }

   @Test
   public void scoping2() 
   {
      assertEquals("1", evalStringToString("var c = 1; function a() {  var c = 5; function b() {c++; } return b; } var result = a()(); c;"));
   }

   @Test
   public void scoping3() 
   {
      assertEquals("3", evalStringToString("var c = 1; function a() { if (!c) { var c = 1000; return 3;} return 5; } var result = a();"));
   }

   
   @Test
   public void ES5Test1() 
   {
      assertEquals("b", evalES5StringToString("var str = ''; Object.defineProperty(Object.prototype, 'babyltest', {value: {babyl:'hi'}}); a = { b: ''}; for (n in a) str += n; str;"));
   }

   @Test
   public void ES5Test2() 
   {
      assertEquals("false", evalES5StringToString("var str = ''; Object.defineProperty(Object.prototype, 'babyltest', {value: {babyl:'hi'}}); a = { b: ''}; a.hasOwnProperty('babyltest')"));
   }

   @Test
   public void ES5Test3() 
   {
      assertEquals("true", evalES5StringToString("var str = ''; Object.defineProperty(Object.prototype, 'babyltest', {value: {babyl:'hi'}}); Object.prototype.hasOwnProperty('babyltest')"));
   }

   @Test
   public void ES5Test4() 
   {
      assertEquals("hi", evalES5StringToString("var str = ''; Object.defineProperty(Object.prototype, 'babyltest', {value: {babyl:'hi'}}); a = {}; a.babyltest.babyl"));
   }

   @Test
   public void ES5Test5() 
   {
      assertEquals("true", evalES5StringToString("Object.getPrototypeOf({}) == Object.prototype"));
   }
   
   @Test
   public void HexNumbersTest1() 
   {
      assertEquals("5", evalES5StringToString("0x7f5 - 0x7f0;"));
   }
}
