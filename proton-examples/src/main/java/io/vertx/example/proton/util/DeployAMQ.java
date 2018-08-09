package io.vertx.example.proton.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * @author Martin Spisiak (mspisiak@redhat.com) on 08/08/18.
 */
public class DeployAMQ {
  private static final String brokerName = "amq63";

  public static void startAMQBroker() throws IOException, InterruptedException {
    System.out.println("Starting AMQ broker through docker...\n");
    ProcessBuilder processBuilder = new ProcessBuilder("docker", "run", "--name", brokerName,
      "-p", "5672:5672", "-d", "registry.access.redhat.com/jboss-amq-6/amq63-openshift");
    Process process = processBuilder.start();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      System.err.println(line);
    }
    boolean endedInTime = process.waitFor(5, TimeUnit.MINUTES);
    if (!endedInTime || process.exitValue() != 0) {
      System.err.println((char) 27 + "[31mA broker init has failed!" + (char) 27 + "[0m");
      System.exit(1);
    }

    ProcessBuilder pb = new ProcessBuilder("docker", "logs", "--follow", brokerName);
    pb.redirectErrorStream(true);
    Process p1 = pb.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p1.getInputStream()));
    String ln;
    while ((ln = reader.readLine()) != null) {
//      System.out.println(ln);
      if (ln.contains("Apache ActiveMQ 5.11.0.redhat-630347") && ln.contains("started")) {
        p1.destroy();
        System.out.println((char) 27 + "[32mAn AMQ broker has been started!" + (char) 27 + "[0m");
        return;
      }
    }
  }

  public static void stopAMQBroker() throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder("docker", "rm", brokerName, "-f");
    Process process = processBuilder.start();
    if (process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0) {
      System.out.println((char) 27 + "[32mAn AMQ broker has been stopped successfully." + (char) 27 + "[0m");
    } else {
      System.err.println((char) 27 + "[31mAn AMQ shutdown unsuccessful, the broker container probably doesn't exist." + (char) 27 + "[0m");
    }
  }
}
