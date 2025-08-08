package com.tie.vibein.profile.data.retrofit


import com.tie.vibein.profile.data.models.ConnectionActionResponse
import com.tie.vibein.profile.data.models.ConnectionStatusResponse
import com.tie.vibein.profile.data.models.GetEventsByUserResponse
import com.tie.vibein.profile.data.models.GetUserResponse
import com.tie.vibein.profile.data.models.MyConnectionsResponse
import com.tie.vibein.profile.data.models.MyProfileResponse
import com.tie.vibein.profile.data.models.MyTicketsActivityResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ProfileApiEndPoint {

    @FormUrlEncoded
    @POST("api_v1/get_my_profile.php")
    suspend fun getMyProfile(@Field("user_id") userId: String): Response<MyProfileResponse>


    @FormUrlEncoded
    @POST("api_v1/get_public_user_profile.php")
    suspend fun getPublicUserProfile(@Field("user_id") userId: String): Response<GetUserResponse>


    @FormUrlEncoded
    @POST("api_v1/get_events_by_user.php")
    suspend fun getEventsByUser(
        @Field("user_id") userId: String
    ): Response<GetEventsByUserResponse>

    @FormUrlEncoded
    @POST("api_v1/check_connection_status.php")
    suspend fun checkConnectionStatus(
        @Field("viewer_id") viewerId: String,   // The person looking at the profile
        @Field("profile_id") profileId: String   // The profile being viewed
    ): Response<ConnectionStatusResponse>

    @FormUrlEncoded
    @POST("api_v1/send_connection_request.php")
    suspend fun sendConnectionRequest(
        @Field("sender_id") senderId: String,
        @Field("receiver_id") receiverId: String
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("api_v1/remove_connection.php")
    suspend fun removeConnection(
        @Field("current_user_id") currentUserId: String,
        @Field("user_to_disconnect_id") userToDisconnectId: String
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("api_v1/respond_to_connection_request.php")
    suspend fun respondToConnectionRequest(
        @Field("connection_id") connectionId: String,
        @Field("response") response: String // "accepted" or "declined"
    ): Response<ConnectionActionResponse>

    @FormUrlEncoded
    @POST("api_v1/get_my_tickets_activity.php")
    suspend fun getMyTicketsActivity(@Field("user_id") userId: String): Response<MyTicketsActivityResponse>

    // For the Profile "Connections" Tab
    @FormUrlEncoded
    @POST("api_v1/get_my_connections.php")
    suspend fun getMyConnections(@Field("user_id") userId: String): Response<MyConnectionsResponse>


}