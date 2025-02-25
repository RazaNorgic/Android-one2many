package com.norgic.vdotokcall_mtm.ui.dashboard.fragment

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.projection.MediaProjection
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.norgic.callsdks.CallClient
import com.norgic.callsdks.enums.CallType
import com.norgic.callsdks.enums.MediaType
import com.norgic.callsdks.enums.SessionType
import com.norgic.callsdks.models.CallParams
import com.norgic.callsdks.models.SessionStateInfo
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.FragmentDialCallBinding
import com.norgic.vdotokcall_mtm.extensions.hide
import com.norgic.vdotokcall_mtm.extensions.launchPeriodicAsync
import com.norgic.vdotokcall_mtm.extensions.show
import com.norgic.vdotokcall_mtm.extensions.showSnackBar
import com.norgic.vdotokcall_mtm.fragments.CallMangerListenerFragment
import com.norgic.vdotokcall_mtm.models.AcceptCallModel
import com.norgic.vdotokcall_mtm.models.GroupModel
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity
import com.norgic.vdotokcall_mtm.utils.performSingleClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.webrtc.VideoTrack


/**
 * Created By: Norgic
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays incoming and outgoing call
 */
class DialCallFragment : CallMangerListenerFragment() {
    private var isIncomingCall: Boolean = false
    private lateinit var binding: FragmentDialCallBinding
    var groupModel : GroupModel? = null
    var username : String? = null

    var acceptCallModel : CallParams? = null
    private var groupList = ArrayList<GroupModel>()
    var isVideoCall: Boolean = false
    private var isInternalAudioIncluded = false
    var screenSharingApp :Boolean = false
    var screenSharingMic :Boolean = false
    var cameraCall :Boolean = false

    var userName : ObservableField<String> = ObservableField<String>()
    var incomingCallTitle : ObservableField<String> = ObservableField<String>()
    var player: MediaPlayer?= null
    private var timerFro30sec: Deferred<Unit> ?= null
    private var refList = ArrayList<String>()

    private lateinit var callClient: CallClient
    private lateinit var prefs: Prefs
    var participantsCount = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDialCallBinding.inflate(inflater, container, false)
        prefs = Prefs(activity)
        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        setArgumentsData()
        setBindingData()

        when {
            isIncomingCall -> setDataForIncomingCall()
            else -> setDataForDialCall()
        }


        if (isIncomingCall) {
            timerFro30sec = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(1000 * 15) {
                test++
                if (test >= 2) {
                    activity?.runOnUiThread {
                        rejectCall()
                    }
                    timerFro30sec?.cancel()
                }
            }
        }

