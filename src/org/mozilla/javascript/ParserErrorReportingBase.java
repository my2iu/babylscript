package org.mozilla.javascript;

public class ParserErrorReportingBase {

	// Exception to unwind
	    protected static class ParserException extends RuntimeException
	    {
	        static final long serialVersionUID = 5882582646773765630L;
	    }

	protected CompilerEnvirons compilerEnv;
	protected ErrorReporter errorReporter;
	protected String sourceURI;
	protected int syntaxErrorCount;
	protected TokenStream ts;

	public ParserErrorReportingBase() {
		super();
	}

	protected void addStrictWarning(String messageId, String messageArg) {
	    if (compilerEnv.isStrictMode())
	        addWarning(messageId, messageArg);
	}

	protected void addWarning(String messageId, String messageArg) {
	    String message = ScriptRuntime.getMessage1(messageId, messageArg);
	    if (compilerEnv.reportWarningAsError()) {
	        ++syntaxErrorCount;
	        errorReporter.error(message, sourceURI, ts.getLineno(),
	                            ts.getLine(), ts.getOffset());
	    } else
	        errorReporter.warning(message, sourceURI, ts.getLineno(),
	                              ts.getLine(), ts.getOffset());
	}

	protected void addError(String messageId) {
	    ++syntaxErrorCount;
	    String message = ScriptRuntime.getMessage0(messageId);
	    errorReporter.error(message, sourceURI, ts.getLineno(),
	                        ts.getLine(), ts.getOffset());
	}

	protected void addError(String messageId, String messageArg) {
	    ++syntaxErrorCount;
	    String message = ScriptRuntime.getMessage1(messageId, messageArg);
	    errorReporter.error(message, sourceURI, ts.getLineno(),
	                        ts.getLine(), ts.getOffset());
	}

	protected RuntimeException reportError(String messageId) {
	    addError(messageId);
	
	    // Throw a ParserException exception to unwind the recursive descent
	    // parse.
	    throw new ParserException();
	}

}