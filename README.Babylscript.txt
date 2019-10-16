The code for Babylscript is based on Mozilla's Rhino JavaScript interpreter, 
version 1.7R2. Since the Babylscript implementation is merely a modification
of Rhino's source code, the Babylscript code has the same license terms (a mix 
of MPL1.1 and GPL2).

This package contains the raw source files. You can suck them into an IDE to
use the code. Any build scripts in the package were left-over from the original
Rhino source code, and may not properly build all the source files.

Only the interpreter portion of the Rhino JavaScript engine was modified. The
optimizer/bytecode-compiler does not work properly after the Babylscript 
changes. As a result, to use Babylscript, you must run with optimizations
disabled:

      Context cx = Context.enter();
      cx.setOptimizationLevel(-1);  // disable optimizations
      try {
         Scriptable scope = cx.initStandardObjects();
         cx.evaluateString(scope, code /* Your code here */, null, 0, null)
      } finally {
         Context.exit();
      }

Some of the more non-ECMAScript portions of the engine were also left unchanged
(e.g. destructuring assignments, let, E4X, etc.), so this functionality may
not work properly with Babylscript features or may not work at all.


QUICK-START GUIDE
=================

A small runner program is included in this source package. Compile it using
the Ant build.xml script in the root directory:

   ant babylscriptrun
   
It will create a babylscriptrun.jar file which you can use to run a file
filled with UTF-8 encoded Babylscript code. A sample HelloWorld Babylscript 
program is provided called "sample.js". Run this program using

   java -jar build/babylscriptrun.jar babylrunner/sample.js


BABYLSCRIPT TO JAVASCRIPT COMPILER
==================================

Using Babylscript as a scripting language for Java programs can be a bit
limiting, so this source package also includes an experimental Babylscript
to JavaScript compiler. You can take Babylscript code, translate it into
regular JavaScript, and then run the result in a normal web browser.

First you need to compile the Babylscript to JavaScript translator using
the Ant build.xml script in the root directory:

   ant babyl2js
   
It will create a babyl2js.jar file which you can use to translate files
containing UTF-8 encoded Babylscript code. A sample HelloWorld Babylscript 
program is provided called "sample.babyl2js.js". Translate this program 
into JavaScript using

   java -jar build/babyl2js.jar -html babylrunner/sample.babyl2js.js output.html

 Then open the output.html file in a web browser to run the result.