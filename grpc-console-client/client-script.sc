import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.grpc.internal._
import akka.grpc.scaladsl._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import example.myapp.helloworld.grpc._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

implicit val actorSys = ActorSystem("HelloWorldClient")
implicit val mat = ActorMaterializer()
implicit val ec = actorSys.dispatcher

def stop(implicit client: AkkaGrpcClient) = {
  client.close()
    .flatMap(_ => actorSys.terminate())
    .onComplete {
      case Success(_) => println("Stopped")
      case Failure(ex) => ex.printStackTrace()
    }
}