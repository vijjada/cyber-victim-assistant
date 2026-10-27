package com.cybervictim.assistant.voice

import com.cybervictim.assistant.portal.StepOwner
import org.json.JSONArray
import org.json.JSONObject

/**
 * ScreenDescriptor captures the current screen's context for Voice Guide.
 * Hard rule: Never send user-entered field values or audio off-device.
 * Only field NAMES and button NAMES are described.
 */
data class ScreenDescriptor(
    val screenId: String,
    val title: String,
    val stepIndex: Int = -1,
    val stepTitle: String = "",
    val buttons: List<String> = emptyList(),
    val fields: List<String> = emptyList(),
    val owner: StepOwner = StepOwner.AI_AUTO,
    val hintKey: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("screenId", screenId)
        put("title", title)
        put("stepIndex", stepIndex)
        put("stepTitle", stepTitle)
        put("buttons", JSONArray(buttons))
        put("fields", JSONArray(fields))
        put("owner", owner.name)
        put("hintKey", hintKey)
    }
}
