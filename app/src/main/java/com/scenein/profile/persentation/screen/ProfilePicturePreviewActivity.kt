package com.scenein.profile.persentation.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.scenein.utils.SP
import com.scenein.R
import com.scenein.databinding.ActivityProfilePicturePreviewBinding
import com.scenein.profile.persentation.view_model.ProfileViewModel
import com.scenein.utils.NetworkState
import jp.wasabeef.glide.transformations.BlurTransformation

class ProfilePicturePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePicturePreviewBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var currentUserId: String

    // The data for this screen is now simple and passed directly
    private var profileUserId: String? = null
    private var profileUserName: String? = null
    private var profilePicUrl: String? = null
    private var profileFullName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePicturePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""

        // --- THIS IS THE DEFINITIVE FIX: Get individual string extras ---
        profileUserId = intent.getStringExtra(EXTRA_USER_ID)
        profileUserName = intent.getStringExtra(EXTRA_USERNAME)
        profilePicUrl = intent.getStringExtra(EXTRA_PROFILE_PIC_URL)
        profileFullName = intent.getStringExtra(EXTRA_FULL_NAME)

        if (profileUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Could not load user data.", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        populateUi()
        setupClickListeners()
        setupObservers()

        // Only check connection status if we are viewing someone else's profile
        if (profileUserId != currentUserId) {
            profileViewModel.checkConnectionStatus( profileUserId!!)
        }
    }

    private fun populateUi() {
        Glide.with(this).load(profilePicUrl).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivProfilePreview)
        Glide.with(this).load(profilePicUrl).apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3))).into(binding.ivBlurredBackground)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { finish() }
        binding.rootLayout.setOnClickListener { finish() }

        binding.actionConnect.setOnClickListener {
            val status = (profileViewModel.connectionStatusState.value as? NetworkState.Success)?.data?.connectionStatus
            val pId = profileUserId ?: return@setOnClickListener
            if (pId == currentUserId) return@setOnClickListener

            when (status) {
                "none", "declined" -> profileViewModel.sendConnectionRequest(pId)
                "pending" -> profileViewModel.removeConnection( pId)
                "accepted" -> profileViewModel.removeConnection( pId)
            }
        }

        binding.actionShare.setOnClickListener {
            val profileUrl = "https://vibein.app/user/${profileUserName ?: profileUserId}"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out ${profileFullName}'s profile on VibeIn!\n$profileUrl")
            }
            startActivity(Intent.createChooser(shareIntent, "Share profile via..."))
        }

        binding.actionCopyLink.setOnClickListener {
            val profileUrl = "https://vibein.app/user/${profileUserName ?: profileUserId}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("VibeIn Profile URL", profileUrl))
            Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.actionQrCode.setOnClickListener {
            Toast.makeText(this, "QR Code feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        profileViewModel.connectionStatusState.observe(this) { state ->
            val status = (state as? NetworkState.Success)?.data
            updateConnectActionUI(status?.connectionStatus, status?.requestSentBy)
        }
        profileViewModel.connectionActionState.observe(this) { state ->
            if (state !is NetworkState.Loading && !profileUserId.isNullOrEmpty()) {
                profileViewModel.checkConnectionStatus( profileUserId!!)
            }
        }
    }

    private fun updateConnectActionUI(status: String?, requestSentBy: String? = null) {
        val isMyOwnProfile = profileUserId == currentUserId

        binding.actionConnect.isVisible = !isMyOwnProfile
        if (isMyOwnProfile) return

        when (status) {
            "accepted" -> {
                binding.ivConnectIcon.setImageResource(R.drawable.ic_check_circle)
                binding.tvConnectLabel.text = "Connected"
            }
            "pending" -> {
                if (requestSentBy == "me") {
                    binding.ivConnectIcon.setImageResource(R.drawable.ic_person_pending)
                    binding.tvConnectLabel.text = "Requested"
                } else {
                    // They sent a request to me, so button should be "Connect Back"
                    binding.ivConnectIcon.setImageResource(R.drawable.ic_person_add)
                    binding.tvConnectLabel.text = "Connect Back"
                }

            }
            else -> { // "none" or "declined"
                binding.ivConnectIcon.setImageResource(R.drawable.ic_person_add)
                binding.tvConnectLabel.text = "Connect"
            }
        }
    }

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_FULL_NAME = "full_name"
        const val EXTRA_PROFILE_PIC_URL = "profile_pic_url"
    }
}