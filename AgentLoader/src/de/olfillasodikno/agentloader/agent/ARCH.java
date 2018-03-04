package de.olfillasodikno.agentloader.agent;

public enum ARCH {
	x86, x64, NOT_FOUND;

	public static ARCH getArch() {
		String arch = System.getProperty("os.arch").toLowerCase();
		if (arch.contains("86")) {
			return x86;
		}
		if (arch.contains("64")) {
			return x64;
		}
		return NOT_FOUND;
	}
}
