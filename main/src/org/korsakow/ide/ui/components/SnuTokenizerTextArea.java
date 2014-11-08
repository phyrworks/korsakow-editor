package org.korsakow.ide.ui.components;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 *
 * @author phoenix
 */
public class SnuTokenizerTextArea extends TokenizerTextArea {
    
    public SnuTokenizerTextArea() {
	
    }
    
    @Override
    protected String buildTokenString(Collection<String> tokens) {
	
	StringBuilder builder = new StringBuilder();
	for (String token : tokens){
	    builder.append(token)
		    .append(canonicalDelimiter);
	}

	return builder.toString();
    }
        
    @Override
     public Collection<String> getTokens(){
	TreeSet<String> set = new TreeSet<>();
	StringTokenizer tokenizer = new StringTokenizer(getText(), " \t\r\n,");
	while (tokenizer.hasMoreTokens()) {
	    String token =  tokenizer.nextToken();
	    set.add(token);
	}
	return set;
    }   
}
