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
package org.scijava.jython.autocompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.swing.script.autocompletion.CompletionText;

public class VarDotAutocompletions implements DotAutocompletions {
	String className;
	public VarDotAutocompletions(final String className) {
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
			try {
				final Class<?> c = Class.forName(this.className);
				for (final Field f: c.getFields())
					ac.add(new CompletionText(f.getName(), c, f));
				for (final Method m: c.getMethods())
					ac.add(new CompletionText(m.getName() + "()", c, m)); // TODO could do a parameter-driven autocompletion
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ac;
	}

	@Override
	public String toString() {
		return "VarDotAutocompletions: " + this.className;
	}
}
