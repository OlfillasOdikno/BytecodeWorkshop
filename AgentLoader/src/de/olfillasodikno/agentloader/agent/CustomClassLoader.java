package de.olfillasodikno.agentloader.agent;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

public class CustomClassLoader extends URLClassLoader{

	private final HashMap<String, byte[]> classes;
	
	private final ArrayList<String> used;
	
	public CustomClassLoader(URL[] urls, ClassLoader parent,HashMap<String, byte[]> classes) {
		super(urls, parent);
		this.classes = classes;
		used = new ArrayList<>();
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		used.add(name);
		byte[] clazz = classes.get(name);
		if(clazz == null) {
			return super.findClass(name);
		}
		return defineClass(name, clazz, 0, clazz.length);
	}
	
	public ArrayList<String> getUsed() {
		return used;
	}
}
