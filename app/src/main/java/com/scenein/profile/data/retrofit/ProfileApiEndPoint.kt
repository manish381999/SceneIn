package com.scenein.profile.data.retrofit



import com.scenein.profile.data.models.ConnectionActionResponse
import com.scenein.profile.data.models.ConnectionStatusResponse
import com.scenein.profile.data.models.GetEventsByUserResponse
import com.scenein.profile.data.models.GetUserResponse
import com.scenein.profile.data.models.MyConnectionsResponse
import com.scenein.profile.data.models.MyProfileResponse
import com.scenein.profile.data.models.MyTicketsActivityResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ProfileApiEndPoint {

    @GET("users/get_my_profile")
    suspend fun getMyProfile(): Response<MyProfileResponse>


    @FormUrlEncoded
    @POST("users/get_public_profile")
    suspend fun getPublicUserProfile(@Field("profile_id") profileId: String): Response<GetUserResponse>


    @FormUrlEncoded
    @POST("events/by_user")
    suspend fun getEventsByUser(
        // The user_id is now nullable. If you pass null, the backend will fetch
        // events for the currently authenticated user. If you provide an ID,
        // it will fetch events for that specific user (for public profiles).
        @Field("user_id") userId: String?
    ): Response<GetEventsByUserResponse>

    @FormUrlEncoded
    @POST("connections/check_status")
    suspend fun checkConnectionStatus(
        @Field("profile_id") profileId: String   // The profile being viewed
    ): Response<ConnectionStatusResponse>

    @FormUrlEncoded
    @POST("connections/send_request")
    suspend fun sendConnectionRequest(
        @Field("receiver_id") receiverId: String
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("connections/remove")
    suspend fun removeConnection(
        @Field("other_user_id") otherUserId: String
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("connections/respond")
    suspend fun respondToConnectionRequest(
        @Field("connection_id") connectionId: String,
        @Field("response") response: String // "accepted" or "declined"
    ): Response<ConnectionActionResponse>


    @GET("tickets/my_tickets")
    suspend fun getMyTicketsActivity(): Response<MyTicketsActivityResponse>


    // For the Profile "Connections" Tab
    @FormUrlEncoded
    @POST("connections/by_user")
    suspend fun getMyConnections(@Field("user_id") userId: String?
    ): Response<MyConnectionsResponse>


}