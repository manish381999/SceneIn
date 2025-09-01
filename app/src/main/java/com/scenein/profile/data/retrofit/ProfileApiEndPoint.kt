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
import retrofit2.http.POST

interface ProfileApiEndPoint {

    @POST("api_v1/get_my_profile.php")
    suspend fun getMyProfile(): Response<MyProfileResponse>


    @FormUrlEncoded
    @POST("api_v1/get_public_user_profile.php")
    suspend fun getPublicUserProfile(@Field("profile_id") profileId: String): Response<GetUserResponse>


    @FormUrlEncoded
    @POST("api_v1/get_events_by_user.php")
    suspend fun getEventsByUser(
        // The user_id is now nullable. If you pass null, the backend will fetch
        // events for the currently authenticated user. If you provide an ID,
        // it will fetch events for that specific user (for public profiles).
        @Field("user_id") userId: String?
    ): Response<GetEventsByUserResponse>

    @FormUrlEncoded
    @POST("api_v1/check_connection_status.php")
    suspend fun checkConnectionStatus(
        @Field("profile_id") profileId: String   // The profile being viewed
    ): Response<ConnectionStatusResponse>

    @FormUrlEncoded
    @POST("api_v1/send_connection_request.php")
    suspend fun sendConnectionRequest(
        @Field("receiver_id") receiverId: String
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("api_v1/remove_connection.php")
    suspend fun removeConnection(
        @Field("other_user_id") otherUserId: String
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("api_v1/respond_to_connection_request.php")
    suspend fun respondToConnectionRequest(
        @Field("connection_id") connectionId: String,
        @Field("response") response: String // "accepted" or "declined"
    ): Response<ConnectionActionResponse>


    @POST("api_v1/get_my_tickets_activity.php")
    suspend fun getMyTicketsActivity(): Response<MyTicketsActivityResponse>

    // For the Profile "Connections" Tab
    @FormUrlEncoded
    @POST("api_v1/get_my_connections.php")
    suspend fun getMyConnections(@Field("user_id") userId: String?
    ): Response<MyConnectionsResponse>


}