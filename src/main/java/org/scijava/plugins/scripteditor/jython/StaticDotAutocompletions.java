/*-
 * #%L
 * Autocompletion for the jython language in the Script Editor
 * %%
 * Copyright (C) 2020 - 2022 SciJava developers.
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
package org.scijava.plugins.scripteditor.jython;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.python.indexer.types.NModuleType;
import org.scijava.ui.swing.script.autocompletion.CompletionText;

public class StaticDotAutocompletions implements DotAutocompletions
{	
	final String className;
	public StaticDotAutocompletions(final String className) {
		this.className = className;
	}
	@Override
	public String getClassname() {
		return this.className;
	}
	@Override
	public List<CompletionText> get() {
		final List<CompletionText> ac = new ArrayList<>();
		if (null != this.className) {
			String msg = "";
			try {
				// Check first if it's a python module
				final NModuleType module = Scope.loadPythonModule(this.className); // Scope.indexer.getBuiltinModule(this.className);
				System.out.println("module is: " + module);
				if (null != module) {
					module.getTable().keySet().forEach( m -> ac.add(new CompletionText(m)));
					// Not need to remove: a file system watcher will do so when the module file is updated or deleted.
					// Scope.indexer.moduleTable.remove(this.className);
					return ac;
				} else {
					msg += "Not a python module: " + this.className;
				}
				// Or a java class:
				try {
					DotAutocompletions.staticFieldsAndStaticMethodsInto(Class.forName(this.className), ac);
					return ac;
				} catch (ClassNotFoundException cnfe) {
					msg += "\nCannot find java class " + this.className;
				}
				// Or a static method of a java class
				try {
					final int idot = this.className.lastIndexOf('.');
					final String name = this.className.substring(idot + 1);
					final Class<?> c = Class.forName(this.className.substring(0, idot));
					// There could be more than one method, with more than one return type
					for (final Class<?> r: Arrays.asList(c.getMethods()).stream()
							.filter(m -> m.getName().equals(name))
							.map(m -> m.getReturnType())
							.distinct()
							.collect(Collectors.toList())) {
						DotAutocompletions.fieldsAndMethodsInto(r, ac);
					}
					return ac;
				} catch (ClassNotFoundException cnfe) {
					msg += "\nCannot derive static method or field from " + this.className;
				}
				
				if (null != msg) {
					System.out.println(msg);
				}
			} catch (Exception e) {
				if (null != msg) System.out.println(msg);
				e.printStackTrace();
			}
		}
		return ac;
	}
	
	@Override
	public String toString() {
		final String completions = get().stream().map(c -> c.getReplacementText()).collect(Collectors.joining(",", "[", "]"));
		return "StaticDotAutocompletions: " + className + " -- " + completions;
	}
}
