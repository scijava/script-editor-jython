package sc.fiji.jython.autocompletion;

import java.util.List;

public class DefVarAutocompletions extends VarDotAutocompletions {

	final List<String> argumentNames;
	
	public DefVarAutocompletions(final String returnClassName, final List<String> argumentNames) {
		super(returnClassName);
		this.argumentNames = argumentNames;
	}
	
	public List<String> getArgumentNames() {
		return this.argumentNames;
	}
	
	@Override
	public String toString() {
		return "DefVarAutocompletions: \n" +
				"  Class: " + super.className +
				"  Arguments: " + String.join(", ", this.argumentNames);
	}
}
