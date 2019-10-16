/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Mike Ang
 *   Igor Bukanov
 *   Yuh-Ruey Chen
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Mike McCabe
 *   Milen Nankov
 *   Norris Boyd
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.mozilla.javascript.Node.Scope;
import org.mozilla.javascript.Node.Symbol;
import org.mozilla.javascript.babylscript.TranslatedNameBindings;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.resources.client.ClientBundle.Source;

/**
 * This class implements the JavaScript parser.
 *
 * It is based on the C source files jsparse.c and jsparse.h
 * in the jsref package.
 *
 * @see TokenStream
 *
 * @author Mike McCabe
 * @author Brendan Eich
 */

public class ParserToJS extends ParserErrorReportingBase
{
    private static String BABYL_TEMP_PREFIX = "$$babyltmp";
    private static class JSRecompilerNodeFactory
    {
        JSScript createScript()
        {
            return new JSScript();
        }
        JSNode createNode() 
        {
            return new JSNode();
        }
        JSNode createNode(String str)
        {
            return new JSNode(str);
        }
        JSFunction createFunction(String lang, String name)
        {
            return new JSFunction(lang, name);
        }
        JSFunction createGetterSetterFunction(String lang, String name)
        {
            JSFunction f = new JSFunction(lang, name);
            f.isShowFunctionKeyword = false;
            return f;
        }
        JSNode createSimpleBinaryOperator(String operator, JSNode left, JSNode right)
        {
            return createNode()
                    .add("(")
                    .add("(").add(left).add(")")
                    .add(operator)
                    .add("(").add(right).add(")")
                    .add(")");
            
        }
        JSNode createSimplePreUnaryOperator(String operator, JSNode val)
        {
            return createNode()
                    .add("(")
                    .add(operator)
                    .add("(")
                    .add(val).add(")")
                    .add(")");
            
        }
        JSNode createSimplePostUnaryOperator(String operator, JSNode val)
        {
            // TODO: This might not be quite right for postfix increments and stuff, but maybe it is
            return createNode()
                    .add("(")
                    .add("(")
                    .add(val).add(")")
                    .add(operator)
                    .add(")");
            
        }
        JSNode createAssignment(String assignOp, JSNode left, JSNode right)
        {
            if (assignOp.equals("="))
            {
                if (left instanceof JSTranslationMapping)
                {
                    JSTranslationMapping trans = (JSTranslationMapping)left;
                    return createNode()
                            .add("(babyl.addTranslation(")
                            .add(trans.obj)
                            .add(",")
                            .add(trans.lang)
                            .add(",")
                            .add(trans.name)
                            .add(",")
                            .add(right)
                            .add("))");
                }
                else 
                {
                    return createNode()
                            .add(left)
                            .add(assignOp)
                            .add(right);
                }
            }
            else
                // TODO: This isn't quite correct because the type of the object may change, requiring a new
                // translation mapping e.g. a=2; a += 'hello';
                // TODO: This also isn't right because the result of the expression isn't a proper babylwrapped object
                return createNode()
                        .add("(")
                        .add(left)
                        .add(")")
                        .add(assignOp)
                        .add("(")
                        .add(right)
                        .add(")");
        }
        JSNode createLabel()
        {
            // This is just some strange thing that Rhino does that allows it to dive
            // into the parser to confirm whether a name refers to a label or not, and if so, it 
            // passes this special value back up out of the parser as confirmation that
            // the name read previously was indeed confirmed to be a label
            JSNode node = new JSNode();
            node.type = Token.LABEL;
            return node;
        }
        JSName createName(String lang, String name)
        {
            return new JSName(lang, name);
        }
        JSVariables createVariable(int declType)
        {
            return new JSVariables(declType);
        }
        JSNode createNumber(String str)
        {
            return new JSNode(str);
        }
        JSNode createSpecialConstant(String str)
        {
            return new JSNode(str);
        }
        String escapeJSString(String str)
        {
            return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                    .replace("\n", "\\\n").replace("\f", "\\\f").replace("\t", "\\\t").replace("\r", "\\\r");
        }
        JSNode createString(String str)
        {
            return new JSNode("'" + escapeJSString(str) + "'");
        }
        JSCatch createCatch(JSName name, JSNode expr, JSNode block)
        {
            return new JSCatch(name, expr, block);
        }
        JSNode createError(String str)
        {
            return new JSNode("error");
        }
        JSScope createBlock()
        {
            return new JSScope(); 
        }
        JSTranslationMapping createTranslationMapping(JSNode obj, JSNode lang, JSNode name)
        {
            return new JSTranslationMapping(obj, lang, name);
        }
    }
    private static class JSNodePair
    {
        JSNodePair() {}
        JSNodePair(JSNode node) { this.node = node; }
        JSNode node = null;
        String str = "";
    }
    private static class JSNode
    {
        JSNode()
        {
            data.add(new JSNodePair());
        }
        JSNode(String str) 
        {
            JSNodePair pair = new JSNodePair();
            pair.str = str;
            data.add(pair);
        }
        ArrayList<JSNodePair> data = new ArrayList<JSNodePair>();
        public String toString() 
        { 
            String str = ""; 
            for (JSNodePair pair: data) 
                str += (pair.node != null ? pair.node.toString() + pair.str : pair.str); 
            return str;
        }
        int type;
        public int getType() { return type; }
        public JSNode add(String str)
        {
            data.get(data.size() -1).str += str;
            return this;
        }
        public JSNode add(JSNode node)
        {
            data.add(new JSNodePair(node));
            return this;
        }
        public JSNode addCondition(JSNode node)
        {
            data.add(new JSNodePair(node));
            return this;
        }
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            for (JSNodePair pair: data)
            {
                if (pair.node != null)
                    pair.node.fillInNameScope(currentScope);
            }
        }
        public void calculateVariableScope(ArrayList<JSScope> currentScope)
        {
            for (JSNodePair pair: data)
            {
                if (pair.node != null)
                    pair.node.calculateVariableScope(currentScope);
            }
        }
    }
    private static class JSTemporary extends JSNode
    {
        int idx;
        JSTemporary(int idx)
        {
            this.idx = idx;
        }
        public String toString() 
        {
            // TODO: Change the prefix so that there are no collisions
            // with variables already used in the code
            return BABYL_TEMP_PREFIX + idx;
        }
    }
    private static class JSName extends JSNode
    {
        boolean isGlobal = true;
        String lang;
        String name;
        JSName(String lang, String str)
        {
            super();
            this.lang = lang;
            this.name = str;
        }
        public String toString() 
        {
            if (isGlobal)
                return "babylroot[babyllookup(babylroot,'" + lang + "','" + name + "')]" + super.toString();
            else
                // Non-globals can't be translated
                return name + super.toString();
        }
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            // go through the scope chain to see if it's a locally defined variable
            boolean isNotGlobal = false;
            for (int n = currentScope.size() - 1; n > 0; n--)
            {
                if (currentScope.get(n).symbols.contains(name))
                {
                    isNotGlobal = true;
                    break;
                }
            }
            isGlobal = !isNotGlobal;

            // Hack for handling the arguments variable
            if ("en".equals(lang) && "arguments".equals(name))
                isGlobal = false;

            super.fillInNameScope(currentScope);
        }
        public JSName copy()
        {
            return new JSName(lang, name);
    }
    }
    private static class JSTranslationMapping extends JSNode
    {
        JSNode obj;
        JSNode lang;
        JSNode name;
        JSTranslationMapping(JSNode obj, JSNode lang, JSNode name)
        {
            super();
            this.obj = obj;
            this.lang = lang;
            this.name = name;
        }
        public String toString() 
        {
            return "(babyl.getTranslation(" + obj + "," + lang + "," + name + "))";
        }
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            obj.fillInNameScope(currentScope);
            lang.fillInNameScope(currentScope);
            name.fillInNameScope(currentScope);
            super.fillInNameScope(currentScope);
        }
        public void calculateVariableScope(ArrayList<JSScope> currentScope)
        {
            obj.calculateVariableScope(currentScope);
            lang.calculateVariableScope(currentScope);
            name.calculateVariableScope(currentScope);
            super.calculateVariableScope(currentScope);
        }
    }
    private static class JSVariables extends JSNode
    {
        boolean isGlobal = true;
        int declType;  // holds variable definitions of type Token.LET, Token.VAR, or Token.CONST
        String declString;
        ArrayList<JSName> names = new ArrayList<JSName>();
        ArrayList<JSNode> assigns = new ArrayList<JSNode>();
        public void addVar(JSName name, JSNode assign)
        {
            names.add(name);
            assigns.add(assign);
        }
        JSVariables(int declType)
        {
            this.declType = declType;
            switch (declType)
            {
            case Token.LET:
                declString = "let ";
                break;
            case Token.CONST:
                declString = "const ";
                break;
            case Token.VAR:
                declString = "var ";
                break;
            default:
                throw new RuntimeException("Unknown variable type");
            }
        }
        public String toString() 
        {
            // Only handle global variables for now
            String str = "";
            if (declType != Token.VAR || !isGlobal)
                str += declString;
            for (int n = 0; n < names.size(); n++)
            {
                if (n != 0) str += ",";
                str += names.get(n).toString();
                if (assigns.get(n) != null)
                    str += "=" + assigns.get(n).toString();
            }
            return str + super.toString();
        }
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            for (JSName s: names)
            {
                s.fillInNameScope(currentScope);
            }
            for (JSNode s: assigns)
            {
                if (s != null)
                    s.fillInNameScope(currentScope);
            }
        }
        public void calculateVariableScope(ArrayList<JSScope> currentScope)
        {
            for (JSName s: names)
            {
                String name = s.name;
                if (declType != Token.VAR)
                {
                    // TODO: Put this in the current scope or does it
                    // actually create its own scope?
                    currentScope.get(currentScope.size()-1).addNameToScope(name);
                    isGlobal = false;
                }
                else
                {
                    // Traverse upwards until we get to a function or the
                    // top level
                    for (int n = currentScope.size() -1; n >= 0; n--)
                    {
                        if (n == 0 || currentScope.get(n) instanceof JSFunction)
                        {
                            currentScope.get(n).addNameToScope(name);
                            isGlobal = (n == 0); 
                            break;
                        }
                    }
                }
            }
            for (JSNode s: assigns)
            {
                if (s != null)
                    s.calculateVariableScope(currentScope);
            }
            super.calculateVariableScope(currentScope);
        }
    }

    private static class JSCatch extends JSScope
    {
        JSName name;
        JSNode expr;
        JSNode block;
        
        JSCatch(JSName name, JSNode expr, JSNode block)
        {
            this.name = name;
            this.expr = expr;
            this.block = block;
        }
        public String toString() 
        {
            String str = "";
            str += "catch (" + name;
            if (expr != null)
                str += " if " + expr;
            str += ") {\n";
            str += block;
            str += "}\n";
            return str + super.toString();
        }
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            currentScope.add(this);
            name.fillInNameScope(currentScope);
            if (expr != null)
                expr.fillInNameScope(currentScope);
            block.fillInNameScope(currentScope);
            super.fillInNameScope(currentScope);
            currentScope.remove(currentScope.size()-1);
        }
        public void calculateVariableScope(ArrayList<JSScope> currentScope)
        {
            currentScope.add(this);
            addNameToScope(name.name);
            if (expr != null)
                expr.calculateVariableScope(currentScope);
            block.calculateVariableScope(currentScope);
            super.calculateVariableScope(currentScope);
            currentScope.remove(currentScope.size()-1);
        }
    }

    private static class JSScope extends JSNode
    {
        // This symbol list is mainly used for tracking arguments for
        // functions (it seems unreliable for other stuff)
        ArrayList<Symbol> symbolList = new ArrayList<Symbol>();
        
        // This symbolTable and parentScope stuff doesn't seem to be reliably
        // maintained, so we'll ignore these things and just manually calculate
        // things from the post-parsed results
        JSScope parent = null;
        HashMap<String, Symbol> symbolTable = new HashMap<String, Symbol>();
        public Symbol getSymbol(String name)
        {
            return symbolTable.get(name);
        }
        public void putSymbol(String name, Symbol symbol) 
        {
//            symbol.containingTable = this;
            symbolTable.put(name, symbol);
            symbolList.add(symbol);
        }
        public JSScope getDefiningScope(String name) {
            if (symbolTable.containsKey(name)) return this;
            if (parent != null) return parent.getDefiningScope(name);
            return null;
        }
        public void setParent(JSScope parent) {
            this.parent = parent;
        }
        public JSScope getParentScope() {
            return parent;
        }
        
        HashSet<String> symbols = new HashSet<String>();
        public void addNameToScope(String name)
        {
            symbols.add(name);
        }
        
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            currentScope.add(this);
            super.fillInNameScope(currentScope);
            currentScope.remove(currentScope.size()-1);
        }
        public void calculateVariableScope(ArrayList<JSScope> currentScope)
        {
            currentScope.add(this);
            super.calculateVariableScope(currentScope);
            currentScope.remove(currentScope.size()-1);
        }
    }
    private static class JSScript extends JSScope
    {
        public void setSourceName(String sourceURI) {}
        public void setBaseLineno(int baseLineno) {}
        public void setEncodedSourceBounds(int sourceStartOffset,
                int sourceEndOffset) {}
        public String getNextTempName() {
            tempNum++;
            return "$" + tempNum;
        }
        private int tempNum = 0;
        private ArrayList<JSFunction> functions = new ArrayList<JSFunction>();
        private ArrayList<String> regexps = new ArrayList<String>();
        public int addFunction(JSFunction fnNode) {
            functions.add(fnNode);
            return functions.size() - 1;
        }
        public JSFunction getFunctionNode(int fnIndex) {
            return functions.get(fnIndex);
        }
        public int addRegexp(String string, String flags) {
            if (string == null) Kit.codeBug();
            regexps.add(string);
            regexps.add(flags);
            return regexps.size() / 2 - 1;
        }
        
        // Below is Babylscript-specific stuff for handling
        // temporaries (the above stuff is from the original
        // parser code, and might not even be needed)
        int tempCount = 0;
        HashSet<Integer> reservedTemps = new HashSet<Integer>();
        public JSTemporary getTemp()
        {
            for (int n = 0;; n++)
            {
                if (!reservedTemps.contains(n))
                {
                    if (n+1 > tempCount)
                        tempCount = n+1;
                    return new JSTemporary(n);
                }
            }
        }
        public void releaseTemp(JSTemporary temp)
        {
            reservedTemps.remove(temp.idx);
        }
        public String toString()
        {
            String str = "";
            for (int n = 0; n < tempCount; n++)
                str += "var " + BABYL_TEMP_PREFIX + n + ";\n";
            str += super.toString();
            return str;
        }
    }
    private static class JSFunction extends JSScript
    {
        boolean isGlobal = true;
        boolean isShowFunctionKeyword = true;
        JSName name = null;
        public JSFunction(String lang, String name) {
            super();
            this.name = new JSName(lang, name);
        }
        boolean itsIgnoreDynamicScope = false;
        private int syntheticType;
        private JSNode body;
        public String getFunctionName() { if (name != null) return name.name; return null; }
        public void setEndLineno(int lineno) { }
        public JSNode init(int functionIndex, JSNode body, int syntheticType) {
            this.body = body;
            this.syntheticType = syntheticType;
            return this;
        }
        public String toString() 
        {
            String str = "";
            if (syntheticType == FunctionNode.FUNCTION_STATEMENT || syntheticType == FunctionNode.FUNCTION_EXPRESSION_STATEMENT)
                str += (isGlobal ? "" : "var ") + name + "=";
            if (isShowFunctionKeyword) str += "function ";
            str += "(";
            boolean first = true;
            for (Symbol s: symbolList)
            {
                if (s.declType == Token.LP)
                {
                    if (!first) str += ",";
                    first = false;
                    str += s.name;
                }
            }
            str += ") {\n";
            for (int n = 0; n < tempCount; n++)
                str += "var " + BABYL_TEMP_PREFIX + n + ";\n";
            str += body.toString() + "}\n";
            return str;
        }
        public void fillInNameScope(ArrayList<JSScope> currentScope)
        {
            // Figure out the name definition if it's a function statement 
            if (name != null)
            {
                name.fillInNameScope(currentScope);
            }
            
            currentScope.add(this);
            body.fillInNameScope(currentScope);
            currentScope.remove(currentScope.size()-1);
            super.fillInNameScope(currentScope);
        }
        public void calculateVariableScope(ArrayList<JSScope> currentScope)
        {
            if (name != null)
            {
                if (syntheticType == FunctionNode.FUNCTION_STATEMENT)
                {
                    // TODO: assume that function statements follow VAR scoping rules
                    // Traverse upwards until we get to a function or the
                    // top level
                    for (int n = currentScope.size() -1; n >= 0; n--)
                    {
                        if (n == 0 || currentScope.get(n) instanceof JSFunction)
                        {
                            currentScope.get(n).addNameToScope(name.name);
                            isGlobal = (n == 0); 
                            break;
                        }
                    }
                }
            }
            // Add the function arguments to the known list of symbols
            for (Symbol s: symbolList)
            {
                if (s.declType == Token.LP)
                {
                    addNameToScope(s.name);
                }
            }
            
            // Recurse in
            currentScope.add(this);
            body.calculateVariableScope(currentScope);
            currentScope.remove(currentScope.size()-1);
            super.calculateVariableScope(currentScope);
        }
    }

    public static interface BabylscriptJSRuntimeLibrary extends ClientBundle
    {
        public static final BabylscriptJSRuntimeLibrary INSTANCE = GWT.create(BabylscriptJSRuntimeLibrary.class);
        
        @Source("babylscriptJSHeader.js")
        TextResource js();
    }
    private static String babylscriptJSHeader; 
    static {
            babylscriptJSHeader = BabylscriptJSRuntimeLibrary.INSTANCE.js().getText();
            
            String languageRemap = "";
            boolean isFirst = true;
            for (String lang: TranslatedNameBindings.EquivalentLanguageNames.keySet())
            {
                if (!isFirst) languageRemap += ",\n";
                isFirst = false;
                languageRemap += "\'" + lang + "\' : \'" + TranslatedNameBindings.EquivalentLanguageNames.get(lang)[0] + "\'";
            }
            String translations = createStandardLibraryTranslations();
            babylscriptJSHeader = babylscriptJSHeader.replace("$$LANG_REMAP$$", languageRemap);
            babylscriptJSHeader = babylscriptJSHeader.replace("$$TRANSLATIONS$$", translations);
    }
    
    public static String createTranslationsFor(String obj, String [] names)
    {
        String str = "";
        str += "obj = " + obj + ";\n";
        Set<String> langs= TranslatedNameBindings.langResourceMap.keySet();
        for (String defaultName : names)
        {
            for (String lang: langs)
            {
                str += "babyl.addTranslation(obj,'" + lang + "','" + TranslatedNameBindings.langResourceMap.get(lang).get(defaultName) + "','" + defaultName + "');\n";
            }
        }
        return str;
    }
    static String createStandardLibraryTranslations()
    {
        String str = "(function() {\n";
        str += "var obj;\n";
        str += createTranslationsFor("babylroot", TranslatedNameBindings.GlobalScopeNames);
        str += createTranslationsFor("Function.prototype", TranslatedNameBindings.BaseFunctionPrototypeNames);
        str += createTranslationsFor("Object.prototype", TranslatedNameBindings.ObjectPrototypeNames);
        str += createTranslationsFor("Error.prototype", TranslatedNameBindings.ErrorPrototypeNames);
        str += createTranslationsFor("Array", TranslatedNameBindings.ArrayConstructorNames);
        str += createTranslationsFor("Array.prototype", TranslatedNameBindings.ArrayPrototypeNames);
        str += createTranslationsFor("String", TranslatedNameBindings.StringConstructorNames);
        str += createTranslationsFor("String.prototype", TranslatedNameBindings.StringPrototypeNames);
        str += createTranslationsFor("Boolean.prototype", TranslatedNameBindings.BooleanPrototypeNames);
        str += createTranslationsFor("Number", TranslatedNameBindings.NumberConstructorNames);
        str += createTranslationsFor("Number.prototype", TranslatedNameBindings.NumberPrototypeNames);
        str += createTranslationsFor("Date", TranslatedNameBindings.DateConstructorNames);
        str += createTranslationsFor("Date.prototype", TranslatedNameBindings.DatePrototypeNames);
        str += createTranslationsFor("Math", TranslatedNameBindings.MathNames);
        //str += createTranslationsFor("Call.prototype", TranslatedNameBindings.CallPrototypeNames);
        //str += createTranslationsFor("Script.prototype", TranslatedNameBindings.ScriptPrototypeNames);
        //str += createTranslationsFor("Iterator.prototype", TranslatedNameBindings.IteratorPrototypeNames);
        str += createTranslationsFor("RegExp.prototype", TranslatedNameBindings.RegExpPrototypeNames);
        // regexp matches
        str += "})();\n";
        return str;
    }
    
    // TokenInformation flags : currentFlaggedToken stores them together
    // with token type
    final static int
        CLEAR_TI_MASK  = 0xFFFF,   // mask to clear token information bits
        TI_AFTER_EOL   = 1 << 16,  // first token of the source line
        TI_CHECK_LABEL = 1 << 17;  // indicates to check for label

    boolean calledByCompileFunction;

    private int currentFlaggedToken;
    private IRFactory nf;
    private JSRecompilerNodeFactory jsFactory = new JSRecompilerNodeFactory();

    private int nestingOfFunction;

    private Decompiler decompiler;
    private String encodedSource;

