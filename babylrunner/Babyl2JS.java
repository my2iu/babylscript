import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Locale;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;



public class Babyl2JS 
{
    public Babyl2JS()
    {
    }
    
    public String translateCode(String code, String filename, boolean useHeaders) 
    {
       Context cx = Context.enter();
       cx.setLocale(Locale.ENGLISH);
       cx.setOptimizationLevel(-1);
       String result = "";
       try {
           result = cx.compileStringToJS(code, filename, 0, useHeaders);
       } finally {
          Context.exit();
       }
       return result;
    }

    public void translateFile(String inFile, String outFile, boolean useHeaders, boolean useHTML) throws IOException
    {
        FileInputStream f = new FileInputStream(inFile);
        InputStreamReader reader = new InputStreamReader(f, "UTF-8");
        
        StringWriter str = new StringWriter();
        char [] buf = new char[1024];
        int numRead = reader.read(buf);
        while (numRead >= 0)
        {
            str.write(buf, 0, numRead);
            numRead = reader.read(buf);
        }
        
        reader.close();
        f.close();
        
        String js = translateCode(str.toString(), inFile, useHeaders);
        
        if (useHTML)
        {
            js = "<!DOCTYPE html>"
                    + "<head>"
                    + "<META http-equiv=\"Content-Type\" content=\"text/html;\" charset=\"UTF-8\">"
                    + "</head>"
                    + "<script>" + js + "</script>";
        }
        
        FileOutputStream outStream = new FileOutputStream(outFile);
        OutputStreamWriter out = new OutputStreamWriter(outStream, "UTF-8");
        out.write(js);
        out.close();
        outStream.close();
    }
    
    static void outputMessage()
    {
        System.out.println(
                "   java -jar babyl2js.jar [-noheaders] [-html] input-Babylscript-file output-JS-file\n" +
                "Translates a Babylscript program into regular \n" +
                "JavaScript code that can be run in a browser. \n " +
                "Specify the name of the file to be translated \n" +
                "as the first argument. The translated JavaScript \n" +
                "code will be stored in the file specified by the \n" +
                "second argument. The input file should be a text \n" +
                "file in UTF-8 format. The translator takes an \n" +
                "optional argument \"-noheaders\" which tells \n" +
                "the translator not to put Babylscript header\n" +
                "code in the file (the header code only needs to\n" +
                "be included in the first JavaScript file of a \n" +
                "program). It also takes an argument \"-html\"\n" +
                "which tells the translator to put <script>..</script>\n" +
                "tags around the output so that the resulting\n" +
                "output can be run directly in a browser.");
    }
    
    public static void main(String[] args) throws IOException 
    {
        boolean noHeaders = false;
        boolean useHTML = false;
        String inFile = null;
        String outFile = null;
        for (String arg: args)
        {
            if (arg.equals("-noheaders"))
                noHeaders = true;
            else if (arg.equals("-html"))
                useHTML = true;
            else if (inFile == null)
                inFile = arg;
            else if (outFile == null)
                outFile = arg;
            else
            {
                outputMessage();
                return;
            }
        }
        
        if (outFile == null)
        {
            outputMessage();
            return;
        }
        
        Babyl2JS translator = new Babyl2JS();
        translator.translateFile(inFile, outFile, !noHeaders, useHTML);
    }

}
