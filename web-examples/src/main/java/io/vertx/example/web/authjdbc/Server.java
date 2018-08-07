package io.vertx.example.web.authjdbc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

import static io.vertx.example.util.DockerDatabase.*;
import static io.vertx.example.util.DockerDbConfig.POSTGRESQL;

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class Server extends AbstractVerticle {

  public static final String AUTHENTICATION_QUERY = "SELECT PASSWORD, PASSWORD_SALT FROM \"user\" WHERE USERNAME = ?";

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) { Runner.runExample(Server.class); }

  @Override
  public void start() throws Exception {

    startDocker(POSTGRESQL);
    JsonObject config = new JsonObject()
      .put("jdbcUrl", "jdbc:postgresql://localhost:5432/" + dbName)
      .put("driverClassName", "org.postgresql.Driver")
      .put("principal", dbUser)
      .put("credential", dbPassword);

    Class.forName("org.postgresql.Driver");
    final JDBCClient client = JDBCClient.createShared(vertx, config);
    // quick load of test data, this is a *sync* helper not intended for
    // real deployments...
    setUpInitialData(client);

    // Create a JDBC client with a test database
    /* JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
      .put("driver_class", "org.hsqldb.jdbcDriver")); */

//     If you are planning NOT to build a fat jar, then use the BoneCP pool since it
//     can handle loading the jdbc driver classes from outside vert.x lib directory
//    JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
//        .put("provider_class", "io.vertx.ext.jdbc.spi.impl.BoneCPDataSourceProvider")
//        .put("jdbcUrl", "jdbc:hsqldb:mem:test?shutdown=true")
//        .put("username", "sa")
//        .put("password", ""));

    Router router = Router.router(vertx);

    // We need cookies, sessions and request bodies
    router.route().handler(CookieHandler.create());
    router.route().handler(BodyHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

    // Simple auth service which uses a JDBC data source
    AuthProvider authProvider = JDBCAuth.create(vertx, client);
    ((JDBCAuth) authProvider).setAuthenticationQuery(AUTHENTICATION_QUERY);

    // We need a user session handler too to make sure the user is stored in the session between requests
    router.route().handler(UserSessionHandler.create(authProvider));

    // Any requests to URI starting '/private/' require login
    router.route("/private/*").handler(RedirectAuthHandler.create(authProvider, "/loginpage.html"));

    // Serve the static private pages from directory 'private'
    router.route("/private/*").handler(StaticHandler.create().setCachingEnabled(false).setWebRoot("authjdbc/private"));

    // Handles the actual login
    router.route("/loginhandler").handler(FormLoginHandler.create(authProvider));

    // Implement logout
    router.route("/logout").handler(context -> {
      context.clearUser();
      // Redirect back to the index page
      context.response().putHeader("location", "/").setStatusCode(302).end();
    });

    // Serve the non private static pages
    router.route().handler(StaticHandler.create("authjdbc/webroot"));

    vertx.createHttpServer().requestHandler(router::accept).listen(8080);

  }
  private void setUpInitialData(JDBCClient client) {
    client.getConnection(sqlConnectionAsyncResult -> {
      if(sqlConnectionAsyncResult.succeeded()){

        SQLConnection connection = sqlConnectionAsyncResult.result();
        executeStatement(connection,"drop table if exists user_roles");
        executeStatement(connection,"drop table if exists \"user\"");
        executeStatement(connection,"drop table if exists roles_perms");
        executeStatement(connection,"create table \"user\" (username varchar(255), password varchar(255), password_salt varchar(255) )");
        executeStatement(connection, "create table user_roles (username varchar(255), role varchar(255))");
        executeStatement(connection, "create table roles_perms (role varchar(255), perm varchar(255))");

        executeStatement(connection, "ALTER TABLE \"user\" ADD CONSTRAINT pk_username PRIMARY KEY (username)");
        executeStatement(connection, "ALTER TABLE user_roles ADD CONSTRAINT \"pk_user_roles\" PRIMARY KEY (username, role)");
        executeStatement(connection, "ALTER TABLE roles_perms ADD CONSTRAINT \"pk_roles_perms\" PRIMARY KEY (role)");
        executeStatement(connection, "ALTER TABLE user_roles ADD CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES \"user\"(username)");
        executeStatement(connection, "ALTER TABLE user_roles ADD CONSTRAINT fk_roles FOREIGN KEY (role) REFERENCES roles_perms(role)");

        executeStatement(connection,"insert into \"user\" values ('tim', 'EC0D6302E35B7E792DF9DA4A5FE0DB3B90FCAB65A6215215771BF96D498A01DA8234769E1CE8269A105E9112F374FDAB2158E7DA58CDC1348A732351C38E12A0', 'C59EB438D1E24CACA2B1A48BC129348589D49303858E493FBE906A9158B7D5DC')");
        executeStatement(connection,"insert into roles_perms values ('dev', 'commit_code')");
        executeStatement(connection,"insert into roles_perms values ('admin', 'merge_pr')");
        executeStatement(connection,"insert into user_roles values ('tim', 'dev')");
        executeStatement(connection,"insert into user_roles values ('tim', 'admin')");
      } else {
        sqlConnectionAsyncResult.cause().printStackTrace();
      }
    });

  }

  @Override
  public void stop() throws Exception {
    stopDockerDatabase();
    super.stop();
  }

  private void executeStatement(SQLConnection connection, String sql) {
    connection.query(sql,resultSetAsyncResult -> {
        if(resultSetAsyncResult.failed()){
          System.out.println("Query :" + sql + ". Failed!");
          resultSetAsyncResult.cause().printStackTrace();
        }
    });
  }

}