// The following are per function variables and should be saved/restored
// during function parsing.
// XXX Move to separated class?
    JSScript currentScriptOrFn;
    JSScope currentScope;
    private int nestingOfWith;
    private Map<String,Node> labelSet; // map of label names into nodes
    private ObjArray loopSet;
    private ObjArray loopAndSwitchSet;
    private int endFlags;
// end of per function variables
    
    public int getCurrentLineNumber() {
        return ts.getLineno();
    }

    public ParserToJS(CompilerEnvirons compilerEnv, ErrorReporter errorReporter)
    {
        this.compilerEnv = compilerEnv;
        this.errorReporter = errorReporter;
    }

    protected Decompiler createDecompiler(CompilerEnvirons compilerEnv)
    {
        return new Decompiler();
    }

    private String lastPeekedLanguageString()
    {
        return ts.getLastLanguageString();
    }

    private int peekToken()
        throws IOException
    {
        int tt = currentFlaggedToken;
        if (tt == Token.EOF) {
            tt = ts.getToken();
            if (tt == Token.EOL) {
                do {
                    tt = ts.getToken();
                } while (tt == Token.EOL);
                tt |= TI_AFTER_EOL;
            }
            currentFlaggedToken = tt;
        }
        return tt & CLEAR_TI_MASK;
    }

    private int peekFlaggedToken()
        throws IOException
    {
        peekToken();
        return currentFlaggedToken;
    }

    private void consumeToken()
    {
        currentFlaggedToken = Token.EOF;
    }

    private int nextToken()
        throws IOException
    {
        int tt = peekToken();
        consumeToken();
        return tt;
    }

    private int nextFlaggedToken()
        throws IOException
    {
        peekToken();
        int ttFlagged = currentFlaggedToken;
        consumeToken();
        return ttFlagged;
    }

    private boolean matchToken(int toMatch)
        throws IOException
    {
        int tt = peekToken();
        if (tt != toMatch) {
            return false;
        }
        consumeToken();
        return true;
    }

    private boolean matchEitherToken(int toMatch, int altMatch)
        throws IOException
    {
        int tt = peekToken();
        if (tt != toMatch && tt != altMatch) {
            return false;
        }
        consumeToken();
        return true;
    }

    private int peekTokenOrEOL()
        throws IOException
    {
        int tt = peekToken();
        // Check for last peeked token flags
        if ((currentFlaggedToken & TI_AFTER_EOL) != 0) {
            tt = Token.EOL;
        }
        return tt;
    }

    private void setCheckForLabel()
    {
        if ((currentFlaggedToken & CLEAR_TI_MASK) != Token.NAME)
            throw Kit.codeBug();
        currentFlaggedToken |= TI_CHECK_LABEL;
    }

    private void mustMatchToken(int toMatch, String messageId)
        throws IOException, ParserException
    {
        if (!matchToken(toMatch)) {
            reportError(messageId);
        }
    }

    private void mustHaveXML()
    {
        if (!compilerEnv.isXmlAvailable()) {
            reportError("msg.XML.not.available");
        }
    }

    public String getEncodedSource()
    {
        return encodedSource;
    }

    public boolean eof()
    {
        return ts.eof();
    }

    boolean insideFunction()
    {
        return nestingOfFunction != 0;
    }
    
    void pushScope(JSNode node) {
        JSScope scopeNode = (JSScope) node;
        if (scopeNode.getParentScope() != null) throw Kit.codeBug();
        scopeNode.setParent(currentScope);
        currentScope = scopeNode;
    }
    
    void popScope() {
        currentScope = currentScope.getParentScope();
    }

    private JSNode enterLoop(Node loopLabel, boolean doPushScope)
    {
//        Node loop = nf.createLoopNode(loopLabel, ts.getLineno());
//        if (loopSet == null) {
//            loopSet = new ObjArray();
//            if (loopAndSwitchSet == null) {
//                loopAndSwitchSet = new ObjArray();
//            }
//        }
//        loopSet.push(loop);
//        loopAndSwitchSet.push(loop);
        JSScope loop = jsFactory.createBlock();
        if (doPushScope) {
            pushScope(loop);
        }
        return loop;
    }

    private void exitLoop(boolean doPopScope)
    {
//        loopSet.pop();
//        loopAndSwitchSet.pop();
        if (doPopScope) {
            popScope();
        }
    }

    private Node enterSwitch(Node switchSelector, int lineno)
    {
        Node switchNode = nf.createSwitch(switchSelector, lineno);
        if (loopAndSwitchSet == null) {
            loopAndSwitchSet = new ObjArray();
        }
        loopAndSwitchSet.push(switchNode);
        return switchNode;
    }

    private void exitSwitch()
    {
        loopAndSwitchSet.pop();
    }

    /*
     * Build a parse tree from the given sourceString.
     *
     * @return an Object representing the parsed
     * program.  If the parse fails, null will be returned.  (The
     * parse failure will result in a call to the ErrorReporter from
     * CompilerEnvirons.)
     */
    public String parse(String sourceString,
                                String sourceURI, int lineno, boolean withHeaders)
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, sourceString, lineno, 
                compilerEnv.getLanguageMode(), compilerEnv.getCustomTokenizerConfig());
        try {
            return parse(withHeaders);
        } catch (IOException ex) {
            // Should never happen
            throw new IllegalStateException();
        }
    }

    private String parse(boolean withHeaders)
        throws IOException
    {
        this.decompiler = createDecompiler(compilerEnv);
        //this.nf = new IRFactory(this);
        currentScriptOrFn = jsFactory.createScript();
        currentScope = currentScriptOrFn;
        int sourceStartOffset = decompiler.getCurrentOffset();
        this.encodedSource = null;
        decompiler.addToken(Token.SCRIPT);

        this.currentFlaggedToken = Token.EOF;
        this.syntaxErrorCount = 0;

        int baseLineno = ts.getLineno();  // line number where source starts

        /* so we have something to add nodes to until
         * we've collected all the source */
        JSNode pn = new JSNode();
//        Node pn = nf.createLeaf(Token.BLOCK);

//        try {
            for (;;) {
                int tt = peekToken();

                if (tt <= Token.EOF) {
                    break;
                }

                JSNode n;
                if (tt == Token.FUNCTION) {
                    consumeToken();
                    try {
                        n = jsFactory.createNode()
                                .add(function(calledByCompileFunction
                                     ? FunctionNode.FUNCTION_EXPRESSION
                                     : FunctionNode.FUNCTION_STATEMENT))
                                .add(";\n");
                    } catch (ParserException e) {
                        break;
                    }
                } else {
                    n = jsstatement();
                }
                pn.add(n);
//                nf.addChildToBack(pn, n);
            }
//        } catch (StackOverflowError ex) {
//            String msg = ScriptRuntime.getMessage0(
//                "msg.too.deep.parser.recursion");
//            throw Context.reportRuntimeError(msg, sourceURI,
//                                             ts.getLineno(), null, 0);
//        }

        if (this.syntaxErrorCount != 0) {
            String msg = String.valueOf(this.syntaxErrorCount);
            msg = ScriptRuntime.getMessage1("msg.got.syntax.errors", msg);
            throw errorReporter.runtimeError(msg, sourceURI, baseLineno,
                                             null, 0);
        }

        currentScriptOrFn.setSourceName(sourceURI);
        currentScriptOrFn.setBaseLineno(baseLineno);
        currentScriptOrFn.setBaseLineno(ts.getLineno());

        int sourceEndOffset = decompiler.getCurrentOffset();
        currentScriptOrFn.setEncodedSourceBounds(sourceStartOffset,
                                                 sourceEndOffset);

// TODO: This is probably fine, but double-check
//        nf.initScript(currentScriptOrFn, pn);
        currentScriptOrFn.add(pn);

        if (compilerEnv.isGeneratingSource()) {
            encodedSource = decompiler.getEncodedSource();
        }
        this.decompiler = null; // It helps GC

        ArrayList<JSScope> scope = new ArrayList<JSScope>();
        currentScriptOrFn.calculateVariableScope(scope);
        assert scope.isEmpty();
        currentScriptOrFn.fillInNameScope(scope);
        
        return (withHeaders ? babylscriptJSHeader : "") + currentScriptOrFn.toString();
    }

    /*
     * The C version of this function takes an argument list,
     * which doesn't seem to be needed for tree generation...
     * it'd only be useful for checking argument hiding, which
     * I'm not doing anyway...
     */
    private JSNode parseFunctionBody()
        throws IOException
    {
        ++nestingOfFunction;
        JSNode pn = jsFactory.createBlock();
//        Node pn = nf.createBlock(ts.getLineno());
        try {
            bodyLoop: for (;;) {
                JSNode n;
                int tt = peekToken();
                switch (tt) {
                  case Token.ERROR:
                  case Token.EOF:
                  case Token.RC:
                    break bodyLoop;

                  case Token.FUNCTION:
                    consumeToken();
                    n = jsFactory.createNode()
                            .add(function(FunctionNode.FUNCTION_STATEMENT))
                            .add(";");
                    break;
                  default:
                    n = jsstatement();
                    break;
                }
                pn.add(n);
//                nf.addChildToBack(pn, n);
            }
        } catch (ParserException e) {
            // Ignore it
        } finally {
            --nestingOfFunction;
        }

        return pn;
    }

    private JSNode function(int functionType)
            throws IOException, ParserException
    {
        return function(functionType, false);
    }

    private JSNode function(int functionType, boolean isGetterSetter)
        throws IOException, ParserException
    {
        int syntheticType = functionType;
        int baseLineno = ts.getLineno();  // line number where source starts

        int functionSourceStart = decompiler.markFunctionStart(functionType);
        String name;
        String lang = null;
        JSNode memberExprNode = null;
        if (matchToken(Token.NAME)) {
            lang = ts.getLastLanguageString();
            name = ts.getString();
            decompiler.addName(name);
            if (!matchToken(Token.LP)) {
                if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                    // Extension to ECMA: if 'function <name>' does not follow
                    // by '(', assume <name> starts memberExpr
                    JSNode memberExprHead = jsFactory.createName(lang, name);
//                    JSNode memberExprHead = nf.createName(lang, name);
                    name = "";
                    memberExprNode = memberExprTail(false, memberExprHead);
                }
                mustMatchToken(Token.LP, "msg.no.paren.parms");
            }
        } else if (matchToken(Token.LP)) {
            // Anonymous function
            name = "";
        } else {
            name = "";
            if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                // Note that memberExpr can not start with '(' like
                // in function (1+2).toString(), because 'function (' already
                // processed as anonymous function
                memberExprNode = memberExpr(false);
            }
            mustMatchToken(Token.LP, "msg.no.paren.parms");
        }

        if (memberExprNode != null) {
            syntheticType = FunctionNode.FUNCTION_EXPRESSION;
        } 
        
        if (syntheticType != FunctionNode.FUNCTION_EXPRESSION && 
            name.length() > 0)
        {
            // Function statements define a symbol in the enclosing scope
            defineSymbol(Token.FUNCTION, false, name);
        }

        boolean nested = insideFunction();

        JSFunction fnNode = isGetterSetter ? jsFactory.createGetterSetterFunction(lang, name) : jsFactory.createFunction(lang, name);
        if (nested || nestingOfWith > 0) {
            // 1. Nested functions are not affected by the dynamic scope flag
            // as dynamic scope is already a parent of their scope.
            // 2. Functions defined under the with statement also immune to
            // this setup, in which case dynamic scope is ignored in favor
            // of with object.
            fnNode.itsIgnoreDynamicScope = true;
        }
        int functionIndex = currentScriptOrFn.addFunction(fnNode);

        int functionSourceEnd;

        JSScript savedScriptOrFn = currentScriptOrFn;
        currentScriptOrFn = fnNode;
        JSScope savedCurrentScope = currentScope;
        currentScope = fnNode;
        int savedNestingOfWith = nestingOfWith;
        nestingOfWith = 0;
        Map<String,Node> savedLabelSet = labelSet;
        labelSet = null;
        ObjArray savedLoopSet = loopSet;
        loopSet = null;
        ObjArray savedLoopAndSwitchSet = loopAndSwitchSet;
        loopAndSwitchSet = null;
        int savedFunctionEndFlags = endFlags;
        endFlags = 0;

        JSNode destructuring = null;
        JSNode body;
        try {
            decompiler.addToken(Token.LP);
            if (!matchToken(Token.RP)) {
                boolean first = true;
                do {
                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    first = false;
                    int tt = peekToken();
// TODO: Fill this in                    
//                    if (tt == Token.LB || tt == Token.LC) {
//                        // Destructuring assignment for parameters: add a 
//                        // dummy parameter name, and add a statement to the
//                        // body to initialize variables from the destructuring
//                        // assignment
//                        if (destructuring == null) {
//                            destructuring = new Node(Token.COMMA);
//                        }
//                        String parmName = currentScriptOrFn.getNextTempName();
//                        defineSymbol(Token.LP, false, parmName);
//                        destructuring.addChildToBack(
//                            nf.createDestructuringAssignment(Token.VAR,
//                                primaryExpr(), nf.createName(ScriptRuntime.TOFILL, parmName)));
//                    } else {
                        mustMatchToken(Token.NAME, "msg.no.parm");
                        String s = ts.getString();
                        defineSymbol(Token.LP, false, s);
                        decompiler.addName(s);
//                    }
                } while (matchEitherToken(Token.COMMA, Token.SEMI));

                mustMatchToken(Token.RP, "msg.no.paren.after.parms");
            }
            decompiler.addToken(Token.RP);

            mustMatchToken(Token.LC, "msg.no.brace.body");
            decompiler.addEOL(Token.LC);
            body = parseFunctionBody();
// TODO: Fill this in            
//            if (destructuring != null) {
//                body.addChildToFront(
//                    new Node(Token.EXPR_VOID, destructuring, ts.getLineno()));
//            }
            mustMatchToken(Token.RC, "msg.no.brace.after.body");

// TODO: Fill this in            
//            if (compilerEnv.isStrictMode() && !body.hasConsistentReturnUsage())
//            {
//              String msg = name.length() > 0 ? "msg.no.return.value"
//                                             : "msg.anon.no.return.value";
//              addStrictWarning(msg, name);
//            }
            
            if (syntheticType == FunctionNode.FUNCTION_EXPRESSION &&
                name.length() > 0 && currentScope.getSymbol(name) == null) 
            {
                // Function expressions define a name only in the body of the 
                // function, and only if not hidden by a parameter name
                defineSymbol(Token.FUNCTION, false, name);
            }
            
            decompiler.addToken(Token.RC);
            functionSourceEnd = decompiler.markFunctionEnd(functionSourceStart);
            if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
                // Add EOL only if function is not part of expression
                // since it gets SEMI + EOL from Statement in that case
                decompiler.addToken(Token.EOL);
            }
        }
        finally {
            endFlags = savedFunctionEndFlags;
            loopAndSwitchSet = savedLoopAndSwitchSet;
            loopSet = savedLoopSet;
            labelSet = savedLabelSet;
            nestingOfWith = savedNestingOfWith;
            currentScriptOrFn = savedScriptOrFn;
            currentScope = savedCurrentScope;
        }

        fnNode.setEncodedSourceBounds(functionSourceStart, functionSourceEnd);
        fnNode.setSourceName(sourceURI);
        fnNode.setBaseLineno(baseLineno);
        fnNode.setEndLineno(ts.getLineno());

        JSNode pn = fnNode.init(functionIndex, body, syntheticType);
