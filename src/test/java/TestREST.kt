import javax.ws.rs.client.ClientBuilder

fun main(args: Array<String>) {
    val client = ClientBuilder.newClient()
    val target = client.target("https://api.ipify.org/?format=json")
    val result = target.request().get().readEntity<Response>(Response::class.java)
    println(result.ip)
}

class Response {
    var ip: String = ""
}