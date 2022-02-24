package org.scijava.plugins.scripteditor.jython;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.scijava.ui.swing.script.autocompletion.CompletionText;

public class ConstructorAutocompletions implements DotAutocompletions
{
	private final Class<?> c;
	
	public ConstructorAutocompletions(final Class<?> c) {
		this.c = c;
	}
	
	@Override
	public List<CompletionText> get() {
		return Arrays.asList(this.c.getConstructors()).stream()
				.map(cons -> new CompletionText(c.getSimpleName(), c, cons))
				.collect(Collectors.toList());
	}

}
