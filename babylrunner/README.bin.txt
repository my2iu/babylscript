The code for Babylscript is based on Mozilla's Rhino JavaScript interpreter, 
version 1.7R2. Since the Babylscript implementation is merely a modification
of Rhino's source code, the Babylscript code has the same license terms (a mix 
of MPL1.1 and GPL2).

This package contains two jar files:

babylscriptrun.jar is a Java-based Babylscript engine wrapped in simple
harness that lets you run Babylscript/Rhino/JavaScript code from the 
command-line. You can script Java using Babylscript code using this
engine. You need Java (version 1.5 or later) to run this jar file.

babyl2js.jar is a Babylscript to JavaScript translator. It translates
Babylscript code into regular JavaScript code that can be run in 
a browser. 


JAVA BABYLSCRIPT ENGINE
=======================

QUICK-START GUIDE 

A sample HelloWorld Babylscript program is provided called "sample.js". 
Run this program using

   java -jar babylscriptrun.jar sample.js


ADDITIONAL INFO

You can also run multiple files by listing each one in turn. This is useful
for including libraries.

   java -jar babylscriptrun.jar lib.js sample.js
   
The programs run using this Babylscript harness must be encoded in UTF-8 
(e.g. in Windows Notepad, when you choose the "Save As..." option, there is an
option that lets you change the encoding). 


BABYLSCRIPT TO JAVASCRIPT COMPILER
==================================

QUICK-START GUIDE 

A sample HelloWorld Babylscript program is provided called "sample.babyl2js.js". 
Translate it to JavaScript using

   java -jar babyl2js.jar -html sample.babyl2js.js output.html
   
Then, open the resulting output.html file in a browser to run the code.


ADDITIONAL INFO 

The babyl2js translator takes two arguments: an input text file of Babylscript
code encoded in UTF-8 and the output file where the translated JavaScript
code will be put (again in UTF-8).

The translator tool has two command-line options. The "-html" option causes
the translator to wrap the resulting output with html <script>...</script>
tags so that the resulting file can be opened directly in a browser (as opposed 
to having the embed the JavaScript file in a separate html file). The "-noheaders"
option will disable the inclusion of the Babylscript runtime library
in the resulting JavaScript code. Although the Babylscript library is needed
to run the translated JavaScript code, if a program consists of multiple
Babylscript/JavaScript files, this runtime library only needs to be included
in the first JavaScript file to be loaded by an html page. The code runs
fine if the runtime library is included more than once, but your programs
will be smaller if they are only included once.   
