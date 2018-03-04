package de.olfillasodikno.agentloader.agent;

public interface ImplVM {
	
	public abstract void loadAgent(String jar_file, String options);
	public abstract void detach();
}
