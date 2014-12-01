package mujava.app;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Reloader extends ClassLoader {
	protected List<String> classpath;
	protected List<Class<?>> cache;
	
	public Reloader() {
		String classpath = System.getProperty("java.class.path");
		String[] classpathEntries = classpath.split(File.pathSeparator);
		this.classpath = Arrays.asList(classpathEntries);
		this.cache = new LinkedList<Class<?>>();
	}
	
	public Reloader(ClassLoader parent) {
		super(parent);
		String classpath = System.getProperty("java.class.path");
		String[] classpathEntries = classpath.split(File.pathSeparator);
		this.classpath = Arrays.asList(classpathEntries);
		this.cache = new LinkedList<Class<?>>();
	}
	
	public Reloader(List<String> classpath, ClassLoader parent) {
		super(parent);
		this.classpath = classpath;
		this.cache = new LinkedList<Class<?>>();
	}

    @Override
    public Class<?> loadClass(String s) throws ClassNotFoundException {
    	Class<?> clazz;
        clazz = findClass(s);
        if (clazz != null) this.cache.add(clazz);
        return clazz;
    }
    
    public Class<?> rloadClass(String s, boolean reload) throws ClassNotFoundException {
    	Class<?> clazz = retrieveFromCache(s);
    	if (clazz != null) {
    		if (!reload) {
    			return clazz;
    		} else {
    			return reload(s);
    		}
    	}
        clazz = findClass(s);
        if (clazz != null) this.cache.add(clazz);
        return clazz;
    }
    
    protected Class<?> reload(String s) throws ClassNotFoundException {
		Class<?> clazz = null;
		Reloader r = new Reloader(this.classpath, this.getParent());
		clazz = r.loadClass(s);
		for (Class<?> c : this.cache) {
			if (c.getName().compareTo(s) != 0) {
				r.loadClass(c.getName());
			}
		}
		return clazz;
	}
    
    protected Class<?> retrieveFromCache(String s) {
    	Class<?> clazz = null;
    	for (Class<?> c : this.cache) {
    		if (c.getName().compareTo(s)==0) {
    			clazz = c;
    			break;
    		}
    	}
    	return clazz;
    }

    @Override
    public Class<?> findClass(String s) {
    	Class<?> clazz;
    	try {
            byte[] bytes = loadClassData(s);
            clazz = this.defineClass(s, bytes, 0, bytes.length);
            resolveClass(clazz);
            return clazz;
        } catch (IOException ioe) {
            try {
            	clazz = super.loadClass(s);
            	resolveClass(clazz);
            	return clazz;
            } catch (ClassNotFoundException ignore) { }
            	ioe.printStackTrace(System.out);
            	return null;
        }
    }

    protected byte[] loadClassData(String className) throws IOException {
    	boolean found = false;
    	File f = null;
    	for (String cp : this.classpath) {
    		f = new File(cp + className.replaceAll("\\.", Core.SEPARATOR) + ".class");
    		found = f.exists();
    		if (found) break;
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
}
