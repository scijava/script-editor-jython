package sc.fiji.jython.autocompletion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Scope {
	final Scope parent;
	final List<Scope> children = new ArrayList<>();
	final HashMap<String, String> imports = new HashMap<>();
	final HashMap<String, String> vars = new HashMap<>();
	
	public Scope(final Scope parent) {
		this.parent = parent;
		if (null != parent) {
			parent.children.add(this);
			this.imports.putAll(parent.imports);
			this.vars.putAll(parent.vars);
		}
	}
	
	public Scope getLast() {
		if (children.isEmpty()) return this;
		return children.get(children.size() -1).getLast();
	}
	
	public void print(final String indent) {
		if ("" == indent) {
			for (final Map.Entry<String, String> e: imports.entrySet())
				System.out.println(indent + "import :: " + e.getKey() + " --> " + e.getValue());
			System.out.println("scope global:");
		}
		for (final Map.Entry<String, String> e: vars.entrySet()) {
			if (null != parent && parent.vars.containsKey(e.getKey())) continue; // to print only the newly added ones
			System.out.println(indent + "var :: " + e.getKey() + " = " + e.getValue());
		}
		
		int i = 0;
		for (final Scope child: children) {
			System.out.println(indent + "scope[" + (i++) + "]:");
			child.print(indent + "  ");
		}
	}
}