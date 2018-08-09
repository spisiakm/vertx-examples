package io.vertx.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * @author Martin Spisiak (mspisiak@redhat.com) on 25/07/18.
 */
public class DockerDatabase {

  public static final String dbUser = "user";
  public static final String dbPassword = "password";
  public static final String dbName = "my_data";
  public static DockerDbConfig dockerAppName;

  public static void stopDockerDatabase() throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder("docker", "rm", dockerAppName.getDbName(), "-f");
    Process process = processBuilder.start();
    if (process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0) {
      System.out.println((char) 27 + "[32mA " + dockerAppName.getDbName() + " database has been stopped successfully." + (char) 27 + "[0m");
    } else {
      System.err.println((char) 27 + "[31mA " + dockerAppName.getDbName() + " database shutdown has failed!" + (char) 27 + "[0m");
    }
  }

  public static void startDocker(DockerDbConfig dockerDbConfigConf) throws IOException, InterruptedException {
    DockerDatabase.dockerAppName= dockerDbConfigConf;
    System.out.println("Starting " + dockerDbConfigConf.getDbName() + " database through docker...\n");
    System.out.println("docker run --name " + dockerDbConfigConf.getDbName() + " -e " + dockerDbConfigConf.getUserEnv() + "=" + dbUser + " -e "
        + dockerDbConfigConf.getPasswdEnv() + "=" + dbPassword + " -e " + dockerDbConfigConf.getDbNameEnv() + "=" + dbName +
      " -p " + dockerDbConfigConf.getPort() + ":" + dockerDbConfigConf.getPort() + " -d " + dockerDbConfigConf.getDbName());
    ProcessBuilder processBuilder = new ProcessBuilder("docker", "run", "--name", dockerDbConfigConf.getDbName(),
      "-e", dockerDbConfigConf.getUserEnv() + "=" + dbUser, "-e", dockerDbConfigConf.getPasswdEnv() + "=" + dbPassword, "-e", dockerDbConfigConf.getDbNameEnv() + "=" + dbName,
      "-p", dockerDbConfigConf.getPort() + ":" + dockerDbConfigConf.getPort(), "-d", dockerDbConfigConf.getDbName() + ":" + dockerDbConfigConf.getContainerVersion());

    Process process = processBuilder.start();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      System.err.println(line);
    }
    boolean endedInTime = process.waitFor(5, TimeUnit.MINUTES);
    if (!endedInTime || process.exitValue() != 0) {
      System.err.println((char) 27 + "[31mA " + dockerDbConfigConf.getDbName() + " database init has failed!" + (char) 27 + "[0m");
      System.exit(1);
    }

    ProcessBuilder pb = new ProcessBuilder("docker", "logs", "--follow", dockerDbConfigConf.getDbName());
    pb.redirectErrorStream(true);
    Process p1 = pb.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p1.getInputStream()));
    String ln;
    int cnt = 0;
    while ((ln = reader.readLine()) != null) {
      //System.out.println(ln);
      if (ln.contains(dockerDbConfigConf.getExceptDeployingResult())) {
        cnt++;
        if (dockerDbConfigConf == DockerDbConfig.MONGODB || cnt == 2) {
          p1.destroy();
          System.out.println((char) 27 + "[32mA " + dockerDbConfigConf.getDbName() + " database through docker has been started!" + (char) 27 + "[0m");
          return;
        }
      }
    }
  }

}
