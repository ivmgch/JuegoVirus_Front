package com.example.virus.data.api

import com.example.virus.model.dto.CreateSessionRequest
import com.example.virus.model.dto.JoinResponse
import com.example.virus.model.dto.JoinSessionRequest
import com.example.virus.model.dto.SessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ViralApi {

    @POST("api/v1/sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): Response<SessionResponse>

    @POST("api/v1/sessions/{id}/join")
    suspend fun joinSession(
        @Path("id") sessionId: Long,
        @Body request: JoinSessionRequest
    ): Response<JoinResponse>

    @POST("api/v1/sessions/{id}/start")
    suspend fun startSession(
        @Path("id") sessionId: Long
    ): Response<SessionResponse>

    @POST("api/v1/sessions/{id}/finish")
    suspend fun finishSession(
        @Path("id") sessionId: Long
    ): Response<SessionResponse>

    @POST("api/v1/sessions/{id}/leave")
    suspend fun leaveSession(
        @Path("id") sessionId: Long,
        @retrofit2.http.Query("playerUuid") playerUuid: String
    ): Response<Map<String, Any>>

    @GET("api/v1/sessions/{id}")
    suspend fun getSession(
        @Path("id") sessionId: Long
    ): Response<SessionResponse>

    @GET("api/v1/sessions/{id}/players/{playerUuid}")
    suspend fun getPlayerStatus(
        @Path("id") sessionId: Long,
        @Path("playerUuid") playerUuid: String
    ): Response<com.example.virus.model.dto.PlayerStatusDto>

    @GET("api/v1/sessions/health")
    suspend fun checkHealth(): Response<Map<String, String>>
}
