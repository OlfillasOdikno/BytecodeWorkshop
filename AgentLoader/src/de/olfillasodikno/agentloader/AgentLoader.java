package de.olfillasodikno.agentloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import de.olfillasodikno.agentloader.agent.ARCH;
import de.olfillasodikno.agentloader.agent.Loader;
import de.olfillasodikno.agentloader.agent.OS;

public class AgentLoader {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException, IOException {
		String agent_file = null, vm_filter = "", agent_args = "", out_file = null;
		for (int i = 0; i < args.length; i++) {
			String s = args[i];
			if (s.equals("--agent")) {
				if (i < args.length - 1) {
					i++;
					agent_file = args[i];
					continue;
				}
			} else if (s.equals("--args")) {
				if (i < args.length - 1) {
					i++;
					agent_args = args[i];
					continue;
				}
			} else if (s.equals("--filter")) {
				if (i < args.length - 1) {
					i++;
					vm_filter = args[i];
					continue;
				}
			} else if (s.equals("--out")) {
				if (i < args.length - 1) {
					i++;
					out_file = args[i];
					continue;
				}
			}
		}

		if (agent_file == null) {
			System.out.println(
					"syntax: --agent <agent.jar> [--args <agent_args>] [--filter <vm_filter>] [--out <out_file>]");
			return;
		}

		File agent = new File(agent_file);
		if (!agent.exists()) {
			return;
		}

		String jar_file = AgentLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		jar_file = jar_file.substring(1);
		String file = extractLoader(jar_file);
		if (file == null) {
			return;
		}
		URL f = Loader.class.getResource("/shaded_tools.jar");

		Loader loader = new Loader();

		Class<?> provider = loader.loadVMClass(f, file, OS.getOS());

		Loader.loadAgent(agent, agent_args, vm_filter, provider);

		if (out_file != null) {
			loader.writeJarWithFiles(f, new File(out_file));
		}

	}

	public static String extractLoader(String parentFile) {
		File parent = new File(parentFile).getParentFile();
		if (parent == null) {
			System.err.println("ERROR getting Parent File");
			return null;
		}
		File folder = new File(parent, "natives");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		OS os = OS.getOS();
		ARCH arch = ARCH.getArch();
		if (arch == ARCH.NOT_FOUND || os == OS.NOT_FOUND) {
			System.err.println("No ARCH or OS for your System");
			return null;
		}
		String path = "/natives/";
		if (arch == ARCH.x64) {
			path += "64/";
		} else if (arch == ARCH.x86) {
			path += "32/";
		}
		if (os == OS.WIN) {
			path += "windows/attach.dll";
		} else if (os == OS.LIN) {
			path += "linux/libattach.so";
		} else if (os == OS.SOL) {
			path += "solaris/libattach.so";
		} else if (os == OS.MAC) {
			System.err.println("ERROR NO MAC support");
			return null;
		}

		File outFile = new File(folder, new File(path).getName());
		if (!outFile.exists()) {
			outFile.getParentFile().mkdirs();
		}
		try {
			InputStream is = AgentLoader.class.getResourceAsStream(path);
			FileOutputStream fos = new FileOutputStream(outFile);
			byte[] buf = new byte[4096];
			int r;
			while ((r = is.read(buf, 0, buf.length)) != -1) {
				fos.write(buf, 0, r);
			}
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outFile.getAbsolutePath();
	}

}
