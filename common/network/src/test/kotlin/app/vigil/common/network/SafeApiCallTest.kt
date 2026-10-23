package app.vigil.common.network

import app.vigil.common.core.ApiErrorBody
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.serialization.Serializable

class SafeApiCallTest : FunSpec({

    val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    suspend fun call(handler: MockRequestHandler): AppResult<ItemDto> {
        val client = createHttpClient(
            baseUrl = "https://api.test/",
            isDebugBuild = false,
            engine = MockEngine(handler),
        )
        return safeApiCall { client.get("items/1").body<ItemDto>() }
    }

    test("a 2xx response is Success") {
        call { respond("""{"id":"1"}""", HttpStatusCode.OK, jsonHeaders) } shouldBe AppResult.Success(ItemDto("1"))
    }

    test("a non-2xx response is Http with the parsed error body") {
        val error = call {
            respond("""{"code":"not_found","message":"No item"}""", HttpStatusCode.NotFound, jsonHeaders)
        }.errorOrNull()
        error.shouldBeInstanceOf<AppError.Network.Http>()
        error.code shouldBe 404
        error.body shouldBe ApiErrorBody("not_found", "No item")
    }

    test("a non-2xx response without a JSON body is Http without a body") {
        val error = call { respond("oops", HttpStatusCode.InternalServerError) }.errorOrNull()
        error.shouldBeInstanceOf<AppError.Network.Http>()
        error.code shouldBe 500
        error.body shouldBe null
    }

    test("a request timeout is Timeout") {
        call { throw HttpRequestTimeoutException("items/1", 1_000) }
            .errorOrNull().shouldBeInstanceOf<AppError.Network.Timeout>()
    }

    test("an IOException is NoConnection") {
        call { throw IOException("offline") }.errorOrNull().shouldBeInstanceOf<AppError.Network.NoConnection>()
    }

    test("a body of the wrong shape is Serialization") {
        call { respond("""{"unexpected":true}""", HttpStatusCode.OK, jsonHeaders) }
            .errorOrNull().shouldBeInstanceOf<AppError.Network.Serialization>()
    }
})

@Serializable
private data class ItemDto(val id: String)

private fun AppResult<*>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error