//        JSNode pn = nf.initFunction(fnNode, functionIndex, body, syntheticType);
        if (memberExprNode != null) {
            pn = jsFactory.createAssignment(tokenToString(Token.ASSIGN), memberExprNode, pn);
//            pn = nf.createAssignment(Token.ASSIGN, memberExprNode, pn);
// TODO: Fill this in later
//            if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
//                // XXX check JScript behavior: should it be createExprStatement?
//                pn = nf.createExprStatementNoReturn(pn, baseLineno);
//            }
        }
        return pn;
    }

    private JSNode statements(JSNode scope)
        throws IOException
    {
        JSNode pn = scope != null ? scope : jsFactory.createBlock();
//        JSNode pn = scope != null ? scope : nf.createBlock(ts.getLineno());

        int tt;
        while ((tt = peekToken()) > Token.EOF && tt != Token.RC) {
            pn.add(jsstatement());
//            nf.addChildToBack(pn, statement());
        }

        return pn;
    }

    private JSNode condition()
        throws IOException, ParserException
    {
        mustMatchToken(Token.LP, "msg.no.paren.cond");
        decompiler.addToken(Token.LP);
        JSNode pn = jsexpr(false);
        mustMatchToken(Token.RP, "msg.no.paren.after.cond");
        decompiler.addToken(Token.RP);

        // Report strict warning on code like "if (a = 7) ...". Suppress the
        // warning if the condition is parenthesized, like "if ((a = 7)) ...".
// TODO: fill this in
//        if (pn.getProp(Node.PARENTHESIZED_PROP) == null &&
//            (pn.getType() == Token.SETNAME || pn.getType() == Token.SETPROP ||
//             pn.getType() == Token.SETELEM))
//        {
//            addStrictWarning("msg.equal.as.assign", "");
//        }
        return pn;
    }

    // match a NAME; return null if no match.
    private JSNode matchJumpLabelName()
        throws IOException, ParserException
    {
        JSNode label = null;

        int tt = peekTokenOrEOL();
        if (tt == Token.NAME) {
            consumeToken();
            String name = ts.getString();
            decompiler.addName(name);
            return new JSNode(name);
// TODO: Fill this in            
//            if (labelSet != null) {
//                label = labelSet.get(name);
//            }
//            if (label == null) {
//                reportError("msg.undef.label");
//            }
        }

        return label;
    }

