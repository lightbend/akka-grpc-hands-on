:load client-script.sc

// to connect to Play server
val clientSettings =
  GrpcClientSettings.usingServiceDiscovery("greeter-service-play")

// Create a client-side stub for the service
implicit val client = GreeterServiceClient(clientSettings)
