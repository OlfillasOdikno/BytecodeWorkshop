package de.olfillasodikno.agentloader.agent;

import java.util.List;

public interface ImplAttachProvider {
	public abstract String name(); 
	
	@SuppressWarnings("rawtypes")
	public abstract List listVirtualMachines();
	
	public abstract ImplVM attachVirtualMachine(String s1);
}