        return binding.root
    }

    var test = 0
    /**
     * Function to link binding data
     * */
    private fun setBindingData() {
        binding.username = userName
        binding.incomingCallTitle = incomingCallTitle
    }

    /**
     * Function to get ths pass data from other fragment
     * */
    private fun setArgumentsData() {
        groupList.clear()
        arguments?.get(GroupModel.TAG)?.let {
            isVideoCall = arguments?.getBoolean(IS_VIDEO_CALL) ?: false
            groupModel = it as GroupModel?
            isIncomingCall = arguments?.get("isIncoming") as Boolean
            screenSharingApp = arguments?.getBoolean("screenApp")?: false
            screenSharingMic = arguments?.getBoolean("screenMic")?: false
            cameraCall = arguments?.getBoolean("video")?: false
            isInternalAudioIncluded = arguments?.getBoolean("internalAudio")?: false
        } ?: kotlin.run{
            groupList = arguments?.getParcelableArrayList<GroupModel>("grouplist") as ArrayList<GroupModel>
            username = arguments?.get("userName") as String?
            acceptCallModel = arguments?.get(AcceptCallModel.TAG) as CallParams?
            isIncomingCall =  arguments?.get("isIncoming") as Boolean
        }
    }
    /**
     * Function to set data when outgoing call dial is implemented and setonClickListener
     * */
    private fun setDataForDialCall() {

        getUsername()
        refList.clear()
        groupModel?.participants?.forEach {
            refList.add(it.refId!!)
        }
        binding.imgCallAccept.hide()
        binding.imgmic.show()
        binding.imgCamera.show()
        incomingCallTitle.set(getString(R.string.calling))

        binding.imgCallReject.performSingleClick {
            rejectCall()
        }

    }


    /**
     * Function to set user/users  name when outgoing call dial is implemented
     * */
    private fun getUsername() {
        groupModel.let { it ->
            if(groupModel?.autoCreated == 1){
                it?.participants?.forEach { name->
                    if (name.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        userName.set(name.fullname)

                    }
                }
            } else {
                var participantNames = ""
                it?.participants?.forEach {
                    if (it.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        participantNames += it.fullname.plus("\n")
                    }
                }
                userName.set(participantNames)
            }
        }
    }

    /**
     * Function to set data when incoming call dial is implemented and setonClickListener
     * */
    private fun setDataForIncomingCall() {

        player = MediaPlayer.create(this.requireContext(), Settings.System.DEFAULT_RINGTONE_URI)
        player?.start()
        if (username.isNullOrEmpty()){
            userName.set("User")
        }else{
        userName.set(username)
        }
        when (acceptCallModel?.sessionType) {
           SessionType.SCREEN -> {
                incomingCallTitle.set(getString(R.string.incoming_call))
            }
            else -> {
                incomingCallTitle.set(getString(R.string.incoming_video_call))
            }
        }

        binding.imgCallAccept.performSingleClick {
           acceptIncomingCall()
        }

        binding.imgCallReject.performSingleClick {
            rejectCall()
        }
    }

    fun rejectCall() {
        timerFro30sec?.cancel()
        if (isIncomingCall) {
            prefs.loginInfo?.let {
                acceptCallModel?.let { it1 -> callClient.rejectIncomingCall(
                    it.refId!!,
                    it1.sessionUUID
                )
                }
            }
        } else {
            (activity as DashBoardActivity).endCall()
        }
        try {
            Navigation.findNavController(binding.root).navigate(R.id.action_open_selection_fragment)
        } catch (e: Exception) {}
    }
    /**
     * Function to be call when incoming dial call is accepted
     * */
    private fun acceptIncomingCall() {

        acceptCallModel?.let {

            (activity as DashBoardActivity).acceptIncomingCall(
                it
            )
            openCallFragment()
        }
        timerFro30sec?.cancel()
    }

    override fun onDetach() {
        timerFro30sec?.cancel()
        super.onDetach()
        player?.stop()
    }

    override fun onDestroy() {
        timerFro30sec?.cancel()
        super.onDestroy()
        player?.stop()
    }

    /**
     * Function to pass data to oter fragment in case of incoming call dial
     * */
    private fun openCallFragment() {
        val bundle = Bundle()
        bundle.putParcelableArrayList("grouplist", groupList)
        bundle.putString("userName", userName.get())
        bundle.putBoolean(IS_VIDEO_CALL, acceptCallModel?.mediaType == MediaType.VIDEO)
        bundle.putParcelable(AcceptCallModel.TAG, acceptCallModel)
        bundle.putInt("participant",participantsCount)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_call_fragment, bundle)
    }

    companion object {
        const val IS_VIDEO_CALL = "IS_VIDEO_CALL"

        const val TAG = "DialCallFragment"
        @JvmStatic
        fun newInstance() = DialCallFragment()

    }

    override fun onIncomingCall(model: CallParams) {}



    override fun onStartCalling() {
        activity?.let {
            it.runOnUiThread {
                val bundle = Bundle()
                bundle.putParcelable(GroupModel.TAG, groupModel)
                bundle.putBoolean(IS_VIDEO_CALL, isVideoCall)
                bundle.putBoolean("isIncoming", false)
                bundle.putBoolean("screenApp",screenSharingApp)
                bundle.putBoolean("screenMic",screenSharingMic)
                bundle.putBoolean("video",cameraCall)
                bundle.putInt("participantsCount",participantsCount)
                bundle.putBoolean("internalAudio",isInternalAudioIncluded)
                Navigation.findNavController(binding.root).navigate(
                    R.id.action_open_call_fragment,
                    bundle
                )
            }
        }
    }

    override fun outGoingCall(toPeer: GroupModel) {
        closeFragmentWithMessage("Call Missed!")
    }

//    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {}

    override fun onCameraStreamReceived(stream: VideoTrack) {}
    override fun onCameraAudioOff(
        sessionStateInfo: SessionStateInfo, isMultySession: Boolean
    ) {}

    override fun onCallRejected(reason: String) {
//        closeFragmentWithMessage(reason)
    }

    override fun onParticipantLeftCall(refId: String?) {

    }

    override fun sessionStart(mediaProjection: MediaProjection?) {
        //        TODO("Not yet implemented")
    }

    override fun acceptedUser(participantCount: Int) {
        participantsCount = participantCount - 1
           }

    override fun onCallMissed() {
       closeFragmentWithMessage("Call Missed!")
    }

    override fun onCallEnd() {
        activity?.runOnUiThread {
            try {
                Navigation.findNavController(binding.root).navigate(R.id.action_open_selection_fragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPublicURL(publicURL: String) {
       //// TODO("Not yet implemented")
    }

    override fun checkCallType() {
//        if ((screenSharingApp && isInternalAudioIncluded && cameraCall) || (screenSharingMic && !isInternalAudioIncluded && cameraCall)
//            ||(screenSharingApp && !isInternalAudioIncluded && cameraCall)) {
//            dialOneToManyVideoCall(mediaType = MediaType.VIDEO,sessionType = SessionType.CALL, groupModel!!)
//        }
        if ((screenSharingApp && !isInternalAudioIncluded) || (screenSharingMic && !isInternalAudioIncluded)
            || (screenSharingApp && isInternalAudioIncluded)){
            moveToDashboard()
        }
    }

    private fun moveToDashboard() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }
    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            onCallEnd()
        }
    }
    private fun dialOneToManyVideoCall(mediaType: MediaType, sessionType: SessionType, groupModel: GroupModel) {

        val refIdList = groupModel.participants.map { it.refId } as java.util.ArrayList<String>
        refIdList.remove(prefs.loginInfo?.refId)

        if (callClient.isConnected() == true) {

            prefs.loginInfo?.let {
                (activity as DashBoardActivity).dialOne2ManyVideoCall(
                    CallParams(
                        refId = it.refId!!,
                        toRefIds = refIdList,
                        mcToken = it.mcToken!!,
                        mediaType = mediaType,
                        callType = CallType.ONE_TO_MANY,
                        sessionType = sessionType,
                        isAppAudio = isInternalAudioIncluded,
                        isBroadcast = 0
                    )
                )
            }
        } else {
            (activity as DashBoardActivity).connectClient()
        }
    }
}