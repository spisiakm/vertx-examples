package io.vertx.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * @author Martin Spisiak (mspisiak@redhat.com) on 25/07/18.
 */
public class DockerDatabase {

  private static final String dockerAppName = "postgresql";
  public static final String dbUser = "user";
  public static final String dbPassword = "password";
  public static final String dbName = "my_data";

  public static void stopDockerDatabase() throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder("docker", "rm", dockerAppName, "-f");
    Process process = processBuilder.start();
    if (process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0) {
      System.out.println((char) 27 + "[32mA postgres database has been stopped successfully." + (char) 27 + "[0m");
    } else {
      System.err.println((char) 27 + "[31mPostgres database shutdown has failed!" + (char) 27 + "[0m");
    }
  }

  public static void startDockerPostgres() throws IOException, InterruptedException {
    System.out.println("Starting postgres database through docker...\n");
    ProcessBuilder processBuilder = new ProcessBuilder("docker", "run", "--name", dockerAppName,
      "-e", "POSTGRES_USER=" + dbUser, "-e", "POSTGRES_PASSWORD=" + dbPassword, "-e", "POSTGRES_DB=" + dbName,
      "-p", "5432:5432", "-d", "postgres");
    Process process = processBuilder.start();
    boolean endedInTime = process.waitFor(5, TimeUnit.MINUTES);
    if (!endedInTime || process.exitValue() != 0) {
      System.err.println((char) 27 + "[31mA posgres database init has failed!" + (char) 27 + "[0m");
      System.exit(1);
    }

    ProcessBuilder pb = new ProcessBuilder("docker", "logs", "--follow", dockerAppName);
    pb.redirectErrorStream(true);
    Process p1 = pb.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p1.getInputStream()));
    String ln;
    int cnt = 0;
    while ((ln = reader.readLine()) != null) {
//      System.out.println(ln);
      if (ln.contains("database system is ready to accept connections")) {
        cnt++;
        if (cnt == 2) {
          p1.destroy();
          System.out.println((char) 27 + "[32mA postgres database through docker has been started!" + (char) 27 + "[0m");
          return;
        }
      }
    }
  }

}