//    private Node statement()
//        throws IOException
//    {
//        try {
//            Node pn = null;  // statementHelper(null);
//            if (pn != null) {
//                if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
//                    addStrictWarning("msg.no.side.effects", "");
//                return pn;
//            }
//        } catch (ParserException e) { }
//
//        // skip to end of statement
//        int lineno = ts.getLineno();
//        guessingStatementEnd: for (;;) {
//            int tt = peekTokenOrEOL();
//            consumeToken();
//            switch (tt) {
//              case Token.ERROR:
//              case Token.EOF:
//              case Token.EOL:
//              case Token.SEMI:
//                break guessingStatementEnd;
//            }
//        }
//        return nf.createExprStatement(nf.createName(ScriptRuntime.TOFILL, "error"), lineno);
//    }
//
    private JSNode jsstatement()
            throws IOException
        {
            try {
                JSNode pn = statementHelper(null);
                if (pn != null) {
// TODO: Fix this up                    
//                    if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
//                        addStrictWarning("msg.no.side.effects", "");
                    return pn;
                }
            } catch (ParserException e) { }

            // skip to end of statement
            int lineno = ts.getLineno();
            guessingStatementEnd: for (;;) {
                int tt = peekTokenOrEOL();
                consumeToken();
                switch (tt) {
                  case Token.ERROR:
                  case Token.EOF:
                  case Token.EOL:
                  case Token.SEMI:
                    break guessingStatementEnd;
                }
            }
            return null;
// TODO: Fill this in            
//            return nf.createExprStatement(nf.createName(ScriptRuntime.TOFILL, "error"), lineno);
        }

    private JSNode statementHelper(Node statementLabel)
        throws IOException, ParserException
    {
        JSNode pn = null;
        int tt = peekToken();

        switch (tt) {
          case Token.IF: {
            consumeToken();

            decompiler.addToken(Token.IF);
            int lineno = ts.getLineno();
            JSNode cond = condition();
            decompiler.addEOL(Token.LC);
            JSNode ifTrue = jsstatement();
            JSNode ifFalse = null;
            if (matchToken(Token.ELSE)) {
                decompiler.addToken(Token.RC);
                decompiler.addToken(Token.ELSE);
                decompiler.addEOL(Token.LC);
                ifFalse = jsstatement();
            }
            decompiler.addEOL(Token.RC);
            pn = jsFactory.createNode()
                    .add("if(")
                    .addCondition(cond)
                    .add(") {\n")
                    .add(ifTrue)
                    .add("}");
            if (ifFalse != null)
                pn.add("else{\n")
                    .add(ifFalse)
                    .add("}\n");
//            pn = nf.createIf(cond, ifTrue, ifFalse, lineno);
            return pn;
          }

          case Token.SWITCH: {
            consumeToken();

            decompiler.addToken(Token.SWITCH);
            int lineno = ts.getLineno();
            mustMatchToken(Token.LP, "msg.no.paren.switch");
            decompiler.addToken(Token.LP);
            pn = jsFactory.createNode()
                    .add("switch(")
                    .add(jsexpr(false))
                    .add(") {\n");
//            pn = enterSwitch(expr(false), lineno);
            try {
                mustMatchToken(Token.RP, "msg.no.paren.after.switch");
                decompiler.addToken(Token.RP);
                mustMatchToken(Token.LC, "msg.no.brace.switch");
                decompiler.addEOL(Token.LC);

                boolean hasDefault = false;
                switchLoop: for (;;) {
                    tt = nextToken();
                    JSNode caseExpression;
                    switch (tt) {
                      case Token.RC:
                        break switchLoop;

                      case Token.CASE:
                        decompiler.addToken(Token.CASE);
                        caseExpression = jsFactory.createNode("case ")
                                .add(jsexpr(false))
                                .add(":\n");
                        mustMatchToken(Token.COLON, "msg.no.colon.case");
                        decompiler.addEOL(Token.COLON);
                        break;

                      case Token.DEFAULT:
                        if (hasDefault) {
                            reportError("msg.double.switch.default");
                        }
                        decompiler.addToken(Token.DEFAULT);
                        hasDefault = true;
                        caseExpression = null;
                        mustMatchToken(Token.COLON, "msg.no.colon.case");
                        decompiler.addEOL(Token.COLON);
                        break;

                      default:
                        reportError("msg.bad.switch");
                        break switchLoop;
                    }

                    JSNode block = jsFactory.createNode();
//                    Node block = nf.createLeaf(Token.BLOCK);
                    while ((tt = peekToken()) != Token.RC
                           && tt != Token.CASE
                           && tt != Token.DEFAULT
                           && tt != Token.EOF)
                    {
                        block.add(jsstatement());
//                        nf.addChildToBack(block, statement());
                    }

                    // caseExpression == null => add default label
                    if (caseExpression == null)
                        pn.add("default:\n");
                    else
                        pn.add(caseExpression);
                    pn.add(block);
//                    nf.addSwitchCase(pn, caseExpression, block);
                }
                decompiler.addEOL(Token.RC);
                pn.add("}\n");
//                nf.closeSwitch(pn);
            } finally {
//                exitSwitch();
            }
            return pn;
          }

          case Token.WHILE: {
            consumeToken();
            decompiler.addToken(Token.WHILE);

            JSNode loop = enterLoop(statementLabel, true);
            try {
                JSNode cond = condition();
                decompiler.addEOL(Token.LC);
                JSNode body = jsstatement();
                decompiler.addEOL(Token.RC);
                pn = jsFactory.createNode()
                        .add("while(")
                        .addCondition(cond)
                        .add(") {\n")
                        .add(body)
                        .add("}\n");
//                pn = nf.createWhile(loop, cond, body);
            } finally {
                exitLoop(true);
            }
            return pn;
          }

          case Token.DO: {
            consumeToken();
            decompiler.addToken(Token.DO);
            decompiler.addEOL(Token.LC);

            JSNode loop = enterLoop(statementLabel, true);
            try {
                JSNode body = jsstatement();
                decompiler.addToken(Token.RC);
                mustMatchToken(Token.WHILE, "msg.no.while.do");
                decompiler.addToken(Token.WHILE);
                JSNode cond = condition();
                pn = jsFactory.createNode()
                        .add("do {\n")
                        .add(body)
                        .add("} while(")
                        .addCondition(cond)
                        .add(");\n");
//                pn = nf.createDoWhile(loop, body, cond);
            } finally {
                exitLoop(true);
            }
            // Always auto-insert semicolon to follow SpiderMonkey:
            // It is required by ECMAScript but is ignored by the rest of
            // world, see bug 238945
            matchToken(Token.SEMI);
            decompiler.addEOL(Token.SEMI);
            return pn;
          }

          case Token.FOR: {
            consumeToken();
            boolean isForEach = false;
            decompiler.addToken(Token.FOR);
            String lang = lastPeekedLanguageString();

            JSNode loop = enterLoop(statementLabel, true);
            try {
                JSNode init;  // Node init is also foo in 'foo in object'
                JSNode cond;  // Node cond is also object in 'foo in object'
                JSNode incr = null;
                JSNode body;
                int declType = -1;

                // See if this is a for each () instead of just a for ()
                if (matchToken(Token.NAME)) {
                    decompiler.addName(ts.getString());
                    if (ts.getString().equals("each")) {
                        isForEach = true;
                    } else {
                        reportError("msg.no.paren.for");
                    }
                }

                mustMatchToken(Token.LP, "msg.no.paren.for");
                decompiler.addToken(Token.LP);
                tt = peekToken();
                if (tt == Token.SEMI) {
                    init = jsFactory.createNode();
                } else {
                    if (tt == Token.VAR || tt == Token.LET) {
                        // set init to a var list or initial
                        consumeToken();    // consume the token
                        decompiler.addToken(tt);
                        init = variables(true, tt);
                        declType = tt;
                    }
                    else {
                        init = jsexpr(true);
                    }
                }

                if (matchToken(Token.IN)) {
                    decompiler.addToken(Token.IN);
                    // 'cond' is the object over which we're iterating
                    cond = jsexpr(false);
                } else {  // ordinary for loop
                    mustMatchToken(Token.SEMI, "msg.no.semi.for");
                    decompiler.addToken(Token.SEMI);
                    if (peekToken() == Token.SEMI) {
                        // no loop condition
                        cond = jsFactory.createNode();
                    } else {
                        cond = jsexpr(false);
                    }

                    mustMatchToken(Token.SEMI, "msg.no.semi.for.cond");
                    decompiler.addToken(Token.SEMI);
                    if (peekToken() == Token.RP) {
                        incr = jsFactory.createNode();
                    } else {
                        incr = jsexpr(false);
                    }
                }

                mustMatchToken(Token.RP, "msg.no.paren.for.ctrl");
                decompiler.addToken(Token.RP);
                decompiler.addEOL(Token.LC);
                body = jsstatement();
                decompiler.addEOL(Token.RC);

                if (incr == null) {
                    // cond could be null if 'in obj' got eaten
                    // by the init node.
                    JSTemporary temp = currentScriptOrFn.getTemp();
                    pn = jsFactory.createNode()
                        .add("for (")
                        .add(init)
                        .add(" in (")
                        .add(temp)
                        .add(" = (")
                        .add(cond)
                        .add("))) {\n");
                    // Check to see if we can translate the names
                    // being iterated over (we'll only handle the
                    // common cases for now--so no using 
                    // properties as iterators etc.)
                    JSName iterated = null; 
                    if (init instanceof JSVariables)
                        iterated = ((JSVariables)init).names.get(0).copy();
                    else if (init instanceof JSName)
                        iterated = ((JSName)init).copy();
                    if (iterated != null)
                    {
                        pn.add(jsFactory.createNode()
                                .add(iterated)
                                .add(" = ")
                                .add("babylreverselookup(")
                                .add(temp)
                                .add(",'")
                                .add(lang)
                                .add("',")
                                .add(iterated)
                                .add(");\n"));
                    }
                    pn.add(body)
                        .add("}\n");
                    currentScriptOrFn.releaseTemp(temp);
//                    pn = nf.createForIn(declType, lang, loop, init, cond, body,
//                                        isForEach);
                } else {
                    pn = jsFactory.createNode()
                            .add("for (")
                            .add(init)
                            .add(";");
                    if (cond.toString().length() != 0)
                        pn.addCondition(cond);
                    pn.add(";")
                            .add(incr)
                            .add(") {\n")
                            .add(body)
                            .add("}\n");
//                    pn = nf.createFor(loop, init, cond, incr, body);
                }
            } finally {
                exitLoop(true);
            }
            return pn;
          }

          case Token.TRY: {
            consumeToken();
            int lineno = ts.getLineno();

            JSNode tryblock;
            JSNode catchblocks = null;
            JSNode finallyblock = null;

            decompiler.addToken(Token.TRY);
            if (peekToken() != Token.LC) {
                reportError("msg.no.brace.try");
            }
            decompiler.addEOL(Token.LC);
            tryblock = jsstatement();
            decompiler.addEOL(Token.RC);

            catchblocks = jsFactory.createNode();
//            catchblocks = nf.createLeaf(Token.BLOCK);

            boolean sawDefaultCatch = false;
            int peek = peekToken();
            if (peek == Token.CATCH) {
                while (matchToken(Token.CATCH)) {
                    if (sawDefaultCatch) {
                        reportError("msg.catch.unreachable");
                    }
                    decompiler.addToken(Token.CATCH);
                    mustMatchToken(Token.LP, "msg.no.paren.catch");
                    decompiler.addToken(Token.LP);

                    mustMatchToken(Token.NAME, "msg.bad.catchcond");
                    String lang = ts.getLastLanguageString();
                    String varName = ts.getString();
                    decompiler.addName(varName);

                    JSNode catchCond = null;
                    if (matchToken(Token.IF)) {
                        decompiler.addToken(Token.IF);
                        catchCond = jsexpr(false);
                    } else {
                        sawDefaultCatch = true;
                    }

                    mustMatchToken(Token.RP, "msg.bad.catchcond");
                    decompiler.addToken(Token.RP);
                    mustMatchToken(Token.LC, "msg.no.brace.catchblock");
                    decompiler.addEOL(Token.LC);

                    catchblocks.add(
                            jsFactory.createCatch(jsFactory.createName(lang, varName), 
                                    catchCond, statements(null)));
//                    
//                    nf.addChildToBack(catchblocks,
//                        nf.createCatch(varName, catchCond,
//                                       statements(null),
//                                       ts.getLineno()));

                    mustMatchToken(Token.RC, "msg.no.brace.after.body");
                    decompiler.addEOL(Token.RC);
                }
            } else if (peek != Token.FINALLY) {
                mustMatchToken(Token.FINALLY, "msg.try.no.catchfinally");
            }

            if (matchToken(Token.FINALLY)) {
                decompiler.addToken(Token.FINALLY);
                decompiler.addEOL(Token.LC);
                finallyblock = jsstatement();
                decompiler.addEOL(Token.RC);
            }
            pn = jsFactory.createNode("try ")
                    .add(tryblock)
                    .add(catchblocks);
            if (finallyblock != null)
                pn.add("finally {\n")
                    .add(finallyblock)
                    .add("}\n");
// TODO: Rhino rewrites the scoping of variables here            
//            pn = nf.createTryCatchFinally(tryblock, catchblocks,
//                                          finallyblock, lineno);

            return pn;
          }

          case Token.THROW: {
            consumeToken();
            if (peekTokenOrEOL() == Token.EOL) {
                // ECMAScript does not allow new lines before throw expression,
                // see bug 256617
                reportError("msg.bad.throw.eol");
            }

            int lineno = ts.getLineno();
            decompiler.addToken(Token.THROW);
            pn = jsFactory.createNode("throw ").add(jsexpr(false));
//            pn = nf.createThrow(expr(false), lineno);
            break;
          }

          case Token.BREAK: {
            consumeToken();
            int lineno = ts.getLineno();

            decompiler.addToken(Token.BREAK);

            // matchJumpLabelName only matches if there is one
            JSNode breakStatement = matchJumpLabelName();
// TODO: Fill this in            
//            if (breakStatement == null) {
//                if (loopAndSwitchSet == null || loopAndSwitchSet.size() == 0) {
//                    reportError("msg.bad.break");
//                    return null;
//                }
//                breakStatement = (Node)loopAndSwitchSet.peek();
//            }
            pn = jsFactory.createNode("break");
            if (breakStatement != null)
                pn.add(" ").add(breakStatement);
//            pn = nf.createBreak(breakStatement, lineno);
            break;
          }

          case Token.CONTINUE: {
            consumeToken();
            int lineno = ts.getLineno();

            decompiler.addToken(Token.CONTINUE);

            Node loop;
            // matchJumpLabelName only matches if there is one
            JSNode label = matchJumpLabelName();
// TODO: Fill this in            
//            if (label == null) {
//                if (loopSet == null || loopSet.size() == 0) {
//                    reportError("msg.continue.outside");
//                    return null;
//                }
//                loop = (Node)loopSet.peek();
//            } else {
//                loop = nf.getLabelLoop(label);
//                if (loop == null) {
//                    reportError("msg.continue.nonloop");
//                    return null;
//                }
//            }
            pn = jsFactory.createNode("continue");
            if (label != null) pn.add(" ").add(label);
//            pn = nf.createContinue(loop, lineno);
            break;
          }

//          case Token.WITH: {
//            consumeToken();
//
//            decompiler.addToken(Token.WITH);
//            int lineno = ts.getLineno();
//            mustMatchToken(Token.LP, "msg.no.paren.with");
//            decompiler.addToken(Token.LP);
//            Node obj = expr(false);
//            mustMatchToken(Token.RP, "msg.no.paren.after.with");
//            decompiler.addToken(Token.RP);
//            decompiler.addEOL(Token.LC);
//
//            ++nestingOfWith;
//            Node body;
//            try {
//                body = statement();
//            } finally {
//                --nestingOfWith;
//            }
//
//            decompiler.addEOL(Token.RC);
//
//            pn = nf.createWith(obj, body, lineno);
//            return pn;
//          }
//
// TODO: Fill this in          
//          case Token.CONST:
          case Token.VAR: {
            consumeToken();
            decompiler.addToken(tt);
            pn = variables(false, tt);
            break;
          }
          
//          case Token.LET: {
//            consumeToken();
//            decompiler.addToken(Token.LET);
//            if (peekToken() == Token.LP) {
//                return let(true);
//            } else {
//                pn = variables(false, tt);
//                if (peekToken() == Token.SEMI)
//                    break;
//                return pn;
//            }
//          }

          case Token.RETURN: 
          case Token.YIELD: {
            pn = returnOrYield(tt, false);
            break;
          }

          case Token.DEBUGGER:
            consumeToken();
            decompiler.addToken(Token.DEBUGGER);
            pn = jsFactory.createNode("debugger");
//            pn = nf.createDebugger(ts.getLineno());
            break;

          case Token.LC:
            consumeToken();
            if (statementLabel != null) {
                decompiler.addToken(Token.LC);
            }
            JSNode scope = jsFactory.createBlock();
            scope.add("{\n");
            //nf.createScopeNode(Token.BLOCK, ts.getLineno());
            pushScope(scope);
            try {
                statements(scope);
                mustMatchToken(Token.RC, "msg.no.brace.block");
                scope.add("}\n");
                if (statementLabel != null) {
                    decompiler.addEOL(Token.RC);
                }
                return scope;
            } finally {
                popScope();
            }

          case Token.ERROR:
            // Fall thru, to have a node for error recovery to work on
          case Token.SEMI:
            consumeToken();
            pn = jsFactory.createNode();
//            pn = nf.createLeaf(Token.EMPTY);
            return pn;

          case Token.FUNCTION: {
            consumeToken();
            pn = jsFactory.createNode()
                    .add(function(FunctionNode.FUNCTION_EXPRESSION_STATEMENT))
                    .add(";");
            return pn;
          }

//          case Token.DEFAULT :
//            consumeToken();
//            mustHaveXML();
//
//            decompiler.addToken(Token.DEFAULT);
//            int nsLine = ts.getLineno();
//
//            if (!(matchToken(Token.NAME)
//                  && ts.getString().equals("xml")))
//            {
//                reportError("msg.bad.namespace");
//            }
//            decompiler.addName(" xml");
//
//            if (!(matchToken(Token.NAME)
//                  && ts.getString().equals("namespace")))
//            {
//                reportError("msg.bad.namespace");
//            }
//            decompiler.addName(" namespace");
//
//            if (!matchToken(Token.ASSIGN)) {
//                reportError("msg.bad.namespace");
//            }
//            decompiler.addToken(Token.ASSIGN);
//
//            Node expr = expr(false);
//            pn = nf.createDefaultNamespace(expr, nsLine);
//            break;

          case Token.NAME: {
            int lineno = ts.getLineno();
            String name = ts.getString();
            setCheckForLabel();
            pn = jsexpr(false);
            if (pn.getType() != Token.LABEL) {
//                pn = nf.createExprStatement(pn, lineno);
            } else {
                // Parsed the label: push back token should be
                // colon that primaryExpr left untouched.
                if (peekToken() != Token.COLON) Kit.codeBug();
                consumeToken();
                // depend on decompiling lookahead to guess that that
                // last name was a label.
                decompiler.addName(name);
                decompiler.addEOL(Token.COLON);

// TODO: fill this in                
//                if (labelSet == null) {
//                    labelSet = new HashMap<String,Node>();
//                } else if (labelSet.containsKey(name)) {
//                    reportError("msg.dup.label");
//                }
//
//                boolean firstLabel;
//                if (statementLabel == null) {
//                    firstLabel = true;
//                    statementLabel = pn;
//                } else {
//                    // Discard multiple label nodes and use only
//                    // the first: it allows to simplify IRFactory
//                    firstLabel = false;
//                }
//                labelSet.put(name, statementLabel);
                try {
                    pn = statementHelper(statementLabel);
                } finally {
//                    labelSet.remove(name);
                }
                pn = jsFactory.createNode(name).add(":\n");
//                if (firstLabel) {
//                    pn = nf.createLabeledStatement(statementLabel, pn);
//                }
                return pn;
            }
            break;
          }

          default: {
            int lineno = ts.getLineno();
            pn = jsexpr(false);
// TODO: Fill this in            
//            pn = nf.createExprStatement(pn, lineno);
            break;
          }
        }

        int ttFlagged = peekFlaggedToken();
        switch (ttFlagged & CLEAR_TI_MASK) {
          case Token.SEMI:
            // Consume ';' as a part of expression
            consumeToken();
            break;
          case Token.ERROR:
          case Token.EOF:
          case Token.RC:
            // Autoinsert ;
            break;
          default:
            if ((ttFlagged & TI_AFTER_EOL) == 0) {
                // Report error if no EOL or autoinsert ; otherwise
                reportError("msg.no.semi.stmt");
            }
            break;
        }
        decompiler.addEOL(Token.SEMI);
        pn.add(";\n");

        return pn;
    }

    /**
     * Returns whether or not the bits in the mask have changed to all set.
     * @param before bits before change
     * @param after bits after change
     * @param mask mask for bits
     * @return true if all the bits in the mask are set in "after" but not 
     *              "before"
     */
    private static final boolean nowAllSet(int before, int after, int mask)
    {
        return ((before & mask) != mask) && ((after & mask) == mask);
    }
    
    private JSNode returnOrYield(int tt, boolean exprContext)
        throws IOException, ParserException
    {
        if (!insideFunction()) {
            reportError(tt == Token.RETURN ? "msg.bad.return"
                                           : "msg.bad.yield");
        }
        consumeToken();
        decompiler.addToken(tt);
        int lineno = ts.getLineno();

        JSNode e;
        /* This is ugly, but we don't want to require a semicolon. */
        switch (peekTokenOrEOL()) {
          case Token.SEMI:
          case Token.RC:
          case Token.EOF:
          case Token.EOL:
          case Token.ERROR:
          case Token.RB:
          case Token.RP:
          case Token.YIELD:
            e = null;
            break;
          default:
            e = jsexpr(false);
            break;
        }

        int before = endFlags;
        JSNode ret;

        if (tt == Token.RETURN) {
            if (e == null ) {
                endFlags |= Node.END_RETURNS;
            } else {
                endFlags |= Node.END_RETURNS_VALUE;
            }
            if (e == null)
                ret = jsFactory.createNode("return");
            else
                ret = jsFactory.createNode("return ").add(e);
//            ret = nf.createReturn(e, lineno);
            
            // see if we need a strict mode warning
            if (nowAllSet(before, endFlags, 
                          Node.END_RETURNS|Node.END_RETURNS_VALUE))
            {
                addStrictWarning("msg.return.inconsistent", "");
            }
        } else {
            endFlags |= Node.END_YIELDS;
            ret = jsFactory.createNode("yield ").add(e);
//            ret = nf.createYield(e, lineno);
//            if (!exprContext)
//                ret = new Node(Token.EXPR_VOID, ret, lineno);
        }

        // see if we are mixing yields and value returns.
        if (nowAllSet(before, endFlags, 
                      Node.END_YIELDS|Node.END_RETURNS_VALUE))
        {
            String name = ((JSFunction)currentScriptOrFn).getFunctionName();
            if (name.length() == 0)
                addError("msg.anon.generator.returns", "");
            else
                addError("msg.generator.returns", name);
        }

        return ret;
    }

    /**
     * Parse a 'var' or 'const' statement, or a 'var' init list in a for
     * statement.
     * @param inFor true if we are currently in the midst of the init
     * clause of a for.
     * @param declType A token value: either VAR, CONST, or LET depending on
     * context.
     * @return The parsed statement
     * @throws IOException
     * @throws ParserException
     */
    private JSNode variables(boolean inFor, int declType)
        throws IOException, ParserException
    {
//        JSNode result = nf.createVariables(declType, ts.getLineno());
        JSVariables result = jsFactory.createVariable(declType);
        boolean first = true;
        for (;;) {
            JSNode destructuring = null;
            String s = null;
            String lang = ScriptRuntime.TOFILL;
            int tt = peekToken();
            if (tt == Token.LB || tt == Token.LC) {
                // Destructuring assignment, e.g., var [a,b] = ...
                destructuring = jsprimaryExpr();
            } else {
                // Simple variable name
                mustMatchToken(Token.NAME, "msg.bad.var");
                s = ts.getString();
                lang = ts.getLastLanguageString();
    
                if (!first)
                    decompiler.addToken(Token.COMMA);
                first = false;
    
                decompiler.addName(s);
                defineSymbol(declType, inFor, s);
            }
    
            JSNode init = null;
            if (matchToken(Token.ASSIGN)) {
                decompiler.addToken(Token.ASSIGN);
                init = jsassignExpr(inFor);
            }

// TODO: Fill this in            
//            if (destructuring != null) {
//                if (init == null) {
//                    if (!inFor)
//                        reportError("msg.destruct.assign.no.init");
//                    nf.addChildToBack(result, destructuring);
//                } else {
//                    nf.addChildToBack(result,
//                        nf.createDestructuringAssignment(declType,
//                            destructuring, init));
//                }
//            } else {
                JSName name = jsFactory.createName(lang, s);
//                JSNode name = nf.createName(lang, s);
                if (init != null)
                    result.addVar(name, init);
//                    nf.addChildToBack(name, init);
                else
                    result.addVar(name, null);
//                nf.addChildToBack(result, name);
//            }
    
            if (!matchToken(Token.COMMA))
                break;
        }
        return result;
    }

    
    private Node let(boolean isStatement)
        throws IOException, ParserException
    {
        return null;
//TODO: Fill this in
//        mustMatchToken(Token.LP, "msg.no.paren.after.let");
//        decompiler.addToken(Token.LP);
//        Node result = nf.createScopeNode(Token.LET, ts.getLineno());
//        pushScope(result);
//        try {
//              Node vars = variables(false, Token.LET);
//              nf.addChildToBack(result, vars);
//              mustMatchToken(Token.RP, "msg.no.paren.let");
//              decompiler.addToken(Token.RP);
//              if (isStatement && peekToken() == Token.LC) {
//                  // let statement
//                  consumeToken();
//                  decompiler.addEOL(Token.LC);
//                  nf.addChildToBack(result, statements(null));
//                  mustMatchToken(Token.RC, "msg.no.curly.let");
//                  decompiler.addToken(Token.RC);
//              } else {
//                  // let expression
//                  result.setType(Token.LETEXPR);
//                  nf.addChildToBack(result, expr(false));
//                  if (isStatement) {
//                      // let expression in statement context
//                      result = nf.createExprStatement(result, ts.getLineno());
//                  }
//              }
//        } finally {
//            popScope();
//        }
//        return result;
    }
    
    void defineSymbol(int declType, boolean ignoreNotInBlock, String name) {
        JSScope definingScope = currentScope.getDefiningScope(name);
        Node.Scope.Symbol symbol = definingScope != null 
                                  ? definingScope.getSymbol(name)
                                  : null;
        boolean error = false;
        if (symbol != null && (symbol.declType == Token.CONST ||
            declType == Token.CONST))
        {
            error = true;
        } else {
            switch (declType) {
              case Token.LET:
                if (symbol != null && definingScope == currentScope) {
                    error = symbol.declType == Token.LET;
                }
                int currentScopeType = currentScope.getType();
                if (!ignoreNotInBlock && 
                    ((currentScopeType == Token.LOOP) ||
                     (currentScopeType == Token.IF)))
                {
                    addError("msg.let.decl.not.in.block");
                }
                currentScope.putSymbol(name, 
                    new Node.Scope.Symbol(declType, name));
                break;
                
              case Token.VAR:
              case Token.CONST:
              case Token.FUNCTION:
                if (symbol != null) {
                    if (symbol.declType == Token.VAR)
                        addStrictWarning("msg.var.redecl", name);
                    else if (symbol.declType == Token.LP) {
                        addStrictWarning("msg.var.hides.arg", name);
                    }
                } else {
                    currentScriptOrFn.putSymbol(name, 
                        new Node.Scope.Symbol(declType, name));
                }
                break;
                
              case Token.LP:
                if (symbol != null) {
                    // must be duplicate parameter. Second parameter hides the 
                    // first, so go ahead and add the second pararameter
                    addWarning("msg.dup.parms", name);
                }
                currentScriptOrFn.putSymbol(name, 
                    new Node.Scope.Symbol(declType, name));
                break;
                
              default:
                throw Kit.codeBug();
            }
        }
        if (error) {
            addError(symbol.declType == Token.CONST ? "msg.const.redecl" :
                     symbol.declType == Token.LET ? "msg.let.redecl" :
                     symbol.declType == Token.VAR ? "msg.var.redecl" :
                     symbol.declType == Token.FUNCTION ? "msg.fn.redecl" :
                     "msg.parm.redecl", name);
        }
    }

    private JSNode jsexpr(boolean inForInit)
            throws IOException, ParserException
        {
            JSNode pn = jsassignExpr(inForInit);
            while (matchToken(Token.COMMA)) {
                decompiler.addToken(Token.COMMA);
//                if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
//                    addStrictWarning("msg.no.side.effects", "");
                if (peekToken() == Token.YIELD) {
                  reportError("msg.yield.parenthesized");
                }
                pn = jsFactory.createSimpleBinaryOperator(",", pn, jsassignExpr(inForInit));
//                pn = nf.createBinary(Token.COMMA, pn, assignExpr(inForInit));
            }
            return pn;
        }

    private JSNode jsassignExpr(boolean inForInit)
            throws IOException, ParserException
        {
            int tt = peekToken();
    // TODO: Fill this in        
//            if (tt == Token.YIELD) {
//                consumeToken();
//                return returnOrYield(tt, true);
//            }
            JSNode pn = condExpr(inForInit);

            tt = peekToken();
            if (Token.FIRST_ASSIGN <= tt && tt <= Token.LAST_ASSIGN) {
                consumeToken();
                decompiler.addToken(tt);
                pn = jsFactory.createAssignment(tokenToString(tt), pn, jsassignExpr(inForInit));
//                pn = nf.createAssignment(tt, pn, assignExpr(inForInit));
            }

            return pn;
        }

    private JSNode condExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = orExpr(inForInit);
        if (matchToken(Token.HOOK)) {
            decompiler.addToken(Token.HOOK);
            JSNode ifTrue = jsassignExpr(false);
            mustMatchToken(Token.COLON, "msg.no.colon.cond");
            decompiler.addToken(Token.COLON);
            JSNode ifFalse = jsassignExpr(inForInit);
            return jsFactory.createNode()
                .add("(")
                .addCondition(pn)
                .add("?")
                .add(ifTrue)
                .add(":")
                .add(ifFalse)
                .add(")");
//            return nf.createCondExpr(pn, ifTrue, ifFalse);
        }

        return pn;
    }

    private JSNode orExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = andExpr(inForInit);
        if (matchToken(Token.OR)) {
            decompiler.addToken(Token.OR);
            pn = jsFactory.createSimpleBinaryOperator(tokenToString(Token.OR), pn, orExpr(inForInit));
//            pn = nf.createBinary(Token.OR, pn, orExpr(inForInit));
        }

        return pn;
    }

    private JSNode andExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = bitOrExpr(inForInit);
        if (matchToken(Token.AND)) {
            decompiler.addToken(Token.AND);
            pn = jsFactory.createSimpleBinaryOperator(tokenToString(Token.AND), pn, andExpr(inForInit));
//            pn = nf.createBinary(Token.AND, pn, andExpr(inForInit));
        }

        return pn;
    }

    private JSNode bitOrExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = bitXorExpr(inForInit);
        while (matchToken(Token.BITOR)) {
            decompiler.addToken(Token.BITOR);
            pn = jsFactory.createSimpleBinaryOperator(tokenToString(Token.BITXOR), pn, bitXorExpr(inForInit));
//            pn = nf.createBinary(Token.BITOR, pn, bitXorExpr(inForInit));
        }
        return pn;
    }

    private JSNode bitXorExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = bitAndExpr(inForInit);
        while (matchToken(Token.BITXOR)) {
            decompiler.addToken(Token.BITXOR);
            pn = jsFactory.createSimpleBinaryOperator(tokenToString(Token.BITXOR), pn, bitAndExpr(inForInit));
//            pn = nf.createBinary(Token.BITXOR, pn, bitAndExpr(inForInit));
        }
        return pn;
    }

    private JSNode bitAndExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = eqExpr(inForInit);
        while (matchToken(Token.BITAND)) {
            decompiler.addToken(Token.BITAND);
            pn = jsFactory.createSimpleBinaryOperator(tokenToString(Token.BITAND), pn, eqExpr(inForInit));
//            pn = nf.createBinary(Token.BITAND, pn, eqExpr(inForInit));
        }
        return pn;
    }

    private JSNode eqExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = relExpr(inForInit);
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.EQ:
              case Token.NE:
              case Token.SHEQ:
              case Token.SHNE:
                consumeToken();
                int decompilerToken = tt;
                int parseToken = tt;
