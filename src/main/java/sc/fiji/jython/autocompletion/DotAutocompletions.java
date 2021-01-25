/*-
 * #%L
 * Autocompletion for the jython language in the Script Editor
 * %%
 * Copyright (C) 2020 Albert Cardona
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
package sc.fiji.jython.autocompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.swing.script.autocompletion.CompletionText;

public interface DotAutocompletions {
	
	static public final DotAutocompletions EMPTY = new EmptyDotAutocompletions();
	
	default public String getClassname() {
		return null;
	}
	
	default public String getSummary() {
		return null;
	}

	public List<CompletionText> get();

	static public List<CompletionText> getPublicFieldsAndMethods(final String className) {
		final List<CompletionText> ac = new ArrayList<>();
		if (null != className) {
			try {
				final Class<?> c = Class.forName(className);
				for (final Field f: c.getFields())
					ac.add(new CompletionText(null, c, f));
				for (final Method m: c.getMethods())
					ac.add(new CompletionText(null, c, m)); // TODO could do a parameter-driven autocompletion
			} catch (final Exception e) {
				System.out.println("Could not load class " + className + " :: " + e.getMessage());
			}
		}
		return ac;
	}

}
