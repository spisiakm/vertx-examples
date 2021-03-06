package io.vertx.examples.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.examples.service.impl.ProcessorServiceImpl;
import io.vertx.examples.service.utils.Runner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * The verticle publishing the service.
 */
public class ProcessorServiceVerticle extends AbstractVerticle {

  ProcessorService service;

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) throws ClassNotFoundException {
    Runner.runExample(ProcessorServiceVerticle.class);
  }

  @Override
  public void start() throws Exception {
    // Create the client object
    service = new ProcessorServiceImpl();
    // Register the handler
    ServiceBinder binder = new ServiceBinder(vertx);
    binder.setAddress("vertx.processor").register(ProcessorService.class, new ProcessorServiceImpl());

    //
    Router router = Router.router(vertx);

    // Allow events for the designated addresses in/out of the event bus bridge
    BridgeOptions opts = new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("vertx.processor"))
        .addOutboundPermitted(new PermittedOptions().setAddress("vertx.processor"));

    // Create the event bus bridge and add it to the router.
    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
    router.route("/eventbus/*").handler(ebHandler);

    //
    router.route().handler(StaticHandler.create());

    //
    vertx.createHttpServer().requestHandler(router::accept).listen(8080);

  }

}