// TODO: Fill this in            
//                if (compilerEnv.getLanguageVersion() == Context.VERSION_1_2) {
//                    // JavaScript 1.2 uses shallow equality for == and != .
//                    // In addition, convert === and !== for decompiler into
//                    // == and != since the decompiler is supposed to show
//                    // canonical source and in 1.2 ===, !== are allowed
//                    // only as an alias to ==, !=.
//                    switch (tt) {
//                      case Token.EQ:
//                        parseToken = Token.SHEQ;
//                        break;
//                      case Token.NE:
//                        parseToken = Token.SHNE;
//                        break;
//                      case Token.SHEQ:
//                        decompilerToken = Token.EQ;
//                        break;
//                      case Token.SHNE:
//                        decompilerToken = Token.NE;
//                        break;
//                    }
//                }
                decompiler.addToken(decompilerToken);
                pn = jsFactory.createSimpleBinaryOperator(tokenToString(parseToken), pn, relExpr(inForInit));
//                pn = nf.createBinary(parseToken, pn, relExpr(inForInit));
                continue;
            }
            break;
        }
        return pn;
    }

    private JSNode relExpr(boolean inForInit)
        throws IOException, ParserException
    {
        JSNode pn = shiftExpr();
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.IN:
                if (inForInit)
                    break;
                // fall through
              case Token.INSTANCEOF:
              case Token.LE:
              case Token.LT:
              case Token.GE:
              case Token.GT:
                consumeToken();
                decompiler.addToken(tt);
                pn = jsFactory.createSimpleBinaryOperator(tokenToString(tt), pn, shiftExpr());
                continue;
            }
            break;
        }
        return pn;
    }

    private JSNode shiftExpr()
        throws IOException, ParserException
    {
        JSNode pn = addExpr();
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.LSH:
              case Token.URSH:
              case Token.RSH:
                consumeToken();
                decompiler.addToken(tt);
                pn = jsFactory.createSimpleBinaryOperator(tokenToString(tt), pn, addExpr());
//                pn = nf.createBinary(tt, pn, addExpr());
                continue;
            }
            break;
        }
        return pn;
    }

    private JSNode addExpr()
        throws IOException, ParserException
    {
        JSNode pn = mulExpr();
        for (;;) {
            int tt = peekToken();
            if (tt == Token.ADD || tt == Token.SUB) {
                consumeToken();
                decompiler.addToken(tt);
                // flushNewLines
                pn = jsFactory.createSimpleBinaryOperator(tokenToString(tt), pn, mulExpr());
//                pn = nf.createBinary(tt, pn, mulExpr());
                continue;
            }
            break;
        }

        return pn;
    }

    private JSNode mulExpr()
        throws IOException, ParserException
    {
        JSNode pn = unaryExpr();
        for (;;) {
            int tt = peekToken();
            switch (tt) {
              case Token.MUL:
              case Token.DIV:
              case Token.MOD:
                consumeToken();
                decompiler.addToken(tt);
                pn = jsFactory.createSimpleBinaryOperator(tokenToString(tt), pn, unaryExpr());
//                pn = nf.createBinary(tt, pn, unaryExpr());
                continue;
            }
            break;
        }

        return pn;
    }

    private JSNode unaryExpr()
        throws IOException, ParserException
    {
        int tt;

        tt = peekToken();

        switch(tt) {
        case Token.TYPEOF:
        case Token.VOID:
        case Token.NOT:
          case Token.BITNOT:
            consumeToken();
            decompiler.addToken(tt);
            return jsFactory.createSimplePreUnaryOperator(tokenToString(tt) + " ", unaryExpr());
//            return nf.createUnary(tt, unaryExpr());

        case Token.ADD:
            consumeToken();
            // Convert to special POS token in decompiler and parse tree
            decompiler.addToken(Token.POS);
            return jsFactory.createSimplePreUnaryOperator(tokenToString(Token.POS), unaryExpr());

        case Token.SUB:
            consumeToken();
            // Convert to special NEG token in decompiler and parse tree
            decompiler.addToken(Token.NEG);
            return jsFactory.createSimplePreUnaryOperator(tokenToString(Token.NEG), unaryExpr());

        case Token.INC:
        case Token.DEC:
            consumeToken();
            decompiler.addToken(tt);
            return jsFactory.createSimplePreUnaryOperator(tokenToString(tt), memberExpr(true));

        case Token.DELPROP:
        {
            consumeToken();
            decompiler.addToken(Token.DELPROP);
            JSNode prop = unaryExpr();
            if (prop instanceof JSTranslationMapping)
            {
                JSTranslationMapping trans = (JSTranslationMapping)prop;
                return jsFactory.createNode()
                        .add("(babyl.delTranslation(")
                        .add(trans.obj)
                        .add(",")
                        .add(trans.lang)
                        .add(",")
                        .add(trans.name)
                        .add("))");
            }
            else
                return jsFactory.createSimplePreUnaryOperator("delete ", prop);
//            return nf.createUnary(Token.DELPROP, unaryExpr());
        }
        
        case Token.ERROR:
            consumeToken();
            break;

// TODO: fill this in            
//        // XML stream encountered in expression.
//        case Token.LT:
//            if (compilerEnv.isXmlAvailable()) {
//                consumeToken();
//                Node pn = xmlInitializer();
//                return memberExprTail(true, pn);
//            }
//            // Fall thru to the default handling of RELOP

        default:
            JSNode pn = memberExpr(true);

            // Don't look across a newline boundary for a postfix incop.
            tt = peekTokenOrEOL();
            if (tt == Token.INC || tt == Token.DEC) {
                consumeToken();
                decompiler.addToken(tt);
                return jsFactory.createSimplePostUnaryOperator(tokenToString(tt), pn);
//                return nf.createIncDec(tt, true, pn);
            }
            return pn;
        }
        return jsFactory.createError("error"); // Only reached on error.Try to continue.

    }

