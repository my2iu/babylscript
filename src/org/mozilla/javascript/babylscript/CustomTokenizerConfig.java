package org.mozilla.javascript.babylscript;

import java.util.HashMap;
import java.util.Map;

public class CustomTokenizerConfig 
{
    public CustomTokenizerConfig()
    {
        keywords = new HashMap<String, String>();
    }
    public Map<String, String> keywords;    
}
