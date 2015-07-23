package mujava.loader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
//import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class is used to reload and re-link classes.
 * <p>
 * <p>
 * Features of this class includes:
 * <li>maintaining a cache of classes to be reloaded</li>
 * <li>defining a priority path to load classes first from this one</li>
 * <li>reloading and re-linking a class and all classes marked as reloadable</li>
 * <li>instead of requiring to load a class as reloadable this version allows to just mark a class as reloadable</li>
 * <li>mark all classes in a folder and set this folder as priority path</li>
 * <p>
 * <p>
 * Main changes from previous version:
 * <li>no longer requires to load a class to make it reloadable</li>
 * <p>
 * 
 * @author Simon Emmanuel Gutierrez Brida
 * @version 2.9.1
 */
public class Reloader extends ClassLoader {
	protected Set<String> classpath;
	protected String priorityPath;
	protected Set<String> reloadableCache;
	protected List<Class<?>> reloadableClassCache;
	protected Reloader child;

	public Reloader(List<String> classpath, ClassLoader parent) {
		super(parent);
		this.classpath = new TreeSet<String>();
		this.classpath.addAll(classpath);
		this.reloadableCache = new TreeSet<String>();
		this.reloadableClassCache = new LinkedList<Class<?>>();
	}
	
	private Reloader(Set<String> classpath, ClassLoader parent, Set<String> reloadableCache) {
		this(classpath, parent);
		this.reloadableCache = reloadableCache;
	}
	
	private Reloader(Set<String> classpath, ClassLoader parent) {
		super(parent);
		this.classpath = classpath;
		this.reloadableClassCache = new LinkedList<Class<?>>();
	}
	

	@Override
	public Class<?> loadClass(String s) throws ClassNotFoundException {
		Class<?> clazz = this.reloadableCache.contains(s)?retrieveFromCache(s):null;
		if (clazz == null) {
			if (this.getParent() != null) {
				try {
					clazz = this.getParent().loadClass(s);
				} catch (ClassNotFoundException e) {}
			}
			if (clazz == null) {
				clazz = findClass(s);
			}
		}
		if (this.reloadableCache.contains(s)) addToCache(clazz);
		return clazz;
	}
	
	public void markClassAsReloadable(String s) {
		this.reloadableCache.add(s);
	}

	public Class<?> loadClassAsReloadable(String s) throws ClassNotFoundException {
		Class<?> clazz = loadClass(s);
		if (clazz != null) {
			addToCache(clazz);
			markClassAsReloadable(s);
		}
		return clazz;
	}

	public Class<?> rloadClass(String s, boolean reload) throws ClassNotFoundException {
		Class<?> clazz = null;
		if (reload) {
			clazz = reload(s);
		}
		if (clazz == null) {
			clazz = loadClass(s);
		}
		if (clazz != null && reload) {
			addToCache(clazz);
			markClassAsReloadable(s);
		}
		return clazz;
	}
	
	public void setPathAsPriority(String path) {
		this.priorityPath = path;
		//this.classpath.remove(path);
	}
	
	public void markEveryClassInFolderAsReloadable(String folder) {
		markEveryClassInFolderAsReloadable(folder, null);
	}
	
	public void markEveryClassInFolderAsReloadable(String folder, Set<String> allowedPackages) {
		File pathFile = new File(folder);
		if (pathFile.exists() && pathFile.isDirectory()) {
			setPathAsPriority(folder);
			crawlAndMark(pathFile, "", allowedPackages);
		}
	}
	
	private void crawlAndMark(File dir, String pkg, Set<String> allowedPackages) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.getName().startsWith(".")) {
				continue;
			} else if (file.isFile() && file.getName().endsWith(".class")) {
				if (allowedPackages != null && !allowedPackages.contains(pkg)) {
					continue;
				}
				this.markClassAsReloadable(this.getClassName(file, pkg));
			} else if (file.isDirectory()) {
				String newPkg;
				if (!pkg.isEmpty()) {
					newPkg = pkg + "." + file.getName();
				} else {
					newPkg = file.getName();
				}
				crawlAndMark(file, newPkg, allowedPackages);
			}
		}
	}
	
	private String getClassName(File file, String pkg) {
		String className;
		String classSimpleName = file.getName();
		int lastDotIdx = classSimpleName.lastIndexOf('.');
		className = classSimpleName.substring(0, lastDotIdx);
		if (!pkg.isEmpty()) {
			className = pkg + "." + className;
		}
		return className;
	}

	protected Class<?> loadAgain(String s) throws ClassNotFoundException {
		Class<?> clazz = null;
		if (classExist(s, this.classpath.toArray(new String[this.classpath.size()]))) {
			clazz = findClass(s);
		} else {
			clazz = loadClassAsReloadable(s);
		}
		return clazz;
	}

	protected Class<?> reload(String s) throws ClassNotFoundException {
		Class<?> clazz = null;
		Set<String> childReloadableCache = new TreeSet<String>();
		childReloadableCache.addAll(this.reloadableCache);
		Reloader r = new Reloader(this.classpath, this, childReloadableCache);
		for (String c : this.reloadableCache) {
			if (c.compareTo(s) != 0) {
				Class<?> newClass = r.loadAgain(c);
				r.addToCache(newClass);
			}
		}
		clazz = r.loadAgain(s);
		this.child = r;
		r.addToCache(clazz);
		return clazz;
	}

	protected Class<?> retrieveFromCache(String s) {
		Class<?> clazz = null;
		for (Class<?> c : this.reloadableClassCache) {
			if (c.getName().compareTo(s)==0) {
				clazz = c;
				break;
			}
		}
		return clazz;
	}

	@Override
	public Class<?> findClass(String s) throws ClassNotFoundException {
		Class<?> clazz = null;
		try {
			byte[] bytes = loadClassData(s);
			clazz = this.defineClass(s, bytes, 0, bytes.length);
			resolveClass(clazz);
			return clazz;
		} catch (IOException ioe) {
			throw new ClassNotFoundException("unable to find class " + s, ioe);
		}
	}

	protected byte[] loadClassData(String className) throws IOException {
		boolean found = false;
		File f = null;
		if (this.priorityPath != null) f = new File(this.priorityPath + className.replaceAll("\\.", File.separator) + ".class");
		if (f != null && f.exists()) {
			found = true;
		}
		if (!found) {
			for (String cp : this.classpath) {
				f = new File(cp + className.replaceAll("\\.", File.separator) + ".class");
				found = f.exists();
				if (found) break;
			}
		}
		if (!found) {
			throw new IOException("File " + className + " doesn't exist\n");
		}
		int size = (int) f.length();
		byte buff[] = new byte[size];
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		dis.readFully(buff);
		dis.close();
		return buff;
	}

	private void addToCache(Class<?> clazz) {
		boolean found = false;
		int i = 0;
		for (Class<?> c : this.reloadableClassCache) {
			found = c.getName().compareTo(clazz.getName()) == 0;
			if (found) break;
			i++;
		}
		if (found) this.reloadableClassCache.remove(i);
		this.reloadableClassCache.add(clazz);
	}

	public Reloader getChild() {
		return this.child;
	}

	public Reloader getLastChild() {
		Reloader lastChild = this;
		while (lastChild.child != null) {
			lastChild = lastChild.child;
		}
		return lastChild;
	}

	private boolean classExist(String s, String[] classpath) {
		boolean found = false;
		File f = null;
		for (String cp : classpath) {
			f = new File(cp + s.replaceAll("\\.", File.separator) + ".class");
			found = f.exists();
			if (found) break;
		}
		return found;
	}

}