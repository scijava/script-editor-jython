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

public interface DotAutocompletions {
	
	static public final DotAutocompletions EMPTY = new EmptyDotAutocompletions();
	
	default public String getClassname() {
		return null;
	}
	
	public List<String> get();
	
	static public List<String> getPublicFieldsAndMethods(final String className) {
		final List<String> ac = new ArrayList<>();
		if (null != className) {
			try {
				final Class<?> c = Class.forName(className);
				for (final Field f: c.getFields())
					ac.add(f.getName());
				for (final Method m: c.getMethods())
					ac.add(m.getName()); // TODO could do a parameter-driven autocompletion
			} catch (Exception e) {
				System.out.println("Could not load class " + className + " :: " + e.getMessage());
			}
		}
		return ac;
	}
}
