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
      Agent.class.getPackageName().replace('.', '/') + "/Probe.class";
  private static final String PROBE_CONSTANTS_RESOURCE =
      Agent.class.getPackageName().replace('.', '/') + "/Probe$Constants.class";
  private static final String PROBE_INJECTION_RESOURCE =
      Agent.class.getPackageName().replace('.', '/') + "/Probe$Injection.class";

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

  private static byte[] resourceToBytes(String resource) throws IOException {
    try (var in = Agent.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Cannot find " + resource + " inside the agent jar");
      }
      return in.readAllBytes();
    }
  }

  private static Path buildBootstrapJar() throws IOException {


    var jarPath = Files.createTempFile("allocation-agent-probe", ".jar");
    jarPath.toFile().deleteOnExit();

    var manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

    try (var output = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
      output.putNextEntry(new JarEntry(PROBE_RESOURCE));
      output.write(resourceToBytes(PROBE_RESOURCE));
      output.closeEntry();
      output.putNextEntry(new JarEntry(PROBE_CONSTANTS_RESOURCE));
      output.write(resourceToBytes(PROBE_CONSTANTS_RESOURCE));
      output.closeEntry();
      output.putNextEntry(new JarEntry(PROBE_INJECTION_RESOURCE));
      output.write(resourceToBytes(PROBE_INJECTION_RESOURCE));
      output.closeEntry();
    }

    return jarPath;
  }
}