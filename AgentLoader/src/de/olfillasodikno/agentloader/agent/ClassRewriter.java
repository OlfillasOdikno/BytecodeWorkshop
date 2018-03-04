package de.olfillasodikno.agentloader.agent;

import java.util.HashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassRewriter extends ClassVisitor {

	private final String[] appendInterfaces;
	private final HashMap<String, String> replaceMap;

	public ClassRewriter(ClassVisitor cv, String[] appendInterfaces, HashMap<String, String> replaceMap) {
		super(Opcodes.ASM6, cv);
		this.appendInterfaces = appendInterfaces;
		this.replaceMap = replaceMap;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (appendInterfaces != null) {
			String[] ifaces = new String[interfaces.length + appendInterfaces.length];
			System.arraycopy(interfaces, 0, ifaces, 0, interfaces.length);
			System.arraycopy(appendInterfaces, 0, ifaces, interfaces.length, appendInterfaces.length);
			interfaces = ifaces;
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (replaceMap == null) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		String newDesc = replaceMap.get(desc);
		if (newDesc != null) {
			desc = newDesc;
		}

		return new MethodRewriter(super.visitMethod(access, name, desc, signature, exceptions), replaceMap);
	}

	public static class MethodRewriter extends MethodVisitor {

		private final HashMap<String, String> replaceMap;

		public MethodRewriter(MethodVisitor mv, HashMap<String, String> replaceMap) {
			super(Opcodes.ASM6, mv);
			this.replaceMap = replaceMap;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if (replaceMap != null && name.equals("loadLibrary")) {
				name = "load";
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			if (replaceMap != null && cst instanceof String) {
				String replace = replaceMap.get(cst);
				if (replace != null) {
					cst = replace;
				}
			}
			super.visitLdcInsn(cst);
		}

	}
}
