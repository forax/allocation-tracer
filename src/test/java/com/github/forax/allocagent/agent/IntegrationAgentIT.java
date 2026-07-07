package com.github.forax.allocagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.forax.allocagent.notagent.testdata.TestProgram;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class IntegrationAgentIT {

  @Test
  public void shouldLogAllocations(@TempDir Path tempDir) throws Exception {
    var agentJar = System.getProperty("allocation.agent.jar");
    assertNotNull(agentJar, "allocation.agent.jar system property is not set");

    var logFile = tempDir.resolve("allocations.log");

    var java = Path.of(
        System.getProperty("java.home"),
        "bin",
        "java")
        .toString();

    var classpath = System.getProperty("java.class.path");

    var process = new ProcessBuilder(
        java,
        "-javaagent:" + agentJar + "=" + logFile,
        "-cp",
        classpath,
        TestProgram.class.getName())
        .inheritIO()
        .start();

    assertEquals(0, process.waitFor());

    assertTrue(Files.exists(logFile));

    var lines = Files.readAllLines(logFile);

    // DEBUG
    lines.forEach(System.err::println);

    assertFalse(lines.isEmpty());

    assertTrue(
        lines.stream().anyMatch(line -> line.contains(TestProgram.class.getSimpleName())),
        () -> "Expected an allocation from " + TestProgram.class.getName());
  }
}