//    private Node xmlInitializer() throws IOException
//    {
//        int tt = ts.getFirstXMLToken();
//        if (tt != Token.XML && tt != Token.XMLEND) {
//            reportError("msg.syntax");
//            return null;
//        }
//
//        /* Make a NEW node to append to. */
//        Node pnXML = nf.createLeaf(Token.NEW);
//
//        String xml = ts.getString();
//        boolean fAnonymous = xml.trim().startsWith("<>");
//
//        Node pn = nf.createName(ScriptRuntime.TOFILL, fAnonymous ? "XMLList" : "XML");
//        nf.addChildToBack(pnXML, pn);
//
//        pn = null;
//        Node expr;
//        for (;;tt = ts.getNextXMLToken()) {
//            switch (tt) {
//            case Token.XML:
//                xml = ts.getString();
//                decompiler.addName(xml);
//                mustMatchToken(Token.LC, "msg.syntax");
//                decompiler.addToken(Token.LC);
//                expr = (peekToken() == Token.RC)
//                    ? nf.createString("")
//                    : expr(false);
//                mustMatchToken(Token.RC, "msg.syntax");
//                decompiler.addToken(Token.RC);
//                if (pn == null) {
//                    pn = nf.createString(xml);
//                } else {
//                    pn = nf.createBinary(Token.ADD, pn, nf.createString(xml));
//                }
//                if (ts.isXMLAttribute()) {
//                    /* Need to put the result in double quotes */
//                    expr = nf.createUnary(Token.ESCXMLATTR, expr);
//                    Node prepend = nf.createBinary(Token.ADD,
//                                                   nf.createString("\""),
//                                                   expr);
//                    expr = nf.createBinary(Token.ADD,
//                                           prepend,
//                                           nf.createString("\""));
//                } else {
//                    expr = nf.createUnary(Token.ESCXMLTEXT, expr);
//                }
//                pn = nf.createBinary(Token.ADD, pn, expr);
//                break;
//            case Token.XMLEND:
//                xml = ts.getString();
//                decompiler.addName(xml);
//                if (pn == null) {
//                    pn = nf.createString(xml);
//                } else {
//                    pn = nf.createBinary(Token.ADD, pn, nf.createString(xml));
//                }
//
//                nf.addChildToBack(pnXML, pn);
//                return pnXML;
//            default:
//                reportError("msg.syntax");
//                return null;
//            }
//        }
//    }

    private void argumentList(JSNode listNode)
        throws IOException, ParserException
    {
        boolean matched;
        matched = matchToken(Token.RP);
        if (!matched) {
            boolean first = true;
            do {
                if (!first)
                    decompiler.addToken(Token.COMMA);
                if (!first)
                    listNode.add(",");
                first = false;
                if (peekToken() == Token.YIELD) {
                    reportError("msg.yield.parenthesized");
                }
                listNode.add(jsassignExpr(false));
//                nf.addChildToBack(listNode, assignExpr(false));
            } while (matchEitherToken(Token.COMMA, Token.SEMI));

            mustMatchToken(Token.RP, "msg.no.paren.arg");
        }
        decompiler.addToken(Token.RP);
    }

    private JSNode memberExpr(boolean allowCallSyntax)
        throws IOException, ParserException
    {
        int tt;

        JSNode pn;

        /* Check for new expressions. */
        tt = peekToken();
        if (tt == Token.NEW) {
            /* Eat the NEW token. */
            consumeToken();
            decompiler.addToken(Token.NEW);

            /* Make a NEW node to append to. */
            pn = jsFactory.createNode()
                    .add("babylwrap(new ")
                    .add(memberExpr(false));
//            pn = nf.createCallOrNew(Token.NEW, memberExpr(false));

            if (matchToken(Token.LP)) {
                pn.add("(");
                decompiler.addToken(Token.LP);
                /* Add the arguments to pn, if any are supplied. */
                argumentList(pn);
                pn.add(")");
            }
            pn.add(")");

            /* XXX there's a check in the C source against
             * "too many constructor arguments" - how many
             * do we claim to support?
             */

            /* Experimental syntax:  allow an object literal to follow a new expression,
             * which will mean a kind of anonymous class built with the JavaAdapter.
             * the object literal will be passed as an additional argument to the constructor.
             */
// TODO: Fill this in            
//            tt = peekToken();
//            if (tt == Token.LC) {
//                nf.addChildToBack(pn, jsprimaryExpr());
//            }
        } else {
            pn = jsprimaryExpr();
        }

        return memberExprTail(allowCallSyntax, pn);
    }

    private JSNode memberExprTail(boolean allowCallSyntax, JSNode pn)
            throws IOException, ParserException
    {
      tailLoop:
        for (;;) {
            int tt = peekToken();
            switch (tt) {

              case Token.DOT:
//              case Token.DOTDOT:
                {
                    int memberTypeFlags;
                    String s;

                    consumeToken();
                    decompiler.addToken(tt);
                    memberTypeFlags = 0;
//                    if (tt == Token.DOTDOT) {
//                        mustHaveXML();
//                        memberTypeFlags = Node.DESCENDANTS_FLAG;
//                    }
//                    if (!compilerEnv.isXmlAvailable()) {
                        mustMatchToken(Token.NAME, "msg.no.name.after.dot");
                        s = ts.getString();
                        decompiler.addName(s);
                        String lang = lastPeekedLanguageString();

                        JSTemporary temp = currentScriptOrFn.getTemp();
                        // Save the LHS
                        pn = jsFactory.createNode()
                                .add("(")
                                .add(temp)
                                .add("=(")
                                .add(pn)
                                .add("))");
                        // Now do the lookup 
                        pn.add("[babyllookup(")
                                .add(temp)
                                .add(",'" + lang + "','" + s + "'")
                                .add(")]");
                        currentScriptOrFn.releaseTemp(temp);
//                        pn = nf.createPropertyGet(pn, lang, null, s, memberTypeFlags);
                        break;
//                    }
//
//                    tt = nextToken();
//                    switch (tt) {
//                    
//                      // needed for generator.throw();
//                      case Token.THROW:
//                        decompiler.addName("throw");
//                        pn = propertyName(pn, "throw", memberTypeFlags);
//                        break;
//
//                      // handles: name, ns::name, ns::*, ns::[expr]
//                      case Token.NAME:
//                        s = ts.getString();
//                        decompiler.addName(s);
//                        pn = propertyName(pn, s, memberTypeFlags);
//                        break;
//
//                      // handles: *, *::name, *::*, *::[expr]
//                      case Token.MUL:
//                        decompiler.addName("*");
//                        pn = propertyName(pn, "*", memberTypeFlags);
//                        break;
//
//                      // handles: '@attr', '@ns::attr', '@ns::*', '@ns::*',
//                      //          '@::attr', '@::*', '@*', '@*::attr', '@*::*'
//                      case Token.XMLATTR:
//                        decompiler.addToken(Token.XMLATTR);
//                        pn = attributeAccess(pn, memberTypeFlags);
//                        break;
//
//                      default:
//                        reportError("msg.no.name.after.dot");
//                    }
                }
//                break;
//
//              case Token.DOTQUERY:
//                consumeToken();
//                mustHaveXML();
//                decompiler.addToken(Token.DOTQUERY);
//                pn = nf.createDotQuery(pn, expr(false), ts.getLineno());
//                mustMatchToken(Token.RP, "msg.no.paren");
//                decompiler.addToken(Token.RP);
//                break;

              case Token.LB:
              {
                String lang = lastPeekedLanguageString();
                consumeToken();
                decompiler.addToken(Token.LB);
                JSNode left = jsexpr(false);
                if (peekToken() == Token.COLON)
                {
                    // Look for a reference to a translated name
                    consumeToken();
                    decompiler.addToken(Token.COLON);
                    JSNode right = jsexpr(false);
                    pn = jsFactory.createTranslationMapping(pn, left, right);
//                    pn = nf.createTranslatedNameGet(pn, null, left, right, 0);
                }
                else
                {
                    JSTemporary temp = currentScriptOrFn.getTemp();
                    // Save the LHS
                    pn = jsFactory.createNode()
                            .add("(")
                            .add(temp)
                            .add("=(")
                            .add(pn)
                            .add("))");
                    // Now do the lookup 
                    pn.add("[babyllookup(")
                            .add(temp)
                            .add(",'" + lang + "',")
                            .add(left)
                            .add(")]");
                    currentScriptOrFn.releaseTemp(temp);
                }
//                    pn = nf.createElementGet(pn, lang, null, left, 0);
                mustMatchToken(Token.RB, "msg.no.bracket.index");
                decompiler.addToken(Token.RB);
                break;
              }

              case Token.LP:
                if (!allowCallSyntax) {
                    break tailLoop;
                }
                consumeToken();
                decompiler.addToken(Token.LP);
                pn.add("(");
//                pn = nf.createCallOrNew(Token.CALL, pn);
                /* Add the arguments to pn, if any are supplied. */
                argumentList(pn);
                pn.add(")"); 
                break;

              default:
                break tailLoop;
            }
        }
        return pn;
    }

    /*
     * Xml attribute expression:
     *   '@attr', '@ns::attr', '@ns::*', '@ns::*', '@*', '@*::attr', '@*::*'
     */
