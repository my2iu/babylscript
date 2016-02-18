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

package org.mozilla.javascript;

import java.io.*;

import org.mozilla.javascript.babylscript.ArabicTokenizer;
import org.mozilla.javascript.babylscript.BabylGenericTokenizer;
import org.mozilla.javascript.babylscript.ChineseTokenizer;
import org.mozilla.javascript.babylscript.CustomTokenizer;
import org.mozilla.javascript.babylscript.CustomTokenizerConfig;
import org.mozilla.javascript.babylscript.EsperantoTokenizer;
import org.mozilla.javascript.babylscript.FrenchTokenizer;
import org.mozilla.javascript.babylscript.GermanTokenizer;
import org.mozilla.javascript.babylscript.JapaneseTokenizer;
import org.mozilla.javascript.babylscript.KoreanTokenizer;
import org.mozilla.javascript.babylscript.PortugueseTokenizer;
import org.mozilla.javascript.babylscript.RomanianTokenizer;
import org.mozilla.javascript.babylscript.RussianTokenizer;
import org.mozilla.javascript.babylscript.SpanishTokenizer;
import org.mozilla.javascript.babylscript.TurkishTokenizer;


/**
 * This class implements the JavaScript scanner.
 *
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @see org.mozilla.javascript.Parser
 *
 * @author Mike McCabe
 * @author Brendan Eich
 */

public class TokenStream
{
    TokenStream(ParserErrorReportingBase parser, Reader sourceReader, String sourceString,
                int lineno, LanguageMode startLanguageMode, CustomTokenizerConfig customLanguageConfig)
    {
        this.parser = parser;
        this.in = new TokenCharStream(sourceReader, sourceString, lineno);
        this.englishTokenizer = BabylGenericTokenizer.createEnglishTokenizer(parser, in, this);
        this.customLanguageConfig = customLanguageConfig; 
        xmlTokenizer = new XMLTokenizer(parser, in, this);
        if (startLanguageMode == null) startLanguageMode = LanguageMode.en;
        setLanguage(startLanguageMode);
    }

    /* This function uses the cached op, string and number fields in
     * TokenStream; if getToken has been called since the passed token
     * was scanned, the op or string printed may be incorrect.
     */
    String tokenToString(int token)
    {
        if (Token.printTrees) {
            String name = Token.name(token);

            switch (token) {
            case Token.STRING:
            case Token.REGEXP:
            case Token.NAME:
                return name + " `" + this.string + "'";

            case Token.NUMBER:
                return "NUMBER " + this.number;
            }

            return name;
        }
        return "";
    }

    static boolean isKeyword(String s)
    {
        return Token.EOF != BabylTokenizer.englishStringToKeyword(s);
    }

    final int getLineno() { return in.lineno; }

    final String getString() { return string; }

    final double getNumber() { return number; }

    final boolean eof() { return in.hitEOF; }

    final int getOffset() { return in.getOffset(); }

    final String getLine() { return in.getLine(); }


    final int getToken() throws IOException
    {
        int token = Token.LANGMODE;
        while (token == Token.LANGMODE)
        {
            token = currentTokenizer.getToken();
        }
        return token; 
    }

    private static boolean isAlpha(int c)
    {
        // Use 'Z' < 'a'
        if (c <= 'Z') {
            return 'A' <= c;
        } else {
            return 'a' <= c && c <= 'z';
        }
    }

