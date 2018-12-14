
:load client-script.sc

// to connect to Lagom server
val clientSettings =
  GrpcClientSettings.usingServiceDiscovery("greeter-service-lagom")

// Create a client-side stub for the service
implicit val client = GreeterServiceClient(clientSettings)