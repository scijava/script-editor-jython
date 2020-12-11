package sc.fiji.jython.autocompletion;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.python.antlr.PythonTree;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.Yield;
import org.python.antlr.ast.alias;
import org.python.antlr.ast.arguments;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.ParserFacade;
import org.python.core.PyObject;
import org.python.indexer.types.NModuleType;

public class JythonScriptParser {
	
	/** Controls whether debugging statements print to stdout. */
	static public boolean DEBUG = false;
	
	/**
	 * Parse valid jython code.
	 * 
	 * @return The top-level {@code Scope}, which is empty (see {@code {@link Scope#isEmpty()}) when the code has errors and can't be parsed by {@code ParserFacade#parse(String, CompileMode, String, CompilerFlags)}.
	 */
	static public Scope parseAST(final String code) {
		// The code includes from beginning of the file until the point at which an autocompletion is requested.
		// Therefore, remove the last line, which would fail to parse because it is incomplete
		try {
			final mod m = ParserFacade.parse(code, CompileMode.exec, "<none>", new CompilerFlags());
			return parseNode(m.getChildren(), null, null);
		} catch (Throwable t) {
			t.printStackTrace();
			return new Scope(null);
		}
	}

	/**
	 * Parse a {@code List} of {@code PythonTree} instances, each representing a python statement
	 * including {@code ImportFrom, Assign, FunctionDef, ClassDef}.
	 * 
	 * @param children The list of statements.
	 * @param parent The {@code Scope} that contains these statements.
	 * @param className Indicates whether the containing {@code Scope} is a python class definition, otherwise null.
	 * @return A new {@code Scope} containing {@code DotAutocompletions} to represent each statement.
	 */
	static public Scope parseNode(final List<PythonTree> children, final Scope parent, final String className) {
		if (null == children) return parent;
		final Scope scope = new Scope(parent, className);
		parseNode(scope, children, className);
		return scope;
	}

	static public void parseNode(final Scope scope, final List<PythonTree> children, final String className) {
		
		for (final PythonTree child : children) {
			print(child.getClass());
			
			if (child instanceof ImportFrom)
				scope.imports.putAll(parseImportFromStatement( (ImportFrom)child ));
			else if (child instanceof Import)
				scope.imports.putAll(parseImportStatement((Import)child));
			else if (child instanceof Assign)
				scope.vars.putAll(parseAssignStatement( (Assign)child, scope ));
			else if (child instanceof FunctionDef)
				parseFunctionDef((FunctionDef)child, scope);
			else if (child instanceof ClassDef)
				parseClassDef((ClassDef)child, scope);
			else if (child instanceof Expr)
				parseExpr((Expr)child, scope);
			else
				print("UNKNOWN child: " + child + " -- " + child.getText());
		}
	}
	
	static public void parseExpr(final Expr child, final Scope scope) {
		// child.getText() shows child is the base
		print("Expr: " + child.getText() + ", " + child.getInternalValue() + ", " + child.getValue() + ", children: " + String.join(", ", child.getChildren().stream().map(PythonTree::toString).collect(Collectors.toList())));
	}

	/**
	 * Parse import statements, considering aliases.
	 * There can be more than one if e.g. commas were used, as in "from ij import IJ, ImageJ".
	 * @param im
	 * @return A map of simple class names or their aliases as keys versus a {@code DotAutocompletions} as value. 
	 */
	static public Map<String, DotAutocompletions> parseImportFromStatement(final ImportFrom im) {
		final Map<String, DotAutocompletions> classes = new HashMap<>();
		final String module = im.getModule().toString();
		for (int i=0; i<im.getNames().__len__(); ++i) {
			final String alias = im.getInternalNames().get(i).getAsname().toString(); // alias: as name
			final String simpleClassName = im.getInternalNames().get(i).getInternalName(); // class name
			classes.put("None" == alias ? simpleClassName : alias, new StaticDotAutocompletions(module + "." + simpleClassName));
		}
		return classes;
	}
	
	static public Map<String, DotAutocompletions> parseImportStatement(final Import im) {
		final Map<String, DotAutocompletions> classes = new HashMap<>();
		final List<alias> aliases = im.getInternalNames();
		for (final alias a: aliases) {
			final String as = a.getInternalAsname();
			final String name = a.getInternalName();
			classes.put(null == as || "None" == as ? name : as, new StaticDotAutocompletions(name));
		}
		return classes;
	}
	
