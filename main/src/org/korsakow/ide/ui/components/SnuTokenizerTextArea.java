/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.korsakow.ide.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.dsrg.soenea.domain.command.CommandException;
import org.korsakow.ide.Application;
import org.korsakow.mappingplugin.IMap;
import org.korsakow.mappingplugin.MapUtils;

/**
 *
 * @author phoenix
 */
public class SnuTokenizerTextArea extends TokenizerTextArea {
    
    public SnuTokenizerTextArea() {
	
    }
    
    @Override
    protected String buildTokenString(Collection<String> tokens) {
	
	/* MAPPING PLUGIN */
	List<IMap> maps = new ArrayList<>();
	
	StringBuilder builder = new StringBuilder();
	for (String token : tokens){
	    try {
		String newToken =  MapUtils.processLOC(token, maps); /* MAPPING PLUGIN */
		
		//if the newToken length is 0, then it is a LOC that doesn't exist
		//in the map.  So we remove it.
		if (newToken.length() == 0)
		    continue;
		
		builder.append(newToken)
			.append(canonicalDelimiter);
	    } catch (CommandException e) {
		Application.getInstance().showUnhandledErrorDialog(e);
	    }
	}

	return builder.toString();

    }
        
    @Override
     public Collection<String> getTokens(){
	TreeSet<String> set = new TreeSet<>();
	StringTokenizer tokenizer = new StringTokenizer(getText(), " \t\r\n,");
	while (tokenizer.hasMoreTokens()) {
	    try {
		String token =  MapUtils.unprocessLOC(tokenizer.nextToken()); /* MAPPING PLUGIN */
		
		//if the newToken length is 0, then it is a LOC that doesn't exist
		//in the map.  So we remove it.
		if (token.length() == 0)
		    continue;
		
		set.add(token);
	    } catch (CommandException e) {
		Application.getInstance().showUnhandledErrorDialog(e);
	    }
	}
	return set;
    }   
}
