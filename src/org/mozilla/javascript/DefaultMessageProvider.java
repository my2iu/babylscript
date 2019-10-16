package org.mozilla.javascript;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import java.util.Locale;
import org.mozilla.javascript.ScriptRuntime.MessageProvider;
import org.mozilla.javascript.resources.Messages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

/* OPT there's a noticable delay for the first error!  Maybe it'd
 * make sense to use a ListResourceBundle instead of a properties
 * file to avoid (synchronized) text parsing.
 */
class DefaultMessageProvider implements MessageProvider {
    public String getMessage(String messageId, Object[] arguments) {
        Messages errMsgs = GWT.create(Messages.class); 
//        final String defaultResource
//            = "org.mozilla.javascript.resources.Messages";
//
//        Context cx = Context.getCurrentContext();
//        Locale locale = cx != null ? cx.getLocale() : Locale.getDefault();
//
//        // ResourceBundle does caching.
//        ResourceBundle rb = ResourceBundle.getBundle(defaultResource, new java.util.Locale(locale.getCountry()));

        messageId = messageId.replace(".", "_");
        String formatString = errMsgs.getString(messageId);

        /*
         * It's OK to format the string, even if 'arguments' is null;
         * we need to format it anyway, to make double ''s collapse to
         * single 's.
         */
        return simpleFormat(formatString, arguments);
    }
    
    private String simpleFormat(String messageId, Object[] arguments)
    {
        // in GWT, there is no MessageFormat object, but the Rhino error messages only
        // need simple string substitution, so we can just emulate that
        if (arguments != null)
        {
            for (int n = 0; n < arguments.length; n++)
                messageId = messageId.replace("{" + n + "}", (String)arguments[n]);
        }
        
        // Java has some complicated rules where '' is replaced by ' but only sometimes.
        // (And they are sometimes replaced by " maybe). Here, I'll just do a simple
        // substitution to handle apostrophes in French and other languages
        messageId = messageId.replace("''", "'");
        return messageId;
    }
    
}