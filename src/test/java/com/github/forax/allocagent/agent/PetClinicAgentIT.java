package com.github.forax.allocagent.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetClinicAgentIT {

  @Test
  public void shouldLogAllocations() throws Exception {
    var agentJar = System.getProperty("allocation.agent.jar");
    assertNotNull(agentJar, "allocation.agent.jar system property is not set");

    var logFile = Path.of("target", "petclinic-allocation.log");
    Files.createDirectories(logFile.getParent());

    var java = Path.of(
        System.getProperty("java.home"),
        "bin",
        "java")
        .toString();

    var process = new ProcessBuilder(
        java,
        "-javaagent:" + agentJar + "=" + logFile,
        "-jar",
        "src/test/resources/spring-petclinic-4.0.0-SNAPSHOT.jar")
        .inheritIO()
        .start();

    Thread.sleep(5_000);

    process.destroy();

    process.onExit().get();  // wait until the process exit

    var lines = Files.readAllLines(logFile);

    assertTrue(lines.size() > 1_000_000);
  }
}