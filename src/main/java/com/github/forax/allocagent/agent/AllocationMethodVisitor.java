package com.github.forax.allocagent.agent;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

final class AllocationMethodVisitor extends LocalVariablesSorter {
  private static final String PROBE_OWNER = Probe.class.getName().replace('.', '/');
  private static final String PROBE_ARRAY_DESC =
      "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
  private static final String PROBE_OBJECT_DESC =
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

  private final String ownerClassName;
  private final String methodName;
  private final String methodDescriptor;

  AllocationMethodVisitor(int access, String descriptor, MethodVisitor mv,
                          String ownerClassName, String methodName, String methodDescriptor) {
    super(Opcodes.ASM9, access, descriptor, mv);
    this.ownerClassName = ownerClassName;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (opcode == Opcodes.NEW) {
      mv.visitLdcInsn(type);
      pushContext();
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE_OWNER, "objectAllocation", PROBE_OBJECT_DESC, false);
    } else if (opcode == Opcodes.ANEWARRAY) {
      mv.visitInsn(Opcodes.DUP);
      pushContext();
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE_OWNER, "arrayAllocation", PROBE_ARRAY_DESC, false);
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    if (opcode == Opcodes.NEWARRAY) {
      mv.visitInsn(Opcodes.DUP);
      pushContext();
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE_OWNER, "arrayAllocation", PROBE_ARRAY_DESC, false);
    }
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    mv.visitInsn(Opcodes.DUP);
    pushContext();
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE_OWNER, "arrayAllocation", PROBE_ARRAY_DESC, false);
  }

  private void pushContext() {
    mv.visitLdcInsn(ownerClassName);
    mv.visitLdcInsn(methodName);
    mv.visitLdcInsn(methodDescriptor);
  }
}