	/**
	 * Parse an assignment (an equal sign) to find out the class of the left side (the variable)
	 * by asking the right side about what it is or returns.
	 * There can be more than one variable when using deconstruction statements like e.g. "width, height = imp.getWidth(), imp.getHeight()".
	 * 
	 * @param assign
	 * @param scope
	 * @return A map of variable names as keys versus {@code DotAutocompletions} as values.
	 */
	static public Map<String, DotAutocompletions> parseAssignStatement(final Assign assign, final Scope scope) {
		final Map<String, DotAutocompletions> assigns = new HashMap<>();
		//final expr right = assign.getInternalValue(); // strangely this works
		final PythonTree right = assign.getChildren().get(1);
		if (right instanceof Tuple || right instanceof org.python.antlr.ast.List) { // TODO are there any other possible?
			final PythonTree left = assign.getChildren().get(0);
			for (int i=0; i<right.getChildren().size(); ++i) {
				final String name = left.getChildren().get(i).getNode().toString();
				final DotAutocompletions val = parseRight(right.getChildren().get(i), scope);
				if (null != val) assigns.put(name, val);
			}
		} else {
			// Left is a Name: simple assignment e.g. "one = 1"
			PyObject left = assign.getChildren().get(0);
			if ( left instanceof Name ) {
				assigns.put(((Name)left).getInternalId(), parseRight(right, scope));
				return assigns;
			}
			// Assignment to an attribute
			// Handle left when it's e.g. self.width = 10 which creates a new member in "self".
			// Will have to be recursive, as it could be multiple dereferences, e.g. self.volume.name = "that"
			// Has to: find out what the base is (e.g. 'self') and add, as an expansion of it, the attribute (e.g. "width")
			// with the assigned class (e.g. "PyInteger" for "10"), and add names, if not there yet, to the appropriate lists for autocompletion.
			System.out.println("left 0: " + left);
			int i = 0;
			final ArrayList<Attribute> attrs = new ArrayList<>();
			while (left instanceof Attribute) {
				final Attribute attr = (Attribute)left;
				attrs.add(attr);
				left = attr.getValue();
			}
			if (left instanceof Name) {
				String varName = ((Name)left).getInternalId();
				Collections.reverse(attrs);
				Scope scopeC = scope;
				for (final Attribute attr: attrs) {
					final DotAutocompletions ac = scopeC.find(varName, DotAutocompletions.EMPTY); // in the first iteration it finds the completions for the base Name
					if (ac instanceof ClassDotAutocompletions) {
						final ClassDotAutocompletions cda = (ClassDotAutocompletions)ac;
						varName = attr.getInternalAttrName().getInternalId(); // in the first iteration becomes the name of the first Attribute
						// Add the name of the Attribute to the list of expansions for the prior varName
						scopeC = cda.scope; // prepare scope for next iteration
						scopeC.vars.put(varName, cda); // Is this needed? I think it isn't
						cda.put(varName); // add varName (e.g. "width") as a possible expansion for the prior varName (e.g. "self").
					} else {
						// Don't know how to handle e.g. self.doThis().that = 10 because for "doThis()" there would be a class return type stored 
						break;
					}
				}
				return assigns;
			}
		}
 
		return assigns;
	}
	
	static public DotAutocompletions parseLeft(final PyObject left, final DotAutocompletions value, final Scope scope) {
		if (left instanceof Name) {
			final DotAutocompletions ac = scope.find(((Name)left).getInternalId(), DotAutocompletions.EMPTY);
			if (ac instanceof ClassDotAutocompletions) return ac;
		}
		
		if (left instanceof Attribute) {
			final Attribute attr = (Attribute)left;
			final DotAutocompletions ac = parseLeft(attr.getValue(), value, scope);
			if (ac instanceof ClassDotAutocompletions) {
				((ClassDotAutocompletions)ac).put(attr.getInternalAttrName().toString());
			}
		}
		
		return DotAutocompletions.EMPTY;
	}
	
	/**
	 * Adds a child Scope to the given parent Scope, and also a variable to the parent scope
	 * with no class, just for the function name. Then populates the child scope.
	 * 
	 * @fn
	 * @parent
	 */
	static public void parseFunctionDef(final FunctionDef fn, final Scope parent) {
		// Get the function name
		final String name = fn.getInternalName();
		// Get the list of argument names, if any
		arguments args = fn.getInternalArgs();
		final List<String> argumentNames = args != null && args.getChildren() != null ? // why oh why not return an empty List<PythonTree>
				args.getChildren().stream().map(arg -> arg.getNode().toString()).collect(Collectors.toList())
				: Collections.emptyList();
		// Parse the function body
		final List<PythonTree> children = fn.getChildren();
		if (null == children) return;
		final Scope fn_scope = new Scope(parent, null);
		// Add arguments to the scope -- must be done BEFORE parseNode
		for (final String arg: argumentNames) {
			// Empty, for the first argument ("self" or similar) will be replaced later if it's part of a class definition.
			fn_scope.vars.put(arg, new ClassDotAutocompletions("<unknown>", Collections.emptyList(), Collections.emptyList(), new ArrayList<String>(), parent));
		}
		parseNode(fn_scope, fn.getChildren(), null);
		// Get the return type, if any
		final PythonTree last = fn.getChildren().get(fn.getChildCount() -1);
		final String returnClassName = last instanceof Return ? parseRight(last.getChildren().get(0), fn_scope).toString() : null;
		parent.vars.put(name, new DefVarDotAutocompletions(name, returnClassName, argumentNames, fn_scope));
	}
	
