package io.vertx.example.health;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author Martin Spisiak (mspisiak@redhat.com) on 10/08/18.
 */
public class HealthChecksExample extends AbstractVerticle {

  @Override
  public void start() throws Exception {

    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

    healthCheckHandler.register("receiver",
      future ->
        vertx.eventBus().send("health", "ping", response -> {
          if (response.succeeded()) {
            future.complete(Status.OK(new JsonObject().put("body", response.result().body())));
          } else {
            future.complete(Status.KO());
          }
        })
    );

    Router router = Router.router(vertx);

    router.get("/health*").handler(healthCheckHandler);
    router.get().handler(StaticHandler.create());

    vertx.setTimer(10000, id -> vertx.eventBus().consumer("health", msg -> {
      System.out.println("Received a ping: " + msg.body());
      msg.reply("ok");
    }));

    vertx.createHttpServer().requestHandler(router::accept).listen(8080);
  }
}
