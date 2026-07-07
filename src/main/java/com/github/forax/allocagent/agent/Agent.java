package com.github.forax.allocagent.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class Agent {
  private static final String PROBE_RESOURCE =
      Probe.class.getName().replace('.', '/') + ".class";

  public static void premain(String agentArgs, Instrumentation inst) throws IOException {
    if (!inst.isRetransformClassesSupported()) {
      throw new AssertionError("Retransform classes are not supported");
    }
    if (!inst.isRedefineClassesSupported()) {
      throw new AssertionError("Redefine classes are not supported");
    }

    // 1. Pull Probe.class out of this very jar and repackage it alone
    //    into a small standalone jar.
    var bootJar = buildBootstrapJar();

    // 2. Append it to the bootstrap loader's search path BEFORE Probe is
    //    ever referenced, so every classloader resolves the same class.
    try (var jarFile = new JarFile(bootJar.toFile())) {
      inst.appendToBootstrapClassLoaderSearch(jarFile);
    }

    // 3. Now safe to touch Probe -- it resolves via the bootstrap loader.
    Probe.init(agentArgs, inst);

    inst.addTransformer(new AllocationTransformer(), true);
  }

  public static void agentmain(String agentArgs, Instrumentation inst) throws IOException {
    premain(agentArgs, inst);
  }

  private static Path buildBootstrapJar() throws IOException {
    byte[] probeBytes;
    try (var in = Agent.class.getClassLoader().getResourceAsStream(PROBE_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Cannot find " + PROBE_RESOURCE + " inside the agent jar");
      }
      probeBytes = in.readAllBytes();
    }

    var jarPath = Files.createTempFile("allocation-agent-probe", ".jar");
    jarPath.toFile().deleteOnExit();

    var manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

    try (var output = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
      output.putNextEntry(new JarEntry(PROBE_RESOURCE));
      output.write(probeBytes);
      output.closeEntry();
    }

    return jarPath;
  }
}