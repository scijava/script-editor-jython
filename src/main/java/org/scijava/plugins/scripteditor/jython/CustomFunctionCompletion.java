package org.scijava.plugins.scripteditor.jython;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;

public class CustomFunctionCompletion extends FunctionCompletion
{
	public CustomFunctionCompletion(
			final CompletionProvider provider,
			final String name,
			final String returnType) {
		super(provider, name, returnType);
	}
	
	@Override
	public String getName() {
		return super.getName() + "()"; // for listing with fields and methods
	}
	
	@Override
	public String getDefinitionString() {
		StringBuilder sb = new StringBuilder();
	
		// Add the return type if applicable (C macros like NULL have no type).
		String type = getType();
		if (type!=null) {
			sb.append(type).append(' ');
		}
	
		// Add the item being described's name.
		sb.append(super.getName()); // without parentheses: will be added below with arguments inside
	
		// Add parameters for functions.
		CompletionProvider provider = getProvider();
		char start = provider.getParameterListStart();
		if (start!=0) {
			sb.append(start);
		}
		for (int i=0; i<getParamCount(); i++) {
			Parameter param = getParam(i);
			type = param.getType();
			String name = param.getName();
			if (type!=null) {
				sb.append(type);
				if (name!=null) {
					sb.append(' ');
				}
			}
			if (name!=null) {
				sb.append(name);
			}
			if (i<super.getParamCount()-1) {
				sb.append(provider.getParameterListSeparator());
			}
		}
		char end = provider.getParameterListEnd();
		if (end!=0) {
			sb.append(end);
		}
	
		return sb.toString();
	}
	
	@Override
	public String getSummary() {
		// Return the CompletionText.getSummary() text, made by ClassUtil.getSummaryCompletion
		// (In principle, the latter is a shortcut to the whole mechanism in superclass FunctionCompletion, but that's fine.)
		return super.getShortDescription();
	}
	
}
