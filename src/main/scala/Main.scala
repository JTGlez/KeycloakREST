import cats.effect._
import cats.implicits._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.circe.syntax._
import org.typelevel.ci.CIString
object Main extends IOApp {
  private case class ClientRepresentation(
     clientId: String,
     enabled: Boolean,
     clientAuthenticatorType: String,
     secret: String,
     redirectUris: List[String],
     protocol: String,
     publicClient: Boolean,
     directAccessGrantsEnabled: Boolean,
     serviceAccountsEnabled: Boolean
   )

  private case class CreateClientRequest(
    clientId: String,
    clientSecret: String,
    redirectUris: List[String],
    publicClient: Boolean,
    directAccessGrantsEnabled: Boolean,
    serviceAccountsEnabled: Boolean
  )

  private case class ClientCredentialsRequest(
     clientId: String,
     clientSecret: String
   )

  private case class TokenResponse(access_token: String)
  private case class AdminTokenRequest(username: String, password: String)

  private val keycloakUrl = "http://localhost:8080"
  private val realm = "Secrets-Realm"

  private val client = BlazeClientBuilder[IO].resource
  private def getAdminToken(username: String, password: String): IO[String] = {
    val data = UrlForm(
      "grant_type" -> "password",
      "client_id" -> "admin-cli",
      "username" -> username,
      "password" -> password
    )

    client.use { httpClient =>
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"$keycloakUrl/realms/master/protocol/openid-connect/token")
      ).withEntity(data)

      httpClient.expect[TokenResponse](request).map(_.access_token)
    }
  }

  private def createClient(clientId: String, clientSecret: String, redirectUris: List[String], publicClient: Boolean, directAccessGrantsEnabled: Boolean, serviceAccountsEnabled: Boolean, adminToken: String): IO[Response[IO]] = {
    val clientRepresentation = ClientRepresentation(
      clientId = clientId,
      enabled = true,
      clientAuthenticatorType = "client-secret",
      secret = clientSecret,
      redirectUris = redirectUris,
      protocol = "openid-connect",
      publicClient = publicClient,
      directAccessGrantsEnabled = directAccessGrantsEnabled,
      serviceAccountsEnabled = serviceAccountsEnabled
    )

    client.use { httpClient =>
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"$keycloakUrl/admin/realms/$realm/clients")
      ).withHeaders(
        Header.Raw(CIString("Authorization"), s"Bearer $adminToken"),
        Header.Raw(CIString("Content-Type"), "application/json")
      ).withEntity(clientRepresentation.asJson)

      // Print the request body for debugging
      IO(println(s"Request body: ${clientRepresentation.asJson.noSpaces}")) *>
        httpClient.run(request).use { response =>
          if (response.status.isSuccess) {
            IO.pure(response)
          } else {
            response.as[String].flatMap { body =>
              IO.raiseError(new Exception(s"Failed to create client: ${response.status} - $body"))
            }
          }
        }
    }
  }

  private def getClientToken(clientId: String, clientSecret: String): IO[TokenResponse] = {
    val data = UrlForm(
      "grant_type" -> "client_credentials",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    )

    client.use { httpClient =>
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"$keycloakUrl/realms/$realm/protocol/openid-connect/token")
      ).withEntity(data)

      httpClient.expect[TokenResponse](request)
    }
  }

  private val createClientService = HttpRoutes.of[IO] {
    case req @ POST -> Root / "create-client" =>
      for {
        // Commented out: Extract the Bearer token from the Authorization header
        // authHeader <- IO.fromOption(req.headers.get[Authorization].map(_.credentials))(new Exception("Authorization header missing"))
        // adminToken <- IO.fromOption(authHeader match {
        //   case Credentials.Token(AuthScheme.Bearer, token) => Some(token)
        //   case _ => None
        // })(new Exception("Invalid Authorization header format"))

        // Automatically fetch a new admin token
        adminToken <- getAdminToken("admin", "admin")

        // Parse the request body
        createClientRequest <- req.as[CreateClientRequest]

        // Create the client in Keycloak
        createResponse <- createClient(
          createClientRequest.clientId,
          createClientRequest.clientSecret,
          createClientRequest.redirectUris,
          createClientRequest.publicClient,
          createClientRequest.directAccessGrantsEnabled,
          createClientRequest.serviceAccountsEnabled,
          adminToken
        )

        // Print the createResponse for debugging
        _ <- IO(println(s"createResponse: ${createResponse.status}"))

        // Respond with a success or error message
        response <- if (createResponse.status.isSuccess) {
          Ok(s"Client ${createClientRequest.clientId} created successfully.")
        } else {
          InternalServerError(s"Failed to create client: ${createResponse.status}")
        }
      } yield response
  }

  private val getClientTokenService = HttpRoutes.of[IO] {
    case req @ POST -> Root / "get-client-token" =>
      for {
        // Parse the request body to get clientId and clientSecret
        clientCredentialsRequest <- req.as[ClientCredentialsRequest]

        // Get the client token
        tokenResponse <- getClientToken(clientCredentialsRequest.clientId, clientCredentialsRequest.clientSecret)

        // Respond with the token
        response <- Ok(tokenResponse.access_token)
      } yield response
  }


  private val adminTokenService = HttpRoutes.of[IO] {
    case req @ POST -> Root / "admin-token" =>
      for {
        adminTokenRequest <- req.as[AdminTokenRequest]
        token <- getAdminToken(adminTokenRequest.username, adminTokenRequest.password)
        response <- Ok(token)
      } yield response
  }

  private val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" => Ok("Hello, Cats!")
  }

  private val httpApp = Router(
    "/" -> (helloWorldService <+> createClientService <+> getClientTokenService <+> adminTokenService)
  ).orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(8081, "0.0.0.0")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}