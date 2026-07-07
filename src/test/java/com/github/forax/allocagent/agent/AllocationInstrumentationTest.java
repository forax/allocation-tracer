package com.github.forax.allocagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AllocationInstrumentationTest {

  @Test
  public void  instrumentsObjectAllocation() {
    byte[] original = createClassWithObjectAllocation();

    var transformer = new AllocationTransformer();
    byte[] transformed = transformer.transform(
        null,
        "example/Test",
        null,
        null,
        original);

    assertNotNull(transformed);

    assertEquals(1, countProbeCalls(transformed));
  }

  @Test
  public void  instrumentsPrimitiveArrayAllocation() {
    var original = createClassWithPrimitiveArrayAllocation();

    var transformer = new AllocationTransformer();
    var transformed = transformer.transform(
        null,
        "example/Test",
        null,
        null,
        original);

    assertNotNull(transformed);

    assertEquals(1, countProbeCalls(transformed));
  }

  @Test
  public void  instrumentsReferenceArrayAllocation() {
    var original = createClassWithReferenceArrayAllocation();

    var transformer = new AllocationTransformer();
    var transformed = transformer.transform(
        null,
        "example/Test",
        null,
        null,
        original);

    assertNotNull(transformed);

    assertEquals(1, countProbeCalls(transformed));
  }

  @Test
  public void  instrumentsMultiArrayAllocation() {
    var original = createClassWithMultiArrayAllocation();

    var transformer = new AllocationTransformer();
    var transformed = transformer.transform(
        null,
        "example/Test",
        null,
        null,
        original);

    assertNotNull(transformed);

    assertEquals(1, countProbeCalls(transformed));
  }

  // ------------------------------------------------------------------------

  private static int countProbeCalls(byte[] bytecode) {
    AtomicInteger counter = new AtomicInteger();

    new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM9) {
      @Override
      public MethodVisitor visitMethod(
          int access,
          String name,
          String descriptor,
          String signature,
          String[] exceptions) {

        return new MethodVisitor(Opcodes.ASM9) {
          @Override
          public void  visitMethodInsn(
              int opcode,
              String owner,
              String name,
              String descriptor,
              boolean isInterface) {

            if (owner.equals(Probe.class.getName().replace('.', '/'))
                && (name.equals("objectAllocation") || name.equals("arrayAllocation"))) {
              counter.incrementAndGet();
            }

            super.visitMethodInsn(
                opcode,
                owner,
                name,
                descriptor,
                isInterface);
          }
        };
      }
    }, 0);

    return counter.get();
  }

  // ------------------------------------------------------------------------

  private static byte[] createClassWithObjectAllocation() {
    var cw = new ClassWriter(0);

    cw.visit(Opcodes.V11,
        Opcodes.ACC_PUBLIC,
        "example/Test",
        null,
        "java/lang/Object",
        null);

    var mv =
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "test",
            "()V",
            null,
            null);

    mv.visitCode();

    mv.visitTypeInsn(Opcodes.NEW, "java/lang/Object");
    mv.visitInsn(Opcodes.DUP);
    mv.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        "java/lang/Object",
        "<init>",
        "()V",
        false);
    mv.visitInsn(Opcodes.POP);

    mv.visitInsn(Opcodes.RETURN);

    mv.visitMaxs(2, 0);
    mv.visitEnd();

    cw.visitEnd();

    return cw.toByteArray();
  }

  private static byte[] createClassWithPrimitiveArrayAllocation() {
    var cw = new ClassWriter(0);

    cw.visit(Opcodes.V11,
        Opcodes.ACC_PUBLIC,
        "example/Test",
        null,
        "java/lang/Object",
        null);

    var mv =
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "test",
            "()V",
            null,
            null);

    mv.visitCode();

    mv.visitIntInsn(Opcodes.BIPUSH, 10);
    mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
    mv.visitInsn(Opcodes.POP);

    mv.visitInsn(Opcodes.RETURN);

    mv.visitMaxs(2, 0);
    mv.visitEnd();

    cw.visitEnd();

    return cw.toByteArray();
  }

  private static byte[] createClassWithReferenceArrayAllocation() {
    var cw = new ClassWriter(0);

    cw.visit(Opcodes.V11,
        Opcodes.ACC_PUBLIC,
        "example/Test",
        null,
        "java/lang/Object",
        null);

    var mv =
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "test",
            "()V",
            null,
            null);

    mv.visitCode();

    mv.visitIntInsn(Opcodes.BIPUSH, 10);
    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
    mv.visitInsn(Opcodes.POP);

    mv.visitInsn(Opcodes.RETURN);

    mv.visitMaxs(2, 0);
    mv.visitEnd();

    cw.visitEnd();

    return cw.toByteArray();
  }

  private static byte[] createClassWithMultiArrayAllocation() {
    var cw = new ClassWriter(0);

    cw.visit(Opcodes.V11,
        Opcodes.ACC_PUBLIC,
        "example/Test",
        null,
        "java/lang/Object",
        null);

    var mv =
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "test",
            "()V",
            null,
            null);

    mv.visitCode();

    mv.visitInsn(Opcodes.ICONST_2);
    mv.visitInsn(Opcodes.ICONST_3);
    mv.visitMultiANewArrayInsn("[[I", 2);
    mv.visitInsn(Opcodes.POP);

    mv.visitInsn(Opcodes.RETURN);

    mv.visitMaxs(3, 0);
    mv.visitEnd();

    cw.visitEnd();

    return cw.toByteArray();
  }
}