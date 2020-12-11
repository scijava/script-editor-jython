package sc.fiji.jython.autocompletion;

import java.util.List;

public class DefVarDotAutocompletions extends VarDotAutocompletions {

	final String fnName;
	final List<String> argumentNames;
	final Scope scope;
	
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
