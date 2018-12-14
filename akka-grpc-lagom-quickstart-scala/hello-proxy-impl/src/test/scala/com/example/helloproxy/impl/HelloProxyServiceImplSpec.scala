package com.example.helloproxy.impl

import akka.actor.ActorSystem
import akka.discovery.SimpleServiceDiscovery.{ Resolved, ResolvedTarget }
import akka.discovery.{ Lookup, SimpleServiceDiscovery }
import akka.grpc.scaladsl.AkkaGrpcClient
import akka.stream.Materializer
import akka.{ Done, NotUsed }
import com.example.hello.api.HelloService
import com.example.helloproxy.api.HelloProxyService
import com.lightbend.lagom.scaladsl.api.{ ProvidesAdditionalConfiguration, ServiceCall }
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.typesafe.config.ConfigFactory
import example.myapp.helloworld.grpc.{ GreeterService, GreeterServiceClient, HelloReply, HelloRequest }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import scala.concurrent.Future
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future, Promise }

class HelloProxyServiceImplSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  "HelloProxy service" should {

    "roundtrip over HTTP" in {
      client.proxyViaHttp("Alice").invoke().map { answer =>
        answer should ===("Stubbed - Hi Alice!")
      }
    }

    "roundtrip over gRPC" in {
      client.proxyViaGrpc("Alice").invoke().map { answer =>
        answer should ===("Stubbed - Hi Alice! (gRPC)")
      }
    }

  }

  // ------------------------------------------------------------------------

  private val server: ServiceTest.TestServer[HelloProxyApplication] =
    ServiceTest.startServer(ServiceTest.defaultSetup) { ctx =>
      new HelloProxyApplication(ctx)
        with LocalServiceLocator
        with ProvidesAdditionalConfiguration {

        // uses Lagom's `ProvidesAdditionalConfiguration` cake layer to setup
        // a test-friendly "akka.discovery.method" (see below)
        override def additionalConfiguration =
          super.additionalConfiguration ++ ConfigFactory.parseString(
            s"akka.discovery.method = ${classOf[PlaceholderServiceDiscovery].getName}"
          )

        // overrides the HTTP client to inject a stub
        override lazy val helloService = new HelloServiceClientStub
        override lazy val greeterServiceClient: GreeterServiceClient = new GreeterServiceClientStub
      }
    }

  val client: HelloProxyService = server.serviceClient.implement[HelloProxyService]

  override protected def afterAll(): Unit = {
    server.stop()
  }


}

class HelloServiceClientStub extends HelloService {
  override def hello(id: String): ServiceCall[NotUsed, String] = ServiceCall {
    _ => Future.successful(s"Stubbed - Hi $id!")
  }
}

class GreeterServiceClientStub(implicit val mat: Materializer) extends GreeterServiceClient with StubbedAkkaGrpcClient {

  private implicit val ec = mat.executionContext

  def sayHello(in: HelloRequest): Future[HelloReply] =
    Future.successful(HelloReply(s"Stubbed - Hi ${in.name}! (gRPC)"))

  
  override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = {
    println(s"sayHello to in stream...")
    in.runWith(Sink.seq)
      .map(elements => HelloReply(s"Hello, ${elements.map(_.name).mkString(", ")}"))
  }

  override def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = {
    println(s"sayHello to ${in.name} with stream of chars...")
    Source(s"Hello, ${in.name}".toList)
      .map(character => HelloReply(character.toString))
  }

  override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = {
    println(s"sayHello to stream...")
    in.map(request => HelloReply(s"Hello, ${request.name}"))
  }  
}


// ------------------------------------------------------------------------
// ------------------------------------------------------------------------

// At the moment, gRPC client obtains a `SimpleServiceDiscovery` from the ActorSystem default settings
// but this test doesn't exercise that `SimpleServiceDiscovery` instance so we use a noop placeholder.
class PlaceholderServiceDiscovery(system: ActorSystem) extends SimpleServiceDiscovery {
  implicit val exCtx: ExecutionContext = system.dispatcher

  override def lookup(lookup: Lookup, resolveTimeout: FiniteDuration): Future[Resolved] = Future {
    Resolved(lookup.serviceName, immutable.Seq(ResolvedTarget("localhost", None, None)))
  }
}

protected trait StubbedAkkaGrpcClient extends AkkaGrpcClient {
  private val _closed = Promise[Done]()

  override def close(): Future[Done] = {
    _closed.trySuccess(Done)
    _closed.future
  }

  override def closed(): Future[Done] = _closed.future
}