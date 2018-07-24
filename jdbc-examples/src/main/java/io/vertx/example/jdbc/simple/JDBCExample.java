package io.vertx.example.jdbc.simple;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

/*
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class JDBCExample extends AbstractVerticle {

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner.runExample(JDBCExample.class);
  }

  @Override
  public void start() throws Exception {

    JsonObject config = new JsonObject()
      .put("jdbcUrl", "jdbc:postgresql://localhost:5432/my_data")
      .put("driverClassName", "org.postgresql.Driver")
      .put("principal", "user")
      .put("credential", "password");

    final JDBCClient client = JDBCClient.createShared(vertx, config);

    client.getConnection(conn -> {
      if (conn.failed()) {
        System.err.println(conn.cause().getMessage());
        return;
      }

      final SQLConnection connection = conn.result();
      connection.execute("create table test(id int primary key, name varchar(255))", res -> {
        if (res.failed()) {
          throw new RuntimeException(res.cause());
        }
        // insert some test data
        connection.execute("insert into test values(1, 'Hello')", insert -> {
          // query some data
          connection.query("select * from test", rs -> {
            for (JsonArray line : rs.result().getResults()) {
              System.out.println(line.encode());
            }

            // and close the connection
            connection.close(done -> {
              if (done.failed()) {
                throw new RuntimeException(done.cause());
              }
            });
          });
        });
      });
    });
  }
}
