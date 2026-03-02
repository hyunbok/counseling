package com.counseling.api.adapter.outbound.external

import com.counseling.api.config.LiveKitProperties
import com.counseling.api.domain.exception.LiveKitException
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.livekit.server.RoomServiceClient
import io.mockk.every
import io.mockk.mockk
import livekit.LivekitModels
import reactor.test.StepVerifier
import retrofit2.Call
import retrofit2.Response

class LiveKitAdapterTest :
    StringSpec({
        val roomServiceClient = mockk<RoomServiceClient>()
        val liveKitProperties =
            LiveKitProperties(
                url = "wss://livekit.example.com",
                apiKey = "test-api-key",
                apiSecret = "test-api-secret-must-be-long-enough-for-hmac",
                tokenTtlSeconds = 3600L,
            )
        val adapter = LiveKitAdapter(roomServiceClient, liveKitProperties)

        val roomName = "tenant1-channel-test-uuid"

        fun buildRoom(name: String): LivekitModels.Room =
            LivekitModels.Room
                .newBuilder()
                .setName(name)
                .build()

        fun <T> successCall(body: T): Call<T> {
            val call = mockk<Call<T>>()
            every { call.execute() } returns Response.success(body)
            return call
        }

        fun <T> errorCall(
            code: Int,
            message: String,
        ): Call<T> {
            val call = mockk<Call<T>>()
            every { call.execute() } returns
                Response.error(
                    code,
                    okhttp3.ResponseBody.create(null, message),
                )
            return call
        }

        "createRoom returns room name on success" {
            val room = buildRoom(roomName)
            every { roomServiceClient.createRoom(roomName, 300, 2) } returns successCall(room)

            StepVerifier
                .create(adapter.createRoom(roomName))
                .assertNext { it shouldBe roomName }
                .verifyComplete()
        }

        "createRoom with custom timeout and maxParticipants returns room name" {
            val room = buildRoom(roomName)
            every { roomServiceClient.createRoom(roomName, 600, 5) } returns successCall(room)

            StepVerifier
                .create(adapter.createRoom(roomName, emptyTimeoutSec = 600, maxParticipants = 5))
                .assertNext { it shouldBe roomName }
                .verifyComplete()
        }

        "createRoom emits LiveKitException on API error" {
            every { roomServiceClient.createRoom(roomName, 300, 2) } returns
                errorCall<LivekitModels.Room>(500, "Internal Server Error")

            StepVerifier
                .create(adapter.createRoom(roomName))
                .expectErrorMatches { it is LiveKitException && it.message?.contains("500") == true }
                .verify()
        }

        "deleteRoom completes successfully when room exists" {
            val call = mockk<Call<Void?>>()
            every { call.execute() } returns Response.success(null)
            every { roomServiceClient.deleteRoom(roomName) } returns call

            StepVerifier
                .create(adapter.deleteRoom(roomName))
                .verifyComplete()
        }

        "deleteRoom completes silently when room not found (404)" {
            val call = mockk<Call<Void?>>()
            every { call.execute() } returns
                Response.error(404, okhttp3.ResponseBody.create(null, "Not Found"))
            every { roomServiceClient.deleteRoom(roomName) } returns call

            StepVerifier
                .create(adapter.deleteRoom(roomName))
                .verifyComplete()
        }

        "deleteRoom emits LiveKitException on non-404 API error" {
            val call = mockk<Call<Void?>>()
            every { call.execute() } returns
                Response.error(500, okhttp3.ResponseBody.create(null, "Internal Server Error"))
            every { roomServiceClient.deleteRoom(roomName) } returns call

            StepVerifier
                .create(adapter.deleteRoom(roomName))
                .expectErrorMatches { it is LiveKitException && it.message?.contains("500") == true }
                .verify()
        }

        "generateToken returns a non-blank JWT string" {
            val token =
                adapter.generateToken(
                    roomName = roomName,
                    identity = "agent:test-uuid",
                    name = "Test Agent",
                )
            token.shouldNotBeBlank()
        }

        "generateToken with canPublish=false returns a non-blank JWT string" {
            val token =
                adapter.generateToken(
                    roomName = roomName,
                    identity = "customer:test-uuid",
                    name = "Test Customer",
                    canPublish = false,
                    canSubscribe = true,
                )
            token.shouldNotBeBlank()
        }

        "generateToken produces distinct tokens for different identities" {
            val token1 =
                adapter.generateToken(
                    roomName = roomName,
                    identity = "agent:uuid-1",
                    name = "Agent One",
                )
            val token2 =
                adapter.generateToken(
                    roomName = roomName,
                    identity = "customer:uuid-2",
                    name = "Customer Two",
                )
            (token1 == token2) shouldBe false
        }

        "generateToken uses configured TTL in seconds" {
            val token =
                adapter.generateToken(
                    roomName = roomName,
                    identity = "agent:ttl-test",
                    name = "TTL Agent",
                )
            token.shouldNotBeBlank()
            token.split(".").size shouldBe 3
        }
    })
