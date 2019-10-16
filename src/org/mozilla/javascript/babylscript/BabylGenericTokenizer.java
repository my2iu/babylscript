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
 *   Roger Lawrence
 *   Mike McCabe
 *   Igor Bukanov
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Milen Nankov
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

package org.mozilla.javascript.babylscript;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.BabylTokenizer;
import org.mozilla.javascript.ParserErrorReportingBase;
import org.mozilla.javascript.TokenCharStream;
import org.mozilla.javascript.TokenStream;

public class BabylGenericTokenizer extends BabylTokenizer
{
    BabylGenericTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts, DecimalNumberReader numberReader, Map<String, String> keywords)
    {
        super(p, in, ts, numberReader);
        
        keywordLookup.put(keywords.get("function"), Id_function);
        keywordLookup.put(keywords.get("if"), Id_if);
        keywordLookup.put(keywords.get("else"), Id_else);
        keywordLookup.put(keywords.get("for"), Id_for);
        keywordLookup.put(keywords.get("while"), Id_while);
        keywordLookup.put(keywords.get("return"), Id_return);  
        keywordLookup.put(keywords.get("do"), Id_do);
        keywordLookup.put(keywords.get("true"), Id_true);
        keywordLookup.put(keywords.get("false"), Id_false);
        keywordLookup.put(keywords.get("var"), Id_var);
        keywordLookup.put(keywords.get("break"), Id_break);
        keywordLookup.put(keywords.get("try"), Id_try);
        keywordLookup.put(keywords.get("switch"), Id_switch);
        keywordLookup.put(keywords.get("case"), Id_case);
        keywordLookup.put(keywords.get("null"), Id_null);

        keywordLookup.put(keywords.get("in"), Id_in);
        keywordLookup.put(keywords.get("int"), Id_int);
        keywordLookup.put(keywords.get("let"), Id_let);
        keywordLookup.put(keywords.get("new"), Id_new);
        keywordLookup.put(keywords.get("byte"), Id_byte);
        keywordLookup.put(keywords.get("char"), Id_char);
        keywordLookup.put(keywords.get("enum"), Id_enum);
        keywordLookup.put(keywords.get("goto"), Id_goto);
        keywordLookup.put(keywords.get("long"), Id_long);
        keywordLookup.put(keywords.get("this"), Id_this);
        keywordLookup.put(keywords.get("void"), Id_void);
        keywordLookup.put(keywords.get("with"), Id_with);
        keywordLookup.put(keywords.get("class"), Id_class);
        keywordLookup.put(keywords.get("yield"), Id_yield);
        keywordLookup.put(keywords.get("const"), Id_const);
        keywordLookup.put(keywords.get("final"), Id_final);
        keywordLookup.put(keywords.get("float"), Id_float);
        keywordLookup.put(keywords.get("short"), Id_short);
        keywordLookup.put(keywords.get("super"), Id_super);
        keywordLookup.put(keywords.get("throw"), Id_throw);
        keywordLookup.put(keywords.get("catch"), Id_catch);
        keywordLookup.put(keywords.get("native"), Id_native);
        keywordLookup.put(keywords.get("delete"), Id_delete);
        keywordLookup.put(keywords.get("throws"), Id_throws);
        keywordLookup.put(keywords.get("import"), Id_import);
        keywordLookup.put(keywords.get("double"), Id_double);
        keywordLookup.put(keywords.get("static"), Id_static);
        keywordLookup.put(keywords.get("public"), Id_public);
        keywordLookup.put(keywords.get("export"), Id_export);
        keywordLookup.put(keywords.get("typeof"), Id_typeof);
        keywordLookup.put(keywords.get("package"), Id_package);
        keywordLookup.put(keywords.get("default"), Id_default);
        keywordLookup.put(keywords.get("finally"), Id_finally);
        keywordLookup.put(keywords.get("boolean"), Id_boolean);
        keywordLookup.put(keywords.get("private"), Id_private);
        keywordLookup.put(keywords.get("extends"), Id_extends);
        keywordLookup.put(keywords.get("abstract"), Id_abstract);
        keywordLookup.put(keywords.get("continue"), Id_continue);
        keywordLookup.put(keywords.get("debugger"), Id_debugger);
        keywordLookup.put(keywords.get("volatile"), Id_volatile);
        keywordLookup.put(keywords.get("interface"), Id_interface);
        keywordLookup.put(keywords.get("protected"), Id_protected);
        keywordLookup.put(keywords.get("transient"), Id_transient);
        keywordLookup.put(keywords.get("implements"), Id_implements);
        keywordLookup.put(keywords.get("instanceof"), Id_instanceof);
        keywordLookup.put(keywords.get("synchronized"), Id_synchronized);    
    }
    
    public static BabylTokenizer createEnglishTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                in, 
                ts, 
                new BabylTokenizer.DecimalNumberReader(),
                TranslatedNameBindings.getKeywordMap("en"));
    }
    public static BabylTokenizer createBengaliTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                in, 
                ts, 
                new BabylTokenizer.DecimalNumberReader('.', '\u09E6', '.'),
                TranslatedNameBindings.getKeywordMap("bn"));
    }
    public static BabylTokenizer createHindiTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                in, 
                ts, 
                new BabylTokenizer.DecimalNumberReader('.', '\u0966', '.'),
                TranslatedNameBindings.getKeywordMap("hi"));
    }
    public static BabylTokenizer createItalianTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                    in, 
                    ts, 
                    new BabylTokenizer.DecimalNumberReader(','),
                    TranslatedNameBindings.getKeywordMap("it")) {
                protected boolean isStringDelimiter(int ch)
                {
                    return (ch == '\'' || ch == '\"' || ch == '\u00AB' || ch == '\u2018' || ch == '\u2019' || ch == '\u201c' || ch == '\u201d');
                }
            };
    }
    public static BabylTokenizer createDutchTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                    in, 
                    ts, 
                    new BabylTokenizer.DecimalNumberReader(','),
                    TranslatedNameBindings.getKeywordMap("nl")) {
                protected boolean isStringDelimiter(int ch)
                {
                    return (ch == '\'' || ch == '\"'
                            || ch == '\u201a' || ch == '\u201e'
                            || ch == '\u2018' || ch == '\u2019' || ch == '\u201c' || ch == '\u201d');
                }
            };
    }
    public static BabylTokenizer createPolishTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                    in, 
                    ts, 
                    new BabylTokenizer.DecimalNumberReader(','),
                    TranslatedNameBindings.getKeywordMap("pl")) {
                protected boolean isStringDelimiter(int ch)
                {
                    return (ch == '\'' || ch == '\"'
                            || ch == '\u201a' || ch == '\u201e'
                            || ch == '\u00AB' || ch == '\u00BB'
                            || ch == '\u2039' || ch == '\u203a'
                            || ch == '\u2018' || ch == '\u2019' || ch == '\u201c' || ch == '\u201d');
                }
            };
    }
    public static BabylTokenizer createSwahiliTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                    in, 
                    ts, 
                    new BabylTokenizer.DecimalNumberReader('.'),
                    TranslatedNameBindings.getKeywordMap("sw"));
    }
    public static BabylTokenizer createMalayIndonesianTokenizer(ParserErrorReportingBase p, TokenCharStream in, TokenStream ts)
    {
        return new BabylGenericTokenizer(p, 
                    in, 
                    ts, 
                    new BabylTokenizer.DecimalNumberReader(',', '0', '.'),
                    TranslatedNameBindings.getKeywordMap("id"));
    }

}
