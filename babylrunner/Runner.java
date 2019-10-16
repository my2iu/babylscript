import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Locale;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;



public class Runner 
{
    ScriptableObject scope;
    
    public Runner()
    {
        Context cx = Context.enter();
        cx.setLocale(Locale.ENGLISH);
        cx.setOptimizationLevel(-1);
        try {
           scope = cx.initStandardObjects();
           cx.evaluateString(scope, "function alert(text) { java.lang.System.out.println(text); } this['fr':'alerter'] = this['ar':'\u062a\u062d\u0630\u064a\u0631'] = 'alert';", null, 0, null);
        } finally {
           Context.exit();
        }
    }
    
    public String runCode(String code, String filename) 
    {
       Context cx = Context.enter();
       cx.setOptimizationLevel(-1);
       try {
          return Context.toString(cx.evaluateString(scope, code, filename, 0, null));
       } finally {
          Context.exit();
       }
    }

    public void runFile(String filename) throws IOException
    {
        FileInputStream f = new FileInputStream(filename);
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
        
        runCode(str.toString(), filename);
    }
    
    public static void main(String[] args) throws IOException 
    {
        if (args.length != 1)
        {
            System.out.println(
                    "Run a Babylscript/JavaScript/ECMAScript program by \n" +
                    "providing the name of the file to run on the command \n " +
                    "line. The file should be text files in UTF-8 format. \n" +
                    "More than one file can be specified on the command, \n" +
                    "line and they will be executed sequentially. \n");
            return;
        }
        
        Runner runner = new Runner();
        for (String filename: args)
            runner.runFile(filename);
    }

}
