package sc.fiji.jython.autocompletion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.antlr.PythonTree;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Tuple;
import org.python.antlr.base.mod;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.ParserFacade;

public class AutoCompletionUtil {
	
	static public String testCode = String.join("\n",
			"from ij import IJ, ImageJ as IJA, VirtualStack",
			"from ij.process import ByteProcessor",
			"imp = IJ.getImage()",
			"width, height = imp.getWidth(), imp.getHeight()",
			"imp2 = imp",
			"class Volume(VirtualStack):",
			"  def getProcessor(self, index):",
			"    return ByteProcessor(512, 512)",
			"  def getSize(self):",
			"    return 10",
			"def setRoi(an_imp):",
			"  ip = an_imp.getStack().getProcessor(3)",
			"  pixels = ip.");
	
	static public class Scope {
		final Scope parent;
		final List<Scope> children = new ArrayList<>();
		final HashMap<String, String> imports = new HashMap<>();
		final HashMap<String, String> vars = new HashMap<>();
		
		public Scope(final Scope parent) {
			this.parent = parent;
			if (null != parent) {
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
	
	/**
	 * Returns the top-level Scope. 
	 */
	static public Scope parseAST(final String code) {
		// The code includes from beginning of the file until the point at which an autocompletion is requested.
		// Therefore, remove the last line, which would fail to parse because it is incomplete
		final int lastLineBreak = code.lastIndexOf("\n");
		final String codeToParse = code.substring(0, lastLineBreak);
		final mod m = ParserFacade.parse(codeToParse, CompileMode.exec, "<none>", new CompilerFlags());

		return parseNode(m.getChildren(), null);
	}
	
	static public Scope parseNode(final List<PythonTree> children, final Scope parent) {
		
		final Scope scope = new Scope(parent);
		if (null != parent)
			parent.children.add(scope);
		
		for (final PythonTree child : children) {
			print(child.getClass());
			
			if (child instanceof ImportFrom)
				scope.imports.putAll(parseImportStatement( (ImportFrom)child ));
			else if (child instanceof Assign)
				scope.vars.putAll(parseAssignStatement( (Assign)child, scope ));
			else if (child instanceof FunctionDef)
				scope.vars.put(parseFunctionDef( (FunctionDef)child, scope), null); // no value: no class. TODO return the list of arguments, for autocompletion.
			else if (child instanceof ClassDef)
				scope.vars.put(parseClassDef( (ClassDef)child, scope), null); // TODO no value, but should have one, too look into its methods and implemented interfaces or superclasses
		}
		
		return scope;
		// Prints the top code blocks
		// class org.python.antlr.ast.ImportFrom
		// class org.python.antlr.ast.ImportFrom
		// class org.python.antlr.ast.Assign
		// class org.python.antlr.ast.Assign
		// class org.python.antlr.ast.ClassDef
		// class org.python.antlr.ast.FunctionDef
	}
	
	/**
	 * Parse import statements, considering aliases.
	 * @param im
	 * @return
	 */
	static public Map<String, String> parseImportStatement(final ImportFrom im) {
		final Map<String, String> classes = new HashMap<>();
		final String module = im.getModule().toString();
		for (int i=0; i<im.getNames().__len__(); ++i) {
			final String alias = im.getInternalNames().get(i).getAsname().toString(); // alias: as name
			final String simpleClassName = im.getInternalNames().get(i).getInternalName(); // class name
			classes.put("None" == alias ? simpleClassName : alias, module + "." + simpleClassName);
		}
		return classes;
	}
	
	static public Map<String, String> parseAssignStatement(final Assign assign, final Scope scope) {
		final Map<String, String> assigns = new HashMap<>();
		//final expr right = assign.getInternalValue(); // strangely this works
		final PythonTree right = assign.getChildren().get(1);
		if (right instanceof Tuple || right instanceof org.python.antlr.ast.List) { // TODO are there any other possible?
			final PythonTree left = assign.getChildren().get(0);
			for (int i=0; i<right.getChildren().size(); ++i) {
				final String name = left.getChildren().get(i).getNode().toString();
				final String val = parseRight(right.getChildren().get(i), scope);
				if (null != val) assigns.put(name, val); // scope.vars.put(name, val);
			}
		} else {
			final String name = assign.getInternalTargets().get(0).getNode().toString();
			final String val = parseRight(right, scope);
			assigns.put(name, val);
		}
 
		return assigns;
	}
	
	/**
	 * Adds a child Scope to the given parent Scope, and also a variable to the parent scope
	 * with no class (just for the name). Then populates the child scope.
	 */
	static public String parseFunctionDef(final FunctionDef fn, final Scope parent) {
		final String name = fn.getInternalName();
		print("function name: " + name);
		parseNode(fn.getChildren(), parent);
		return name;
	}
	
	static public String parseClassDef(final ClassDef c, final Scope parent) {
		final String name = c.getInternalName();
		parseNode(c.getChildren(), parent);
		return name;
	}
	
	/** Discover the class of the right statement in an assignment.
	 * 
	 * @param right
	 */
	static public String parseRight(final Object right, final Scope scope) {
		if (right instanceof Name) {
			return scope.vars.getOrDefault( ((Name)right).toString(), null);
		}
		if (right instanceof Call) {
			// TODO recursive, to parse e.g. imp.getProcessor().getPixels()
			// to figure out what class is imp (if known), what class getProcessor returns,
			// and then what class getPixels returns.
			// And to handle also IJ.getImage()
		}
		return null;
	}

	static public final void print(Object s) {
		System.out.println(s);
	}
	
	static public final void main(String[] args) {
		try {
			parseAST(testCode).print("");
		} catch (Exception e) {
			e.printStackTrace();
			if (null != e.getCause())
				e.getCause().printStackTrace();
		}
	}
}