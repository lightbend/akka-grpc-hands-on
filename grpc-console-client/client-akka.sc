:load client-script.sc

// to connect to Akka Http server
val clientSettings =
  GrpcClientSettings.usingServiceDiscovery("greeter-service-akka").withTls(false)  

// Create a client-side stub for the service
implicit val client = GreeterServiceClient(clientSettings)