	/**
	 * Adds an entry to the parent scope with the python classname, e.g. "Volume", with its parameters (from its __init__ method if any),
	 * including as well the methods and fields from any superclass,
	 * and adds another entry in the class scope for "self" with all the class method names.
	 * 
	 * @param c
	 * @param parent
	 */
	static public void parseClassDef(final ClassDef c, final Scope parent) {
		final String pyClassname = c.getInternalName();
		final Scope class_scope = parseNode(c.getChildren(), parent, pyClassname);
		// Methods of the class
		final List<String> classDotAutocompletions = new ArrayList<>();
		// Iterate vars of the scope, which are those of the class only
		for (final DotAutocompletions da: class_scope.vars.values()) {
			if (da instanceof DefVarDotAutocompletions) {
				final DefVarDotAutocompletions dda = (DefVarDotAutocompletions)da;
				classDotAutocompletions.add(dda.fnName);
			}
		}
		// Superclasses
		final List<String> superclassNames = new ArrayList<>();
		for (final expr e: c.getInternalBases()) {
			final DotAutocompletions da = parent.find(e.getText(), null);
			if (null == da || null == da.getClassname())
				print("Could not find completions and className for " + e.getText());
			else
				superclassNames.add(da.getClassname());
		}
		// Search for the constructor __init__ if any to get the constructor parameters
		final List<String> argumentNames = new ArrayList<>();
		final ClassDotAutocompletions cda = new ClassDotAutocompletions(pyClassname, superclassNames, argumentNames, classDotAutocompletions, class_scope);
		for (final PythonTree child: c.getChildren()) {
			if (!(child instanceof FunctionDef)) continue;
			final FunctionDef fn = (FunctionDef)child;
			final List<PythonTree> args = fn.getInternalArgs().getChildren();
			if (args.size() > 0) {
				// Populate argument list by reading them from the __init__ method
				if ("__init__".equals(fn.getInternalName())) {
					// Add all arguments except the first one, which is the internal reference conventionally named "self"
					argumentNames.addAll(args.subList(1, args.size()).stream()
						.map(arg -> arg.getNode().toString()).collect(Collectors.toList()));
				}
				// Add completions to the first argument (generally "self")
				// TODO check annotations, shouldn't add them if the function is static
				System.out.println(args.get(0).getNode().toString());
				final DefVarDotAutocompletions fnda = (DefVarDotAutocompletions)class_scope.vars.get(fn.getInternalName());
				final DotAutocompletions argda = fnda.scope.find(fnda.argumentNames.get(0), DotAutocompletions.EMPTY);
				if (argda instanceof ClassDotAutocompletions) {
					// Replace the autocompletions for the first argument with cda plus whichever autocompletions it accumulated within the function definition.
					fnda.scope.vars.put(fnda.argumentNames.get(0), cda.plus(argda.get()));
				}
				//.scope.vars.put(args.get(0).getNode().toString(), cda);
			}
		}
		
		// Add to the parent scope for expansion of the constructor name plus parameters 
		parent.vars.put(pyClassname, cda);
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
		if (right instanceof Num) {
			// e.g. return 10
			// e.g. n = 42
			return new VarDotAutocompletions(((Num)right).getInternalN().getClass().getName());
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
			// Could also be a python module, e.g. attempting to autocomplete "os.path."
			try {
				final NModuleType module = Scope.indexer.loadModule(className + "." + name); // overly expensive: any way to check it exists without loading it?
				if (null != module)
					return new StaticDotAutocompletions(className  + "." + name);
			} catch (Exception e) {
				print("Not a python module: " + className + "." + name);
			}
		}
		if (right instanceof Call) {
			// e.g. a method call, in particular the last one in the chain
			// imp = IJ.getImage().getProcessor()
			final Call call = (Call)right;
			return parseRight(call.getFunc(), scope); // getFunc() returns an Attribute or a Name
		}
		if (right instanceof Yield) {
			final Yield yield = (Yield)right;
			return parseRight(yield.getValue(), scope);
		}
		
		return DotAutocompletions.EMPTY;
	}

	static public final void print(Object s) {
		if (DEBUG) System.out.println(s);
	}
}