package com.github.forax.allocagent.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

final class AllocationTransformer implements ClassFileTransformer {

  // Never instrument our own classes: Probe.allocation()
  private static final String AGENT_PACKAGE =
      Agent.class.getPackage().getName().replace('.', '/');

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) {

    //System.err.println("[allocation-agent] try to transform " + className);

    if (className == null || className.startsWith(AGENT_PACKAGE) || isPlatformClass(className)) {
      return null; // null == "leave unchanged"
    }

    //System.err.println("[allocation-agent] try to instrument " + className);

    try {
      var reader = new ClassReader(classfileBuffer);
      var writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
      var visitor = new AllocationClassVisitor(writer);
      reader.accept(visitor, 0);
      return writer.toByteArray();
    } catch (Throwable t) {
      // An instrumentation bug should not crash the target application
      System.err.println("[allocation-agent] failed to instrument " + className + ": " + t);
      return null;
    }
  }

  private boolean isPlatformClass(String className) {
    // Bootstrap/JDK classes are skipped for simplicity
    return className.startsWith("java/")
        || className.startsWith("javax/")
        || className.startsWith("jdk/")
        || className.startsWith("sun/")
        || className.startsWith("com/sun/");
  }
}