// TODO: Fill this in        
//    private Node attributeAccess(Node pn, int memberTypeFlags)
//        throws IOException
//    {
//        memberTypeFlags |= Node.ATTRIBUTE_FLAG;
//        int tt = nextToken();
//
//        switch (tt) {
//          // handles: @name, @ns::name, @ns::*, @ns::[expr]
//          case Token.NAME:
//            {
//                String s = ts.getString();
//                decompiler.addName(s);
//                pn = propertyName(pn, s, memberTypeFlags);
//            }
//            break;
//
//          // handles: @*, @*::name, @*::*, @*::[expr]
//          case Token.MUL:
//            decompiler.addName("*");
//            pn = propertyName(pn, "*", memberTypeFlags);
//            break;
//
//          // handles @[expr]
//          case Token.LB:
//          {
//            decompiler.addToken(Token.LB);
//            String lang = lastPeekedLanguageString();
//            pn = nf.createElementGet(pn, lang, null, expr(false), memberTypeFlags);
//            mustMatchToken(Token.RB, "msg.no.bracket.index");
//            decompiler.addToken(Token.RB);
//            break;
//          }
//
//          default:
//            reportError("msg.no.name.after.xmlAttr");
//            pn = nf.createPropertyGet(pn, lastPeekedLanguageString(), null, "?", memberTypeFlags);
//            break;
//        }
//
//        return pn;
//    }

    /**
     * Check if :: follows name in which case it becomes qualified name
     */
//    private Node propertyName(Node pn, String name, int memberTypeFlags)
//        throws IOException, ParserException
//    {
//        String lang = lastPeekedLanguageString();
//        
//        String namespace = null;
//        if (matchToken(Token.COLONCOLON)) {
//            decompiler.addToken(Token.COLONCOLON);
//            namespace = name;
//
//            int tt = nextToken();
//            switch (tt) {
//              // handles name::name
//              case Token.NAME:
//                name = ts.getString();
//                decompiler.addName(name);
//                break;
//
//              // handles name::*
//              case Token.MUL:
//                decompiler.addName("*");
//                name = "*";
//                break;
//
//              // handles name::[expr]
//              case Token.LB:
//              {
//                String sublang = lastPeekedLanguageString();
//                decompiler.addToken(Token.LB);
//                pn = nf.createElementGet(pn, sublang, namespace, expr(false),
//                                         memberTypeFlags);
//                mustMatchToken(Token.RB, "msg.no.bracket.index");
//                decompiler.addToken(Token.RB);
//                return pn;
//              }
//
//              default:
//                reportError("msg.no.name.after.coloncolon");
//                name = "?";
//            }
//        }
//
//        pn = nf.createPropertyGet(pn, lang, namespace, name, memberTypeFlags);
//        return pn;
//    }

