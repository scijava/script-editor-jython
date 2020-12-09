package sc.fiji.jython.autocompletion;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.python.indexer.Indexer;
import org.python.indexer.types.NModuleType;


public class Scope {
	final Scope parent;
	final boolean is_class;
	final private List<Scope> children = new ArrayList<>();
	final HashMap<String, DotAutocompletions> imports = new HashMap<>();
	final HashMap<String, DotAutocompletions> vars = new HashMap<>();
	
	/** Access to jython's builtins (functions in the global scope) and default modules (array, itertools, csv, etc.)
	 *  as well as to an other user-defined modules.
	 */
	static final Indexer indexer = new Indexer();
	
	static private Thread module_watcher;
	static private WatchService watcher;
	static private Hashtable<WatchKey, Path> keys = new Hashtable<>();
	
	static {
		try {
			watcher = FileSystems.getDefault().newWatchService();
			module_watcher = new Thread() {
				@Override
				public void run() {
					while (true) {
						WatchKey key = null;
						try {
							key = watcher.take(); // waits until there is an event
						} catch (InterruptedException x) {
							return;
						}
						if (keys.containsKey(key)) {
							System.out.println("Python module at:\n" + keys.get(key) + "\n ... was updated. Clearing indexer cache.");
							// One of the files changed: unload all, given that parent modules would have been loaded as well
							// and it gets complicated quickly to find out which need to be reloaded and which don't.
							keys.clear();
							synchronized (indexer) {
								indexer.clearModuleTable();
							}
						}
					}
				}
			};
			module_watcher.setPriority(Thread.NORM_PRIORITY);
			module_watcher.start();
		} catch (Exception e ){
			System.out.println("Failed to start filesystem watcher service for python modules");
		}
	}
	
	/**
	 * Load a python module and watch its file, if any.
	 * When the file is updated or deleted, all loaded modules will be removed from the cache,
	 * because the loading of a module may trigger the loading of a parent module
	 * or additional modules that it links to.
	 * 
	 * @param qname
	 * @return The python module.
	 */
	static NModuleType loadPythonModule(final String qname) {
		synchronized (indexer) {
			NModuleType mod = null;
			try {
				mod = indexer.loadModule(qname);
			} catch (Exception e) {
				System.out.println("Could not load python module named " + qname);
				e.printStackTrace();
				return null;
			}
			try {
				// TODO: if it's an __init__.py, must watch ALL THE FILES in the containing directly
				final String filepath = indexer.getLoadedFiles().stream()
						.filter(s -> s.endsWith("/" + qname + ".py") || s.endsWith("/" + qname + "/__init__.py")).findFirst().get();
				if (null != filepath) {
					final Path path = new File(filepath).toPath();
					final WatchKey key = path.register(watcher,
							StandardWatchEventKinds.ENTRY_MODIFY,
							StandardWatchEventKinds.ENTRY_DELETE);
					keys.put(key, path);
				} else {
					System.out.println("Python module " + qname + " doesn't have an associated file path.");
				}
			} catch (Exception e) {
				System.out.println("Could not load python module named " + qname);
				System.out.println(e.getMessage());
			}
			return mod;
		}
	}
	
	public Scope(final Scope parent) {
		this(parent, false);
	}
	
	public Scope(final Scope parent, final boolean is_class) {
		this.parent = parent;
		if (null != parent) {
			parent.children.add(this);
		}
		this.is_class = is_class;
	}
	
	public boolean isEmpty() {
		return imports.isEmpty() && vars.isEmpty();
	}
	
	public DotAutocompletions find(final String name, final DotAutocompletions default_value) {
		Scope scope = this;
		while (null != scope) {
			DotAutocompletions da = scope.vars.get(name);
			if (null == da)
				da = scope.imports.get(name);
			if (null != da)
				return da;
			scope = scope.parent;
		}
		// Check python builtins
		final String builtin_className = "__builtin__." + name + "."; // e.g. __builtin__.str.join
		final List<String> dotAutocompletions = indexer.getBindings().keySet().stream()
				.filter(s -> s.startsWith(builtin_className))
				.map(s -> s.substring(builtin_className.length()))
				.collect(Collectors.toList());
		if (!dotAutocompletions.isEmpty())
			return new ClassDotAutocompletions(name, Collections.emptyList(), Collections.emptyList(), dotAutocompletions);
		
		return default_value;
	}
	
	public List<String> findStartsWith(final String name) {
		final List<String> completions = new ArrayList<>();
		Scope scope = this;
		while (null != scope) {
			for (final String varName: scope.vars.keySet()) {
				if (varName.startsWith(name)) completions.add(varName);
			}
			for (final String importName: scope.imports.keySet()) {
				if (importName.startsWith(name)) completions.add(importName);
			}
			for (String builtinName: indexer.getBindings().keySet()) {
				if (builtinName.startsWith("__builtin__."))
					builtinName = builtinName.substring(12); // without the "__builtin__." prefix
				if (builtinName.startsWith(name)) completions.add(builtinName);
			}
			scope = scope.parent;
		}
		return completions;
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
	
	public void print(final String indent) {
		if ("" == indent) {
			System.out.println("scope global:");
		}
		System.out.println(indent + "available imports: " + String.join(", ", this.getImports().keySet()));
		System.out.println(indent + "declared imports:");
		for (final Map.Entry<String, DotAutocompletions> e: imports.entrySet())
			System.out.println(indent + "  import :: " + e.getKey() + " --> " + e.getValue());
		System.out.println(indent + "available vars: " + String.join(", ", this.getVars().keySet()));
		System.out.println(indent + "declared vars:");
		for (final Map.Entry<String, DotAutocompletions> e: vars.entrySet()) {
			System.out.println(indent + "  var :: " + e.getKey() + " = " + e.getValue());
		}
		
		int i = 0;
		for (final Scope child: children) {
			System.out.println(indent + "scope[" + (i++) + "]:");
			child.print(indent + "  ");
		}
	}
}