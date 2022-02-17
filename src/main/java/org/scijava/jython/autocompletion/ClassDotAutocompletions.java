/*-
 * #%L
 * Autocompletion for the jython language in the Script Editor
 * %%
 * Copyright (C) 2020 - 2022 Albert Cardona
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package org.scijava.jython.autocompletion;

import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.swing.script.autocompletion.CompletionText;

public class ClassDotAutocompletions extends DefVarDotAutocompletions {
	final List<String> superclassNames; // List of superclasses
	final List<CompletionText>dotAutocompletions; // List of class methods and fields
	
	/**
	 * 
	 * @param fnName
	 * @param superclassNames
	 * @param argumentNames
	 * @param dotAutocompletions
	 * @param class_scope
	 */
	public ClassDotAutocompletions(final String fnName, List<String> superclassNames,
			final List<String> argumentNames, final List<CompletionText> dotAutocompletions, final Scope class_scope) {
		super(fnName, null, argumentNames, class_scope);
		this.superclassNames = superclassNames;
		this.dotAutocompletions = dotAutocompletions;
	}
	
	@Override
	public List<CompletionText> get() {
		final List<CompletionText> ac = new ArrayList<>(this.dotAutocompletions);
		for (final String className: this.superclassNames) {
			List<CompletionText> fm = DotAutocompletions.getPublicFieldsAndMethods(className);
			if (fm.isEmpty()) {
				// Maybe it's a python class
				// get them all via the org.python.indexer.Indexer and Builtins classes, see StaticDotAutocompletions
				System.out.println("Don't know yet how to handle class " + className);
			}
			ac.addAll(fm);
		}
		return ac;
	}
	
	public void put(final CompletionText entry) {
		if (this.dotAutocompletions.contains(entry)) return; // list search OK: very low N
		this.dotAutocompletions.add(entry);
	}
	
	/** Make this be cda plus its own dotAutocompletions.
	 * 
	 * @param cda
	 */
	public void mutateIntoPlus(final ClassDotAutocompletions cda) 
	{
		this.className = cda.className;
		this.fnName = cda.fnName;
		this.superclassNames.clear();
		this.superclassNames.addAll(cda.superclassNames);
		this.dotAutocompletions.addAll(cda.dotAutocompletions);
		this.argumentNames.clear();
		this.argumentNames.addAll(cda.argumentNames);
		this.scope = cda.scope;
	}
	
	@Override
	public String toString() {
		return "ClassDotAutocompletions: " + this.fnName + "(" + String.join(", ", this.argumentNames) + ")" + " -- " + String.join(", ", this.get().toString());
	}
}
