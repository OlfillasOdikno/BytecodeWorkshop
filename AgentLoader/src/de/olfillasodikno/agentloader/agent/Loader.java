package de.olfillasodikno.agentloader.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class Loader {

	private CustomClassLoader lastCl;

	public Class<?> loadVMClass(URL jar_file, String libPath, OS os) {
		try {

			HashMap<String, byte[]> classes = new HashMap<>();
			JarInputStream jis = new JarInputStream(jar_file.openStream());
			JarEntry je;
			byte[] buffer = new byte[4096];
			while ((je = jis.getNextJarEntry()) != null) {
				if (!je.getName().endsWith(".class")) {
					continue;
				}

				int size;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				while ((size = jis.read(buffer)) != -1) {
					bos.write(buffer, 0, size);
				}
				bos.close();

				String name = je.getName().split(".class")[0].replaceAll("/", ".");

				HashMap<String, String> replaceMap = null;

				ClassReader cr = new ClassReader(bos.toByteArray());
				ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
				String[] interfaces = null;
				if (name.equals("com.sun.tools.attach.VirtualMachineDescriptor")) {
					interfaces = new String[] { ImplDescriptor.class.getName().replace(".", "/") };
				}
				if (name.equals("com.sun.tools.attach.VirtualMachine")) {
					interfaces = new String[] { ImplVM.class.getName().replace(".", "/") };
				}
				if (name.equals("com.sun.tools.attach.spi.AttachProvider")) {
					interfaces = new String[] { ImplAttachProvider.class.getName().replace(".", "/") };
				}
				if (name.startsWith("sun.tools.attach")) {
					replaceMap = new HashMap<>();
					replaceMap.put("attach", libPath);
					replaceMap.put("(Ljava/lang/String;)Lcom/sun/tools/attach/VirtualMachine;",
							"(Ljava/lang/String;)L" + ImplVM.class.getName().replace(".", "/") + ";");
				}
				ClassRewriter visitor = new ClassRewriter(cw, interfaces, replaceMap);
				cr.accept(visitor, ClassReader.EXPAND_FRAMES);
				classes.put(name, cw.toByteArray());
			}
			jis.close();

			lastCl = new CustomClassLoader(new URL[] { jar_file }, ClassLoader.getSystemClassLoader(), classes);
			Class<?> vmC = null;
			if (os == OS.WIN) {
				vmC = lastCl.findClass("sun.tools.attach.WindowsAttachProvider");
			} else if (os == OS.LIN) {
				vmC = lastCl.findClass("sun.tools.attach.LinuxAttachProvider");
			} else if (os == OS.SOL) {
				vmC = lastCl.findClass("sun.tools.attach.SolarisAttachProvider");
			}
			lastCl.close();
			return vmC;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void writeJarWithFiles(URL in, File out) throws FileNotFoundException, IOException {
		if (lastCl == null) {
			System.err.println("loadVMClass first!");
			return;
		}
		ArrayList<String> filter = lastCl.getUsed();

		JarInputStream jis = new JarInputStream(in.openStream());
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(out));
		JarEntry je;
		byte[] buffer = new byte[4096];
		while ((je = jis.getNextJarEntry()) != null) {
			if (!je.getName().endsWith(".class")) {
				continue;
			}
			String name = je.getName().split(".class")[0].replaceAll("/", ".");
			if (!filter.contains(name)) {
				continue;
			}
			int size;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			while ((size = jis.read(buffer)) != -1) {
				bos.write(buffer, 0, size);
			}
			bos.close();

			jos.putNextEntry(new JarEntry(je.getName()));
			jos.write(bos.toByteArray());
		}
		jis.close();
		jos.close();
	}

	public static void loadAgent(File jar_file, String options, String filter, Class<?> vmClass)
			throws InstantiationException, IllegalAccessException {
		ImplAttachProvider impl = (ImplAttachProvider) vmClass.newInstance();

		List<?> vmList = impl.listVirtualMachines();
		Iterator<?> it = vmList.iterator();
		String name = ManagementFactory.getRuntimeMXBean().getName();
		while (it.hasNext()) {
			ImplDescriptor desc = (ImplDescriptor) it.next();
			if (filter != null && !filter(desc.displayName(), filter) || name.contains(desc.id())) {
				continue;
			}
			try {
				ImplVM vm = impl.attachVirtualMachine(desc.id());
				System.out.println("[" + desc.id() + "] Loading Agent in: " + desc.displayName());
				vm.loadAgent(jar_file.getAbsolutePath(), options);
				vm.detach();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static boolean filter(String input, String filter) {
		if (filter.startsWith("!") && filter.length() > 1) {
			filter = filter.substring(1);
			return !input.contains(filter);
		}
		return input.contains(filter);
	}
}
