package de.olfillasodikno.agentloader.agent;

public enum OS {
	WIN, LIN, MAC, SOL, NOT_FOUND;

	public static OS getOS() {
		
		String os = System.getProperty("os.name").toLowerCase();

		if (os.contains("win")) {
			return WIN;
		}
		if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			return LIN;
		}
		if (os.contains("mac")) {
			return MAC;
		}
		if (os.contains("sunos") || os.contains("solaris")) {
			return SOL;
		}
		return NOT_FOUND;
	}

}
