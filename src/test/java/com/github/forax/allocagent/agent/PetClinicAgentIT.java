package com.github.forax.allocagent.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetClinicAgentIT {

  @Test
  public void shouldLogAllocations() throws Exception {
    var agentJar = System.getProperty("allocation.agent.jar");
    assertNotNull(agentJar, "allocation.agent.jar system property is not set");

    var isGitHubAction = "true".equals(System.getenv("GITHUB_ACTIONS"));
    var sleepTime = isGitHubAction ? 20_000 : 7_000;

    var logFile = Path.of("target", "petclinic-allocation.log");
    Files.createDirectories(logFile.getParent());

    var java = Path.of(
        System.getProperty("java.home"),
        "bin",
        "java")
        .toString();

    var petClinicJar = PetClinicAgentIT.class.getResource("/spring-petclinic-4.0.0-SNAPSHOT.jar");
    assertNotNull(petClinicJar, "petclinic-jar not found");
    var petClinicJarPath = Path.of(petClinicJar.toURI());

    var process = new ProcessBuilder(
        java,
        "-javaagent:" + agentJar + "=" + logFile,
        "-jar",
        petClinicJarPath.toString())
        .inheritIO()
        .start();

    Thread.sleep(sleepTime);

    process.destroy();

    process.onExit().get();  // wait until the process exit

    var lines = Files.readAllLines(logFile);

    System.err.println(lines.size() + " lines read");
    System.err.println(lines.stream().limit(10).collect(Collectors.toList()));

    assertTrue(lines.size() > 100_000);
  }
}