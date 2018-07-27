package io.vertx.example.util;

public enum DockerDbConfig {
  POSTGRESQL("postgres", "POSTGRES_USER" , "POSTGRES_PASSWORD" ,"POSTGRES_DB", "latest",  5432 , "database system is ready to accept connections"),
  MONGODB("mongo","MONGO_INITDB_ROOT_USERNAME","MONGO_INITDB_ROOT_PASSWORD","MONGO_INITDB_DATABASE","3.4.0",27017, "waiting for connections on port");

  private final String dbName;
  private final String userEnv;
  private final String passwdEnv;
  private final String dbNameEnv;
  private final int port;
  private final String containerVersion;
  private final String exceptDeployingResult;

  DockerDbConfig(String dbName, String userEnv, String passwdEnv, String dbNameEnv, String conatinerVersion, int port, String exceptDeployingResult) {
    this.dbName = dbName;
    this.userEnv = userEnv;
    this.passwdEnv = passwdEnv;
    this.dbNameEnv = dbNameEnv;
    this.containerVersion = conatinerVersion;
    this.port = port;
    this.exceptDeployingResult = exceptDeployingResult;
  }

  public  String getDbName(){
    return this.dbName;
  }

  public String getUserEnv() {
    return userEnv;
  }

  public String getPasswdEnv() {
    return passwdEnv;
  }

  public String getDbNameEnv() {
    return dbNameEnv;
  }

  public int getPort() {
    return port;
  }

  public String getContainerVersion() {
    return containerVersion;
  }

  public String getExceptDeployingResult() {
    return exceptDeployingResult;
  }
}
