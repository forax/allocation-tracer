package com.github.forax.allocagent.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class AllocationClassVisitor extends ClassVisitor {
  private String ownerClassName;

  AllocationClassVisitor(ClassVisitor cv) {
    super(Opcodes.ASM9, cv);
  }

  @Override
  public void visit(int version, int access, String name, String signature,
                    String superName, String[] interfaces) {
    this.ownerClassName = name.replace('/', '.');
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor,
                                   String signature, String[] exceptions) {
    var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (mv == null) {
      return null;
    }
    return new AllocationMethodVisitor(access, descriptor, mv, ownerClassName, name, descriptor);
  }
}