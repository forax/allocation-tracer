package com.github.forax.allocagent.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class AllocationTransformerTest {

  private final AllocationTransformer transformer = new AllocationTransformer();

  @Test
  public void  ignoresNullClassName() {
    assertNull(transformer.transform(
        null,
        null,
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  ignoresJavaClasses() {
    assertNull(transformer.transform(
        null,
        "java/lang/String",
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  ignoresJavaxClasses() {
    assertNull(transformer.transform(
        null,
        "javax/crypto/Cipher",
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  ignoresJdkClasses() {
    assertNull(transformer.transform(
        null,
        "jdk/internal/misc/Unsafe",
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  ignoresSunClasses() {
    assertNull(transformer.transform(
        null,
        "sun/misc/Unsafe",
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  ignoresComSunClasses() {
    assertNull(transformer.transform(
        null,
        "com/sun/tools/javac/Main",
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  ignoresAgentClasses() {
    var packageName =
        Agent.class.getPackage().getName().replace('.', '/');

    assertNull(transformer.transform(
        null,
        packageName + "/SomeClass",
        null,
        null,
        new byte[0]));
  }

  @Test
  public void  invalidClassDoesNotThrow() {
    var invalid = "this is not a class"
        .getBytes(StandardCharsets.UTF_8);

    assertDoesNotThrow(() -> {
      var result = transformer.transform(
          null,
          "foo/Bar",
          null,
          null,
          invalid);

      assertNull(result);
    });
  }
}