    /* As defined in ECMA.  jsscan.c uses C isspace() (which allows
     * \v, I think.)  note that code in getChar() implicitly accepts
     * '\r' == \u000D as well.
     */
    static boolean isJSSpace(int c)
    {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        } else {
            return c == 0xA0
                || Character.getType((char)c) == Character.SPACE_SEPARATOR;
        }
    }

    private static boolean isJSFormatChar(int c)
    {
        return c > 127 && Character.getType((char)c) == Character.FORMAT;
    }

    /**
     * Parser calls the method when it gets / or /= in literal context.
     */
    void readRegExp(int startToken)
        throws IOException
    {
        stringBufferTop = 0;
        if (startToken == Token.ASSIGN_DIV) {
            // Miss-scanned /=
            addToString('=');
        } else {
            if (startToken != Token.DIV) Kit.codeBug();
        }

        boolean inCharSet = false; // true if inside a '['..']' pair
        int c;
        while ((c = in.getChar()) != '/' || inCharSet) {
            if (c == '\n' || c == TokenCharStream.EOF_CHAR) {
                in.ungetChar(c);
                throw parser.reportError("msg.unterminated.re.lit");
            }
            if (c == '\\') {
                addToString(c);
                c = in.getChar();
            } else if (c == '[') {
                inCharSet = true;
            } else if (c == ']') {
                inCharSet = false;
            }
            addToString(c);
        }
        int reEnd = stringBufferTop;

        while (true) {
            if (in.matchChar('g'))
                addToString('g');
            else if (in.matchChar('i'))
                addToString('i');
            else if (in.matchChar('m'))
                addToString('m');
            else
                break;
        }

        if (isAlpha(in.peekChar())) {
            throw parser.reportError("msg.invalid.re.flag");
        }

        this.string = new String(stringBuffer, 0, reEnd);
        this.regExpFlags = new String(stringBuffer, reEnd,
                                      stringBufferTop - reEnd);
    }

    boolean isXMLAttribute() 
    {
        return xmlTokenizer.isXMLAttribute();
    }

    int getFirstXMLToken() throws IOException
    {
        return xmlTokenizer.getFirstXMLToken();
    }

    int getNextXMLToken() throws IOException
    {
        return xmlTokenizer.getNextXMLToken();
    }

    private String getStringFromBuffer()
    {
        return new String(stringBuffer, 0, stringBufferTop);
    }

    private void addToString(int c)
    {
        int N = stringBufferTop;
        if (N == stringBuffer.length) {
            char[] tmp = new char[stringBuffer.length * 2];
            System.arraycopy(stringBuffer, 0, tmp, 0, N);
            stringBuffer = tmp;
        }
        stringBuffer[N] = (char)c;
        stringBufferTop = N + 1;
    }

    public static enum LanguageMode
    {
        ar,
        bn,
        de,
        en,
	eo,
        es,
        fr,
        hi,
        id,
        it,
        ja,
        ko,
        nl,
        pl,
        pt,
        test,
        ro,
        ru,
        sw,
        tr,
        zh
    }
    public static LanguageMode stringToLanguageMode(String str)
    {
        if (str == null)
            return LanguageMode.en;
        if ("ar".equals(str))
            return LanguageMode.ar;
        else if ("bn".equals(str))
            return LanguageMode.bn;
        else if ("de".equals(str))
            return LanguageMode.de;
        else if ("en".equals(str))
            return LanguageMode.en;
        else if ("eo".equals(str))
            return LanguageMode.eo;
        else if ("es".equals(str))
            return LanguageMode.es;
        else if ("fr".equals(str))
            return LanguageMode.fr;
        else if ("hi".equals(str))
            return LanguageMode.hi;
        else if ("id".equals(str))
            return LanguageMode.id;
        else if ("it".equals(str))
            return LanguageMode.it;
        else if ("ja".equals(str))
            return LanguageMode.ja;
        else if ("ko".equals(str))
            return LanguageMode.ko;
        else if ("nl".equals(str))
            return LanguageMode.nl;
        else if ("pl".equals(str))
            return LanguageMode.pl;
        else if ("pt".equals(str))
            return LanguageMode.pt;
        else if ("ro".equals(str))
            return LanguageMode.ro;
        else if ("ru".equals(str))
            return LanguageMode.ru;
        else if ("sw".equals(str))
            return LanguageMode.sw;
        else if ("tr".equals(str))
            return LanguageMode.tr;
        else if ("zh".equals(str))
            return LanguageMode.zh;
        else if ("test".equals(str))
            return LanguageMode.test;
        return LanguageMode.en;
    }
    public static String languageModeToString(LanguageMode lang)
    {
        switch(lang)
        {
        case ar:
            return "ar";
        case bn:
            return "bn";
        case de:
            return "de";
        case en:
            return "en";
        case eo:
            return "eo";
        case es:
            return "es";
        case fr:
            return "fr";
        case hi:
            return "hi";
        case id:
            return "id";
        case it:
            return "it";
        case ja:
            return "ja";
        case ko:
            return "ko";
        case nl:
            return "nl";
        case pl:
            return "pl";
        case pt:
            return "pt";
        case ro:
            return "ro";
        case ru:
            return "ru";
        case sw:
            return "sw";
        case tr:
            return "tr";
        case zh:
            return "zh";
        case test:
            return "test";
        }
        return null;
    }
    public void setLanguage(LanguageMode language)
    {
        languageMode = language;
        switch(language)
        {
        case ar:
            currentTokenizer = new ArabicTokenizer(parser, in, this);
            break;
        case bn:
            currentTokenizer = BabylGenericTokenizer.createBengaliTokenizer(parser, in, this);
            break;
        case de:
            currentTokenizer = new GermanTokenizer(parser, in, this);
            break;
        case en:
            currentTokenizer = this.englishTokenizer;
        case eo:
            currentTokenizer = new EsperantoTokenizer(parser, in, this);
            break;
        case es:
            currentTokenizer = new SpanishTokenizer(parser, in, this);
            break;
        case fr:
            currentTokenizer = new FrenchTokenizer(parser, in, this);
            break;
        case hi:
            currentTokenizer = BabylGenericTokenizer.createHindiTokenizer(parser, in, this);
            break;
        case id:
            currentTokenizer = BabylGenericTokenizer.createMalayIndonesianTokenizer(parser, in, this);
            break;
        case it:
            currentTokenizer = BabylGenericTokenizer.createItalianTokenizer(parser, in, this);
            break;
        case ja:
            currentTokenizer = new JapaneseTokenizer(parser, in, this);
            break;
        case ko:
            currentTokenizer = new KoreanTokenizer(parser, in, this);
            break;
        case nl:
            currentTokenizer = BabylGenericTokenizer.createDutchTokenizer(parser, in, this);
            break;
        case pl:
            currentTokenizer = BabylGenericTokenizer.createPolishTokenizer(parser, in, this);
            break;
        case pt:
            currentTokenizer = new PortugueseTokenizer(parser, in, this);
            break;
        case ro:
            currentTokenizer = new RomanianTokenizer(parser, in, this);
            break;
        case ru:
            currentTokenizer = new RussianTokenizer(parser, in, this);
            break;
        case sw:
            currentTokenizer = BabylGenericTokenizer.createSwahiliTokenizer(parser, in, this);
            break;
        case tr:
            currentTokenizer = new TurkishTokenizer(parser, in, this);
            break;
        case zh:
            currentTokenizer = new ChineseTokenizer(parser, in, this);
            break;
        case test:
            this.currentTokenizer = new CustomTokenizer(parser, in, this, customLanguageConfig);
            break;
        }
    }

    public String getLastLanguageString()
    {
        return languageModeToString(languageMode);
    }

    String regExpFlags;

    // Set this to an initial non-null value so that the Parser has
    // something to retrieve even if an error has occurred and no
    // string is found.  Fosters one class of error, but saves lots of
    // code.
    String string = "";
    double number;

    private char[] stringBuffer = new char[128];
    private int stringBufferTop;
    ObjToIntMap allStrings = new ObjToIntMap(50);

    private TokenCharStream in;

    private LanguageMode languageMode = LanguageMode.en;
    private BabylTokenizer currentTokenizer;
    private BabylTokenizer englishTokenizer;
    private CustomTokenizerConfig customLanguageConfig;
    private XMLTokenizer xmlTokenizer;
    
    private ParserErrorReportingBase parser;
}
