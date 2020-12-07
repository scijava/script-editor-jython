package sc.fiji.jython.autocompletion;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.python.antlr.PythonTree;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Tuple;
import org.python.antlr.base.mod;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.ParserFacade;
import org.python.core.PyObject;

public class JythonScriptParser {
	
	static public String testCode = String.join("\n",
			"from ij import IJ, ImageJ as IJA, VirtualStack, ImagePlus",
			"from ij.process import ByteProcessor",
			"grey8 = IJ.getImage().GREY8", // static field but should work
			"pixels = IJ.getImage().getProcessor().getPixels()",
			"imp = IJ.getImage()",
			"ip = imp.getProcessor()",
			"width, height = imp.getWidth(), imp.getHeight()",
			"imp2 = imp",
			"class Volume(VirtualStack):",
			"  def getProcessor(self, index):",
			"    return ByteProcessor(512, 512)",
			"  def getSize(self):",
			"    return 10",
			"def createImage(w, h):",
			"  imp = ImagePlus('new', ByteProcessor(w, h))",
			"  return imp",
			"def setRoi(an_imp):",
			"  ip = an_imp.getStack().getProcessor(3)",
			"  pixels = ip.");
	
	/**
	 * Returns the top-level Scope. 
	 */
	static public Scope parseAST(final String code) {
		// The code includes from beginning of the file until the point at which an autocompletion is requested.
		// Therefore, remove the last line, which would fail to parse because it is incomplete
		final int lastLineBreak = code.lastIndexOf("\n");
		final String codeToParse = code.substring(0, lastLineBreak);
		final mod m = ParserFacade.parse(codeToParse, CompileMode.exec, "<none>", new CompilerFlags());

		return parseNode(m.getChildren(), null, false);
	}
	
	static public Scope parseNode(final List<PythonTree> children, final Scope parent, final boolean is_class) {
		
		final Scope scope = new Scope(parent, is_class);
		
		for (final PythonTree child : children) {
			print(child.getClass());
			
			if (child instanceof ImportFrom)
				scope.imports.putAll(parseImportStatement( (ImportFrom)child ));
			else if (child instanceof Assign)
				scope.vars.putAll(parseAssignStatement( (Assign)child, scope ));
			else if (child instanceof FunctionDef)
				parseFunctionDef( (FunctionDef)child, scope);
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
	static public Map<String, DotAutocompletions> parseImportStatement(final ImportFrom im) {
		final Map<String, DotAutocompletions> classes = new HashMap<>();
		final String module = im.getModule().toString();
		for (int i=0; i<im.getNames().__len__(); ++i) {
			final String alias = im.getInternalNames().get(i).getAsname().toString(); // alias: as name
			final String simpleClassName = im.getInternalNames().get(i).getInternalName(); // class name
			classes.put("None" == alias ? simpleClassName : alias, new StaticDotAutocompletions(module + "." + simpleClassName));
		}
		return classes;
	}
	
	static public Map<String, DotAutocompletions> parseAssignStatement(final Assign assign, final Scope scope) {
		final Map<String, DotAutocompletions> assigns = new HashMap<>();
		//final expr right = assign.getInternalValue(); // strangely this works
		final PythonTree right = assign.getChildren().get(1);
		if (right instanceof Tuple || right instanceof org.python.antlr.ast.List) { // TODO are there any other possible?
			final PythonTree left = assign.getChildren().get(0);
			for (int i=0; i<right.getChildren().size(); ++i) {
				final String name = left.getChildren().get(i).getNode().toString();
				final DotAutocompletions val = parseRight(right.getChildren().get(i), scope);
				if (null != val) assigns.put(name, val); // scope.vars.put(name, val);
			}
		} else {
			final String name = assign.getInternalTargets().get(0).getNode().toString();
			final DotAutocompletions val = parseRight(right, scope);
			assigns.put(name, val);
		}
 
		return assigns;
	}
	
	/**
	 * Adds a child Scope to the given parent Scope, and also a variable to the parent scope
	 * with no class (just for the name). Then populates the child scope.
	 * 
	 * @fn
	 * @parent
	 * 
	 * return
	 */
	static public void parseFunctionDef(final FunctionDef fn, final Scope parent) {
		// Get the function name
		final String name = fn.getInternalName();
		// Get the list of argument names
		final List<String> argumentNames = fn.getInternalArgs().getChildren().stream()
				.map(arg -> arg.getNode().toString()).collect(Collectors.toList());
		// Parse the function body
		final Scope fn_scope = parseNode(fn.getChildren(), parent, false);
		// Get the return type, if any
		final PythonTree last = fn.getChildren().get(fn.getChildCount() -1);
		final String returnClassName = last instanceof Return ? parseRight(last.getChildren().get(0), fn_scope).toString() : null;
		parent.vars.put(name, new DefVarAutocompletions(returnClassName, argumentNames));
	}
	
	static public String parseClassDef(final ClassDef c, final Scope parent) {
		final String name = c.getInternalName();
		final Scope class_scope = parseNode(c.getChildren(), parent, true);
		// Search for the constructor __init__ if any
		for (final PythonTree child: c.getChildren()) {
			if (child instanceof FunctionDef && "__init__" == child.getNode().toString()) {
				final List<String> argumentNames = ((FunctionDef)child).getInternalArgs().getChildren().stream()
						.map(arg -> arg.getNode().toString()).collect(Collectors.toList());
				if (argumentNames.size() > 0) {
					final String first = argumentNames.get(0);
					if ("self" == first || "this" == first) {
						final List<String> dotAutocompletions = new ArrayList<>();
						// Get all methods and fields of the class
						// TODO
						// TODO the e.g. self.width = 10 within an __init__ will need to be captured in a Call parsing
						class_scope.vars.put(first, new ClassDotAutocompletions(dotAutocompletions));
					}
				}
				break;
			}
		}
		return name;
	}
	
	/** Discover the class returned by the right statement in an assignment.
	 * 
	 * @param right
	 */
	static public DotAutocompletions parseRight(final PyObject right, final Scope scope) {
		if (right instanceof Name) {
			// e.g. the name of another variable:
			// imp2 = imp
			// e.g. the name of a constructor or a function
			// ip = ByteProcessor(512, 512)
			return scope.find( ((Name)right).getInternalId(), DotAutocompletions.EMPTY);
		}
		if (right instanceof Attribute) {
			// e.g. a field or a method
			// gray8 = IJ.getImage().GRAY8
			final Attribute attr = (Attribute)right;
			final DotAutocompletions da = parseRight(attr.getValue(), scope);
			if (DotAutocompletions.EMPTY == da)
				return da;
			final String name = attr.getInternalAttr();
			final String className = da.getClassname();
			try {
				final Class<?> c = Class.forName(className);
				for (final Method m : c.getMethods())
					if (m.getName().equals(name))
						return new VarDotAutocompletions(m.getReturnType().getName());
				return new VarDotAutocompletions(c.getField(name).getType().getName());
			} catch (Exception e) {
				print("Could not find method or field " + name + " in class " + className);
			}
		}
		if (right instanceof Call) {
			// e.g. a method call, in particular the last one in the chain
			// imp = IJ.getImage().getProcessor()
			final Call call = (Call)right;
			return parseRight(call.getFunc(), scope); // getFunc() returns an Attribute or a Name
		}
		
		return DotAutocompletions.EMPTY;
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