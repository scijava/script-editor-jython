/*-
 * #%L
 * Autocompletion for the jython language in the Script Editor
 * %%
 * Copyright (C) 2020 - 2021 Albert Cardona
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

import java.util.List;

public class DefVarDotAutocompletions extends VarDotAutocompletions {

	String fnName;
	final List<String> argumentNames;
	Scope scope;
	
	public DefVarDotAutocompletions(final String fnName, final String returnClassName, final List<String> argumentNames, final Scope scope) {
		super(returnClassName);
		this.fnName = fnName;
		this.argumentNames = argumentNames;
		this.scope = scope;
	}
	
	public List<String> getArgumentNames() {
		return this.argumentNames;
	}
	
	@Override
	public String toString() {
		return "DefVarAutocompletions:" +
				"  Class: " + super.className +
				"  Arguments: " + String.join(", ", this.argumentNames);
	}
}