//    private JSNode arrayComprehension(String langArray, String arrayName, Node expr)
//        throws IOException, ParserException
//    {
//        if (nextToken() != Token.FOR)
//            throw Kit.codeBug(); // shouldn't be here if next token isn't 'for'
//        decompiler.addName(" "); // space after array literal expr
//        decompiler.addToken(Token.FOR);
//        boolean isForEach = false;
//        if (matchToken(Token.NAME)) {
//            decompiler.addName(ts.getString());
//            if (ts.getString().equals("each")) {
//                isForEach = true;
//            } else {
//                reportError("msg.no.paren.for");
//            }
//        }
//        mustMatchToken(Token.LP, "msg.no.paren.for");
//        decompiler.addToken(Token.LP);
//        String lang;
//        String name;
//        int tt = peekToken();
//        if (tt == Token.LB || tt == Token.LC) {
//            // handle destructuring assignment
//            name = currentScriptOrFn.getNextTempName();
//            lang = ScriptRuntime.TOFILL;
//            defineSymbol(Token.LP, false, name);
//            expr = nf.createBinary(Token.COMMA,
//                nf.createAssignment(Token.ASSIGN, primaryExpr(), 
//                                    nf.createName(lang, name)),
//                expr);
//        } else if (tt == Token.NAME) {
//            consumeToken();
//            name = ts.getString();
//            lang = ts.getLastLanguageString();
//            decompiler.addName(name);
//        } else {
//            reportError("msg.bad.var");
//            return nf.createNumber(0);
//        }
//
//        Node init = nf.createName(lang, name);
//        // Define as a let since we want the scope of the variable to
//        // be restricted to the array comprehension
//        defineSymbol(Token.LET, false, name);
//        
//        mustMatchToken(Token.IN, "msg.in.after.for.name");
//        decompiler.addToken(Token.IN);
//        Node iterator = expr(false);
//        mustMatchToken(Token.RP, "msg.no.paren.for.ctrl");
//        decompiler.addToken(Token.RP);
//        
//        Node body;
//        tt = peekToken();
//        if (tt == Token.FOR) {
//            body = arrayComprehension(langArray, arrayName, expr);
//        } else {
//            Node call = nf.createCallOrNew(Token.CALL,
//                nf.createPropertyGet(nf.createName(langArray, arrayName), 
//                                     ScriptRuntime.TOFILL, null,
//                                     "push", 0));
//            call.addChildToBack(expr);
//            body = new Node(Token.EXPR_VOID, call, ts.getLineno());
//            if (tt == Token.IF) {
//                consumeToken();
//                decompiler.addToken(Token.IF);
//                int lineno = ts.getLineno();
//                Node cond = null; //condition();
//                body = nf.createIf(cond, body, null, lineno);
//            }
//            mustMatchToken(Token.RB, "msg.no.bracket.arg");
//            decompiler.addToken(Token.RB);
//        }
//
//        Node loop = enterLoop(null, true);
//        try {
//            return nf.createForIn(Token.LET, lang, loop, init, iterator, body,
//                                  isForEach);
//        } finally {
//            exitLoop(false);
//        }
//    }
    
    private JSNode jsprimaryExpr()
            throws IOException, ParserException
    {
        JSNode pn;

        int ttFlagged = nextFlaggedToken();
        int tt = ttFlagged & CLEAR_TI_MASK;

        switch(tt) {

          case Token.FUNCTION:
            return function(FunctionNode.FUNCTION_EXPRESSION);

          case Token.LB: {
            ArrayList<JSNode> elems = new ArrayList<JSNode>();
            int skipCount = 0;
            int destructuringLen = 0;
            decompiler.addToken(Token.LB);
            boolean after_lb_or_comma = true;
            for (;;) {
                tt = peekToken();

                if (tt == Token.COMMA || tt == Token.SEMI) {
                    consumeToken();
                    decompiler.addToken(Token.COMMA);
                    if (!after_lb_or_comma) {
                        after_lb_or_comma = true;
                    } else {
                        elems.add(null);
                        ++skipCount;
                    }
                } else if (tt == Token.RB) {
                    consumeToken();
                    decompiler.addToken(Token.RB);
                    // for ([a,] in obj) is legal, but for ([a] in obj) is 
                    // not since we have both key and value supplied. The
                    // trick is that [a,] and [a] are equivalent in other
                    // array literal contexts. So we calculate a special
                    // length value just for destructuring assignment.
                    destructuringLen = elems.size() + 
                                       (after_lb_or_comma ? 1 : 0);
                    break;
// TODO: Fill this in                    
//                } else if (skipCount == 0 && elems.size() == 1 &&
//                           tt == Token.FOR)
//                {
//                    JSNode scopeNode = nf.createScopeNode(Token.ARRAYCOMP, 
//                                                        ts.getLineno());
//                    String tempName = currentScriptOrFn.getNextTempName();
//                    pushScope(scopeNode);
//                    try {
//                        defineSymbol(Token.LET, false, tempName);
//                        Node expr = (Node) elems.get(0);
//                        Node block = nf.createBlock(ts.getLineno());
//                        Node init = new Node(Token.EXPR_VOID, 
//                            nf.createAssignment(Token.ASSIGN, 
//                                nf.createName(ScriptRuntime.TOFILL, tempName),
//                                nf.createCallOrNew(Token.NEW,
//                                    nf.createName(ScriptRuntime.TOFILL, "Array"))), ts.getLineno());
//                        block.addChildToBack(init);
//                        block.addChildToBack(arrayComprehension(ScriptRuntime.TOFILL, tempName, 
//                            expr));
//                        scopeNode.addChildToBack(block);
//                        scopeNode.addChildToBack(nf.createName(ScriptRuntime.TOFILL, tempName));
//                        return scopeNode;
//                    } finally {
//                        popScope();
//                    }
                } else {
                    if (!after_lb_or_comma) {
                        reportError("msg.no.bracket.arg");
                    }
                    elems.add(jsassignExpr(false));
                    after_lb_or_comma = false;
                }
            }
            JSNode arr = jsFactory.createNode()
                    .add("[");
            boolean isFirst = true;
            for (JSNode elem : elems)
            {
                if (!isFirst) arr.add(",");
                isFirst = false;
                if (arr != null) arr.add(elem);
            }
            arr.add("]");
            return arr;
//            return nf.createArrayLiteral(elems, skipCount, destructuringLen);
          }

          case Token.LC: {
            ObjArray elems = new ObjArray();
            decompiler.addToken(Token.LC);
            if (!matchToken(Token.RC)) {

                boolean first = true;
            commaloop:
                do {
                    String property;

                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    else
                        first = false;

                    tt = peekToken();
                    switch(tt) {
                      case Token.NAME:
                      case Token.STRING:
                        consumeToken();
                        // map NAMEs to STRINGs in object literal context
                        // but tell the decompiler the proper type
                        String s = ts.getString();
                        if (tt == Token.NAME) {
                            if (s.equals("get") &&
                                peekToken() == Token.NAME) {
                                decompiler.addToken(Token.GET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                property = s;
//                                property = ScriptRuntime.getIndexObject(s);
                                if (!getterSetterProperty(elems, property,
                                                          true))
                                    break commaloop;
                                break;
                            } else if (s.equals("set") &&
                                       peekToken() == Token.NAME) {
                                decompiler.addToken(Token.SET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                property = s;
//                                property = ScriptRuntime.getIndexObject(s);
                                if (!getterSetterProperty(elems, property,
                                                          false))
                                    break commaloop;
                                break;
                            }
                            decompiler.addName(s);
                        } else {
                            decompiler.addString(s);
                        }
                        property = s;
//                        property = ScriptRuntime.getIndexObject(s);
                        plainProperty(elems, property);
                        break;

                      case Token.NUMBER:
                        consumeToken();
                        double n = ts.getNumber();
                        decompiler.addNumber(n);
                        property = ts.getString();
//                        property = ScriptRuntime.getIndexObject(n);
                        plainProperty(elems, property);
                        break;

                      case Token.RC:
                        // trailing comma is OK.
                        break commaloop;
                    default:
                        reportError("msg.bad.prop");
                        break commaloop;
                    }
                } while (matchEitherToken(Token.COMMA, Token.SEMI));

                mustMatchToken(Token.RC, "msg.no.brace.prop");
            }
            decompiler.addToken(Token.RC);
            JSNode obj = jsFactory.createNode();
            obj.add("{");
            for (int n = 0; n < elems.size(); n++)
            {
                JSNode prop = (JSNode)elems.get(n);
                if (n != 0) obj.add(",");
                obj.add(prop);
            }
            obj.add("}");
            return obj;
//            return nf.createObjectLiteral(elems);
          }
          
//          case Token.LET:
//            decompiler.addToken(Token.LET);
//            return let(false);

          case Token.LP:

            /* Brendan's IR-jsparse.c makes a new node tagged with
             * TOK_LP here... I'm not sure I understand why.  Isn't
             * the grouping already implicit in the structure of the
             * parse tree?  also TOK_LP is already overloaded (I
             * think) in the C IR as 'function call.'  */
            decompiler.addToken(Token.LP);
            pn = jsFactory.createNode("(")
                    .add(jsexpr(false))
                    .add(")");
//            pn.putProp(Node.PARENTHESIZED_PROP, Boolean.TRUE);
            decompiler.addToken(Token.RP);
            mustMatchToken(Token.RP, "msg.no.paren");
            return pn;

//          case Token.XMLATTR:
//            mustHaveXML();
//            decompiler.addToken(Token.XMLATTR);
//            pn = attributeAccess(null, 0);
//            return pn;

          case Token.NAME: {
            String name = ts.getString();
            String lang = ts.getLastLanguageString();
            if ((ttFlagged & TI_CHECK_LABEL) != 0) {
                if (peekToken() == Token.COLON) {
                    // Do not consume colon, it is used as unwind indicator
                    // to return to statementHelper.
                    // XXX Better way?
                    return jsFactory.createLabel();
                }
            }

            decompiler.addName(name);
// TODO: Fill this in            
//            if (compilerEnv.isXmlAvailable()) {
//                pn = propertyName(null, name, 0);
//            } else {
                pn = jsFactory.createName(lang, name);
//            }
            return pn;
          }

          case Token.NUMBER: {
            double n = ts.getNumber();
            String str = ts.getString();
            decompiler.addNumber(n);
            return jsFactory.createNumber(str);
          }

          case Token.STRING: {
            String s = ts.getString();
            decompiler.addString(s);
            return jsFactory.createString(s);
          }

          case Token.DIV:
          case Token.ASSIGN_DIV: {
            // Got / or /= which should be treated as regexp in fact
            ts.readRegExp(tt);
            String flags = ts.regExpFlags;
            ts.regExpFlags = null;
            String re = ts.getString();
            decompiler.addRegexp(re, flags);
            int index = currentScriptOrFn.addRegexp(re, flags);
            return jsFactory.createNode()
                    .add(tt == Token.DIV ? "/" : "/=")
                    .add(re)
                    .add("/")
                    .add(flags);
//            return nf.createRegExp(index);
          }

          case Token.NULL:
          case Token.THIS:
          case Token.FALSE:
          case Token.TRUE:
            decompiler.addToken(tt);
            return jsFactory.createSpecialConstant(tokenToString(tt));

          case Token.RESERVED:
            reportError("msg.reserved.id");
            break;

          case Token.ERROR:
            /* the scanner or one of its subroutines reported the error. */
            break;

          case Token.EOF:
            reportError("msg.unexpected.eof");
            break;

          default:
            reportError("msg.syntax");
            break;
        }
        return null;    // should never reach here
    }


    private void plainProperty(ObjArray elems, String property)
            throws IOException {
        mustMatchToken(Token.COLON, "msg.no.colon.prop");

        // OBJLIT is used as ':' in object literal for
        // decompilation to solve spacing ambiguity.
        decompiler.addToken(Token.OBJECTLIT);
        elems.add(jsFactory.createNode()
                .add("'")
                .add(jsFactory.escapeJSString(property))
                .add("'")
                .add(":")
                .add(jsassignExpr(false)));
//        elems.add(property);
//        elems.add(jsassignExpr(false));
    }

    private boolean getterSetterProperty(ObjArray elems, String property,
                                         boolean isGetter) throws IOException {
        JSNode f = function(FunctionNode.FUNCTION_EXPRESSION, true);
        if (!(f instanceof JSFunction)) {
//        if (f.getType() != Token.FUNCTION) {
            reportError("msg.bad.prop");
            return false;
        }
//        int fnIndex = f.getExistingIntProp(Node.FUNCTION_PROP);
//        JSFunction fn = currentScriptOrFn.getFunctionNode(fnIndex);
//        if (fn.getFunctionName().length() != 0) {
//            reportError("msg.bad.prop");
//            return false;
//        }
        if (isGetter) {
            elems.add(jsFactory.createNode("get ")
                    .add(property)
                    .add(f));
//            elems.add(nf.createUnary(Token.GET, f));
        } else {
            elems.add(jsFactory.createNode("set ")
                    .add(property)
                    .add(f));
//            elems.add(nf.createUnary(Token.SET, f));
        }
        return true;
    }
    
    private String tokenToString(int token)
    {
        switch(token) {
        case Token.TRUE: return "true";
        case Token.FALSE: return "false";
        case Token.NULL: return "null";
        case Token.THIS: return "this";
        case Token.FUNCTION: return "function";
        case Token.COMMA: return ", ";
        case Token.LC: return "{ ";
        case Token.RC:  return "}";
        case Token.LP: return "(";
        case Token.RP: return ")";
        case Token.LB: return "[";
        case Token.RB: return "]";
        case Token.DOT: return ".";
        case Token.NEW: return "new ";
        case Token.DELPROP: return "delete ";
        case Token.IF: return "if";
        case Token.ELSE: return "else";
        case Token.FOR: return "for";
        case Token.IN: return " in ";
        case Token.WITH: return "with ";
        case Token.WHILE: return "while ";
        case Token.DO: return "do";
        case Token.TRY: return "true";
        case Token.CATCH: return "catch";
        case Token.FINALLY: return "finally";
        case Token.THROW: return "throw";
        case Token.SWITCH: return "switch";
        case Token.BREAK: return "break";
        case Token.CONTINUE: return "continue";
        case Token.CASE: return "case";
        case Token.DEFAULT: return "default";
        case Token.RETURN: return "return";
        case Token.VAR: return "var ";
        case Token.LET: return "let ";
        case Token.SEMI: return ";";
        case Token.ASSIGN: return "=";
        case Token.ASSIGN_ADD: return "+=";
        case Token.ASSIGN_SUB: return "-=";
        case Token.ASSIGN_MUL: return "*=";
        case Token.ASSIGN_DIV: return "/=";
        case Token.ASSIGN_MOD: return "%=";
        case Token.ASSIGN_BITOR: return "|=";
        case Token.ASSIGN_BITXOR: return "^=";
        case Token.ASSIGN_BITAND: return "&=";
        case Token.ASSIGN_LSH: return "<<=";
        case Token.ASSIGN_RSH: return ">>=";
        case Token.ASSIGN_URSH: return ">>>=";
        case Token.HOOK: return "?";
        case Token.OBJECTLIT: return ":";
        case Token.COLON: return ":";
        case Token.OR: return "||";
        case Token.AND: return "&&";
        case Token.BITOR: return "|";
        case Token.BITXOR: return "^";
        case Token.BITAND: return "&";
        case Token.SHEQ: return "===";
        case Token.SHNE: return "!==";
        case Token.EQ: return "==";
        case Token.NE: return "!=";
        case Token.LE: return "<=";
        case Token.LT: return "<";
        case Token.GE: return ">=";
        case Token.GT: return ">";
        case Token.INSTANCEOF: return "instanceof ";
        case Token.LSH: return "<<";
        case Token.RSH: return ">>";
        case Token.URSH: return ">>>";
        case Token.TYPEOF: return "typeof ";
        case Token.VOID: return "void ";
        case Token.CONST: return "const";
        case Token.YIELD: return "yield";
        case Token.NOT: return "!";
        case Token.BITNOT: return "~";
        case Token.POS: return "+";
        case Token.NEG: return "-";
        case Token.INC: return "++";
        case Token.DEC: return "--";
        case Token.ADD: return "+";
        case Token.SUB: return "-";
        case Token.MUL: return "*";
        case Token.DIV: return "/";
        case Token.MOD: return "%";
        
        
        
        case Token.COLONCOLON: return "::";
        case Token.DOTDOT: return "..";
        case Token.DOTQUERY: return ".(";
        case Token.XMLATTR: return "@";

        default:
            throw new RuntimeException("Looking for a string representation for a token that shouldn't be looked up");
        }
    }
}
