package sc.fiji.jython.autocompletion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Scope {
	final Scope parent;
	final boolean is_class;
	final List<Scope> children = new ArrayList<>();
	final HashMap<String, DotAutocompletions> imports = new HashMap<>();
	final HashMap<String, DotAutocompletions> vars = new HashMap<>();
	
	public Scope(final Scope parent) {
		this(parent, false);
	}
	
	public Scope(final Scope parent, final boolean is_class) {
		this.parent = parent;
		if (null != parent) {
			parent.children.add(this);
			//this.imports.putAll(parent.imports);
			//this.vars.putAll(parent.vars);
		}
		this.is_class = is_class;
	}
	
	public DotAutocompletions find(final String name, final DotAutocompletions default_value) {
		Scope scope = this;
		while (null != scope) {
			DotAutocompletions da = scope.vars.get(name);
			if (null == da)
				da = scope.imports.get(name);
			if (null != da) return da;
			scope = scope.parent;
		}
		return default_value;
	}
	
	public HashMap<String, DotAutocompletions> getImports() {
		final HashMap<String, DotAutocompletions> imports = new HashMap<>(this.imports);
		Scope parent = this.parent;
		while (parent != null) {
			imports.putAll(parent.getImports());
			parent = parent.parent;
		}
		return imports;
	}
	
	public HashMap<String, DotAutocompletions> getVars() {
		final HashMap<String, DotAutocompletions> vars = new HashMap<>(this.vars);
		Scope parent = this.parent;
		while (parent != null) {
			vars.putAll(parent.getVars());
			parent = parent.parent;
		}
		return vars;
	}
	
	public boolean isClass() {
		return this.is_class;
	}
	
	public Scope getLast() {
		if (children.isEmpty()) return this;
		return children.get(children.size() -1).getLast();
	}
	
	/*
	public void putFunctionDef(final List<String> def) {
		if (is_class) {
			// TODO: add as a possible autocompletion of "self"
			if (this.vars.containsKey("self")) {
				
			}
		} else {
			this.vars.put(def.get(0), def.subList(1, def.size()).stream().reduce((a,  s) -> a + "," + s).get());
		}
	}
	*/
	
	public void print(final String indent) {
		if ("" == indent) {
			for (final Map.Entry<String, DotAutocompletions> e: imports.entrySet())
				System.out.println(indent + "import :: " + e.getKey() + " --> " + e.getValue());
			System.out.println("scope global:");
		}
		for (final Map.Entry<String, DotAutocompletions> e: vars.entrySet()) {
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