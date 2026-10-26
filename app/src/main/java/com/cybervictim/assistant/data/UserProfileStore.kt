package com.cybervictim.assistant.data

import android.content.Context
import com.cybervictim.assistant.ai.CaseSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class UserProfile(
    val fullName: String = "",
    val mobile: String = "",
    val gender: String = "Male",
    val dob: String = "",
    val email: String = "",
    val state: String = "",
    val district: String = "",
    val policeStation: String = "",
    val address: String = "",
    val pincode: String = "",
    val ackNumber: String = "",
    val complaintStatus: String = "Not Filed",
    val victimId: String = "", // AI-Generated Unique ID for Victim Identification
)

object UserProfileStore {
    private const val PREF_NAME = "victim_user_profile"
    private const val KEY_NAME = "full_name"
    private const val KEY_MOBILE = "mobile"
    private const val KEY_GENDER = "gender"
    private const val KEY_DOB = "dob"
    private const val KEY_EMAIL = "email"
    private const val KEY_STATE = "state"
    private const val KEY_DISTRICT = "district"
    private const val KEY_POLICE_STATION = "police_station"
    private const val KEY_ADDRESS = "address"
    private const val KEY_PINCODE = "pincode"
    private const val KEY_ACK = "ack_number"
    private const val KEY_STATUS = "complaint_status"
    private const val KEY_VICTIM_ID = "victim_id"

    /**
     * AI Agent automatically generates a unique victim identification ID
     * for mobile application tracking and official cyber portal complaint mapping.
     */
    fun generateAiVictimId(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val randDigits = (10000..99999).random()
        val hexToken = java.util.UUID.randomUUID().toString().substring(0, 4).uppercase()
        return "VIC-$year-$randDigits-$hexToken"
    }

    fun get(context: Context): UserProfile {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return UserProfile(
            fullName = sp.getString(KEY_NAME, "") ?: "",
            mobile = sp.getString(KEY_MOBILE, "") ?: "",
            gender = sp.getString(KEY_GENDER, "Male") ?: "Male",
            dob = sp.getString(KEY_DOB, "") ?: "",
            email = sp.getString(KEY_EMAIL, "") ?: "",
            state = sp.getString(KEY_STATE, "") ?: "",
            district = sp.getString(KEY_DISTRICT, "") ?: "",
            policeStation = sp.getString(KEY_POLICE_STATION, "") ?: "",
            address = sp.getString(KEY_ADDRESS, "") ?: "",
            pincode = sp.getString(KEY_PINCODE, "") ?: "",
            ackNumber = sp.getString(KEY_ACK, "") ?: "",
            complaintStatus = sp.getString(KEY_STATUS, "Not Filed") ?: "Not Filed",
            victimId = sp.getString(KEY_VICTIM_ID, "") ?: "",
        )
    }

    /**
     * Ensures an AI-generated unique victim ID exists for the current user.
     */
    fun ensureVictimId(context: Context): String {
        val current = get(context)
        if (current.victimId.isNotBlank()) return current.victimId
        val generated = generateAiVictimId()
        save(context, current.copy(victimId = generated))
        return generated
    }

    fun save(context: Context, profile: UserProfile) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // If victimId is blank, AI automatically generates a unique ID for victim identification
        val finalVictimId = if (profile.victimId.isBlank()) {
            val existingId = sp.getString(KEY_VICTIM_ID, "") ?: ""
            if (existingId.isNotBlank()) existingId else generateAiVictimId()
        } else {
            profile.victimId
        }

        sp.edit()
            .putString(KEY_NAME, profile.fullName)
            .putString(KEY_MOBILE, profile.mobile)
            .putString(KEY_GENDER, profile.gender)
            .putString(KEY_DOB, profile.dob)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_STATE, profile.state)
            .putString(KEY_DISTRICT, profile.district)
            .putString(KEY_POLICE_STATION, profile.policeStation)
            .putString(KEY_ADDRESS, profile.address)
            .putString(KEY_PINCODE, profile.pincode)
            .putString(KEY_ACK, profile.ackNumber)
            .putString(KEY_STATUS, profile.complaintStatus)
            .putString(KEY_VICTIM_ID, finalVictimId)
            .commit()

        // Sync to active case in CaseDb and CaseSessionStore
        val caseId = CaseSession.ensure()
        val db = CaseDb.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            if (finalVictimId.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "other", valueText = "Victim App ID: $finalVictimId", sourceEvidenceId = 0, confidence = 1f))
            }
            if (profile.fullName.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "victim_name", valueText = profile.fullName, sourceEvidenceId = 0, confidence = 1f))
                CaseSessionStore.current?.onUserMessage("Complainant Name: ${profile.fullName}")
            }
            if (profile.mobile.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "phone", valueText = profile.mobile, sourceEvidenceId = 0, confidence = 1f))
                CaseSessionStore.current?.onUserMessage("Complainant Mobile: ${profile.mobile}")
            }
            if (profile.email.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "other", valueText = "Email: ${profile.email}", sourceEvidenceId = 0, confidence = 1f))
            }
            if (profile.state.isNotBlank()) {
                db.dao().addFact(CaseFact(caseId = caseId, type = "other", valueText = "State: ${profile.state}, District: ${profile.district}, Police Station: ${profile.policeStation}, Pincode: ${profile.pincode}", sourceEvidenceId = 0, confidence = 1f))
            }
        }
    }
}
