package org.mozilla.javascript.gwt;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.InterpretedFunction;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.debug.DebuggableScript;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

public class Test implements EntryPoint 
{

    static class AlertFunction extends BaseFunction
    {
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            if (args.length > 0)
                alertPassthrough("" + args[0]);
            return null;
        }
        public static native void alertPassthrough(String msg) /*-{
            $wnd.alert(msg);
        }-*/;
    }

    static class ContinuationTestFunction extends BaseFunction
    {
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            System.out.println("enter continuation");
            ContinuationPending pending = cx.captureSystemContinuation();
            throw pending;
        }
    }
    
    // TODO: This might be called fairly often, so it might be
    // useful to rewrite this so that it doesn't use exceptions
    // for continuations
    static class AsynchronousToBlockingFunction extends BaseFunction
    {
        // NOTE: This code is NOT reentrant, so don't call back into JavaScript
        
        // Override this with the code for starting the asynchronous operation
        protected void startCall(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            
        }
        
        // Override this with the code that checks if the asynchronous operation
        // is done. Return true iff the operation is done.
        protected boolean isCallDone(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            return true;
        }
        
        // After the operation is complete, this function will be executed
        // to get the result of the asynchronous operation
        protected Object getCallResult(Context cx, Scriptable scope, Scriptable thisObj, Object [] args)
        {
            return null;
        }
        
        Context savedCx;
        Scriptable savedScope;
        Scriptable savedThisObj;
        Object [] savedArgs;
        Object savedResult;
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            savedCx = cx;
            savedScope = scope;
            savedThisObj = thisObj;
            savedArgs = args;
            
            startCall(savedCx, savedScope, savedThisObj, savedArgs);
            if (!isCallDone()) 
            {
                ContinuationPending pending = cx.captureSystemContinuation();
                pending.setApplicationState(this);
                throw pending;
            }
            return getResult();
        }
        public boolean isCallDone()
        {
            boolean isDone = isCallDone(savedCx, savedScope, savedThisObj, savedArgs);
            if (isDone) savedResult = getCallResult(savedCx, savedScope, savedThisObj, savedArgs); 
            return isDone;
        }
        public Object getResult()
        {
            return savedResult;
        }
        
    }
    
    public static class AsynchronousBlockingReturns extends JavaScriptObject
    {
        protected AsynchronousBlockingReturns() {}
        final public native boolean isDone() /*-{
            return this.done();
        }-*/;
        final public native JavaScriptObject result() /*-{
            return this.result();
        }-*/; 
    }
    
    public static class SleepFunction extends AsynchronousToBlockingFunction
    {
        protected void startCall(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            fns = sleep(5000).cast();
        }
        protected boolean isCallDone(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            return fns.isDone();
        }
        protected Object getCallResult(Context cx, Scriptable scope, Scriptable thisObj, Object [] args)
        {
            return fns.result();
        }
        AsynchronousBlockingReturns fns;
        public static native JavaScriptObject sleep(int millis) /*-{
            var isDone = false;
            var toReturn = null;
            var handle = $wnd.setTimeout(function() {
                    isDone = true;
                }, millis);
            return {
                    done: function() { return isDone; },
                    result: function() { return toReturn; }
                };
        }-*/;
    }
    
    Context cx;
    Scriptable scope;
    
    public void onModuleLoad() 
    {
        cx = Context.enter();
        cx.setOptimizationLevel(-1);
        
        String result = "";
        try {
            scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, null, "alert", new AlertFunction());
            ScriptableObject.putProperty(scope, null, "continuationTest", new ContinuationTestFunction());
            ScriptableObject.putProperty(scope, null, "sleep", new SleepFunction());
//            result = Context.toString(cx.evaluateString(scope, "22", "<test>", 0, null));

            CompilerEnvirons compilerEnv = new CompilerEnvirons(); 
            Parser p = new Parser(compilerEnv, null);
//            if (returnFunction) {
//                p.calledByCompileFunction = true;
//            }
//             tree;
//            String sourceString = "22 + 23; alert('hi'); continuationTest()";
            String sourceString = "for (n = 0; n < 100; n++) if ((n % 10) == 9) alert(n);";
            int lineno = 0;
            String sourceName = "hey";
            if (sourceString != null) {
                ScriptOrFnNode tree = p.parse(sourceString, sourceName, lineno);
                Interpreter compiler = new Interpreter();

                String encodedSource = p.getEncodedSource();

                Object bytecode = compiler.compile(compilerEnv,
                                                   tree, encodedSource,
                                                   false);

                InterpretedFunction resultScript;
                resultScript = (InterpretedFunction) compiler.createScriptObject(bytecode, null);
                cx.setTimeSliceSize(10);
                try {
                    result = "" + cx.executeScriptWithContinuations(resultScript, scope);
                } catch (ContinuationPending pending) {
                    if (pending.getApplicationState() instanceof AsynchronousToBlockingFunction)
                    {
                        restartContinuationInTimer(pending);
                    }
                    else if (pending.getApplicationState() instanceof Context.TimeSliceExpiredClass)
                    {
                        restartTimeSliceInTimer(pending);
                    }
                    else
                        result = "" + cx.resumeContinuation(pending.getContinuation(), scope, 5);
                }
//                result = "" + resultScript.exec(cx, scope);

            } 

            
//public final Object evaluateString(Scriptable scope, String source,
//                    String sourceName, int lineno,
//                    Object securityDomain)
//{
//Script script = compileString(source, sourceName, lineno,
//                   securityDomain);
//if (script != null) {
//return script.exec(this, scope);
//} else {
//return null;
//}
//}
            
        } finally {
            Context.exit();
        }

        
        Element el = Document.get().getElementById("fillin");
        el.setInnerHTML(result);
    }

    void restartTimeSliceInTimer(final ContinuationPending pending)
    {
        Timer t = new Timer() {
            public void run() {
                try {
                    cx.resumeContinuation(pending.getContinuation(), scope, pending.getApplicationState());
                } catch (ContinuationPending pending) {
                    if (pending.getApplicationState() instanceof Context.TimeSliceExpiredClass)
                    {
                        restartTimeSliceInTimer(pending);
                    }
                }
            }
        };
        t.schedule(1);
    }

    void restartContinuationInTimer(final ContinuationPending pending)
    {
        Timer t = new Timer() {
            public void run() {
                AsynchronousToBlockingFunction async = (AsynchronousToBlockingFunction)pending.getApplicationState();
                if (async.isCallDone())
                {
                    Window.alert("" + cx.resumeContinuation(pending.getContinuation(), scope, async.getResult()));
                }
                else
                    Window.alert("nothing");
            }
        };
        t.schedule(6000);

    }

}
