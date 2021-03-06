package io.vertx.example.jdbc.query_params;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.io.IOException;

import static io.vertx.example.util.DockerDbConfig.*;
import static io.vertx.example.util.DockerDatabase.*;

/*
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class JDBCExample extends AbstractVerticle {

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner.runExample(JDBCExample.class);
  }

  @Override
  public void start() {

    vertx.executeBlocking(future -> {
      try {
        startDocker(POSTGRESQL);
        future.complete();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        future.fail(e);
      }
    }, result -> {
      if (result.succeeded()) {
        JsonObject config = new JsonObject()
          .put("jdbcUrl", "jdbc:postgresql://localhost:5432/" + dbName)
          .put("driverClassName", "org.postgresql.Driver")
          .put("principal", dbUser)
          .put("credential", dbPassword);

        final JDBCClient client = JDBCClient.createShared(vertx, config);

        client.getConnection(conn -> {
          if (conn.failed()) {
            System.err.println(conn.cause().getMessage());
            return;
          }
          final SQLConnection connection = conn.result();

          // create a test table
          connection.execute("create table test(id int primary key, name varchar(255))", create -> {
            if (create.failed()) {
              System.err.println("Cannot create the table");
              create.cause().printStackTrace();
              return;
            }

            // insert some test data
            connection.execute("insert into test values (1, 'Hello'), (2, 'World')", insert -> {

              // query some data with arguments
              connection.queryWithParams("select * from test where id = ?", new JsonArray().add(2), rs -> {
                if (rs.failed()) {
                  System.err.println("Cannot retrieve the data from the database");
                  rs.cause().printStackTrace();
                  return;
                }

                for (JsonArray line : rs.result().getResults()) {
                  System.out.println(line.encode());
                }

                // and close the connection
                connection.close(done -> {
                  if (done.failed()) {
                    throw new RuntimeException(done.cause());
                  }

                  try {
                    stopDockerDatabase();
                  } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                  }
                });
              });
            });
          });
        });
      }
    });
  }
}
