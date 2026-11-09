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

    @GET("api/v1/sessions/{id}")
    suspend fun getSession(
        @Path("id") sessionId: Long
    ): Response<SessionResponse>

    @GET("api/v1/sessions/health")
    suspend fun checkHealth(): Response<Map<String, String>>
}
