/**
 * 
 */
package org.korsakow.ide.ui.components;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.JTextField;

public class TokenizerTextArea extends JTextField
{
	private Color focusBackgroundColor;
	private Color focusForegroundColor;
	private final Color defaultBackgroundColor;
	private final Color defaultForegroundColor;
	
	protected String canonicalDelimiter = ", ";
	
	public TokenizerTextArea()
	{
		defaultBackgroundColor = getBackground();
		defaultForegroundColor = getForeground();
		
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (focusBackgroundColor != null)
					setBackground(focusBackgroundColor);
				if (focusForegroundColor != null) {
					setForeground(focusForegroundColor);
					setCaretColor(focusForegroundColor);
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				if (focusBackgroundColor != null)
					setBackground(defaultBackgroundColor);
				if (focusForegroundColor != null) {
					setForeground(defaultForegroundColor);
					setCaretColor(focusForegroundColor);
				}
				
				Collection<String> tokens = getTokens();
				String text = buildTokenString(tokens);
				// this check avoids unnecessary dispatches of change events
				// which can wreak havock
				if (!text.equals(getText()))
					setText(text);
			}
		});
	}
	public void setCanonicalDelimiter(String deliminter)
	{
		canonicalDelimiter = deliminter;
	}
	
	protected String buildTokenString(Collection<String> tokens)
	{
		StringBuilder builder = new StringBuilder();
		for (String token : tokens)
			builder.append(token)
				.append(canonicalDelimiter);
	    
		return builder.toString();
	}
	
	public void setTokens(Collection<String> tokens)
	{
		String tokenString = buildTokenString(tokens);
		
		super.setText(tokenString);
	}
	public Collection<String> getTokens()
	{
		TreeSet<String> set = new TreeSet<>();
		StringTokenizer tokenizer = new StringTokenizer(getText(), " \t\r\n,");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			set.add(token);
		}
		return set;
	}
	public Color getFocusBackgroundColor()
	{
		return focusBackgroundColor;
	}
	public void setFocusBackgroundColor(Color focusBackgroundColor)
	{
		this.focusBackgroundColor = focusBackgroundColor;
	}
	public Color getFocusForegroundColor()
	{
		return focusForegroundColor;
	}
	public void setFocusForegroundColor(Color focusForegroundColor)
	{
		this.focusForegroundColor = focusForegroundColor;
	}
}