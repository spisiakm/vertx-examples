package io.vertx.example.util;

public enum DockerDbConfig {
  POSTGRESQL("postgres", "POSTGRES_USER" , "POSTGRES_PASSWORD" ,"POSTGRES_DB", 5432),
  MONGODB("mongo","MONGO_INITDB_ROOT_USERNAME","MONGO_INITDB_ROOT_PASSWORD","MONGO_INITDB_DATABASE",27017);

  private final String dbName;
  private final String userEnv;
  private final String passwdEnv;
  private final String dbNameEnv;
  private final int port;

  DockerDbConfig(String dbName, String userEnv, String passwdEnv, String dbNameEnv, int port) {
    this.dbName = dbName;
    this.userEnv = userEnv;
    this.passwdEnv = passwdEnv;
    this.dbNameEnv = dbNameEnv;
    this.port = port;
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
}
