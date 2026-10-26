package com.cybervictim.assistant.portal

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Engine that maps case facts + scenario data to concrete form-fill values
 * for the cybercrime.gov.in portal. Works in two modes:
 *
 * 1. DETERMINISTIC: Uses scenario_buttons.json portal_fields + extracted CaseFacts
 *    to build a field→value map without calling AI.
 * 2. GEMINI-ASSISTED: Sends the DOM field snapshot + case text to Gemini and gets
 *    back a precise field→value JSON map.
 *
 * ActionPolicy is enforced at every step: captcha/OTP/password/submit/preview
 * fields are NEVER auto-filled.
 */
object PortalAutoFillEngine {

    /**
     * Represents a single form field found on the current portal page.
     */
    data class PageField(
        val id: String,
        val name: String,
        val tag: String,       // input | select | textarea
        val type: String,      // text | email | select-one | datetime-local | file | checkbox | radio | textarea
        val required: Boolean,
        val options: List<Pair<String, String>> = emptyList(), // (value, text) for selects
        val currentValue: String = "",
        val label: String = "",
    )

    /**
     * A fill instruction: which field to set and to what value.
     */
    data class FillAction(
        val fieldId: String,
        val fieldName: String,
        val value: String,
        val owner: StepOwner,
        val hint: String = "",   // shown to victim for HUMAN fields
    )

    /**
     * Result of an auto-fill analysis for a page.
     */
    data class FillPlan(
        val actions: List<FillAction>,
        val humanFields: List<FillAction>,     // fields victim must fill (captcha, OTP, passwords)
        val lockedButtons: List<String>,        // buttons victim must press (submit, preview)
        val statusMessage: String,
    )

    // ── JS to scan the current page DOM and extract all form fields ──

    val DOM_SCANNER_JS = """
    (function() {
        var result = { fields: [], buttons: [], url: window.location.href, title: document.title };
        
        // Scan all inputs, selects, textareas
        var els = document.querySelectorAll('input, select, textarea');
        for (var i = 0; i < els.length; i++) {
            var el = els[i];
            if (el.type === 'hidden' && !el.id && !el.name) continue;
            var field = {
                id: el.id || '',
                name: el.name || '',
                tag: el.tagName.toLowerCase(),
                type: el.type || el.tagName.toLowerCase(),
                required: el.required || false,
                value: el.value || '',
                label: '',
                visible: !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length),
                options: []
            };
            // Get label
            if (el.id) {
                var lbl = document.querySelector('label[for="' + el.id + '"]');
                if (lbl) field.label = lbl.textContent.trim();
            }
            if (!field.label && el.parentElement) {
                var parentLabel = el.parentElement.querySelector('label');
                if (parentLabel) field.label = parentLabel.textContent.trim();
            }
            if (!field.label && el.placeholder) field.label = el.placeholder;
            // Get select options
            if (el.tagName === 'SELECT') {
                var opts = el.options;
                for (var j = 0; j < opts.length; j++) {
                    field.options.push({ v: opts[j].value, t: opts[j].text.trim() });
                }
            }
            result.fields.push(field);
        }
        
        // Scan buttons
        var btns = document.querySelectorAll('button, input[type="submit"], input[type="button"], a.btn, .btn');
        for (var b = 0; b < btns.length; b++) {
            var btn = btns[b];
            var text = (btn.textContent || btn.value || '').trim();
            if (text) {
                result.buttons.push({
                    text: text,
                    id: btn.id || '',
                    type: btn.type || '',
                    visible: !!(btn.offsetWidth || btn.offsetHeight || btn.getClientRects().length)
                });
            }
        }
        
        return JSON.stringify(result);
    })();
    """.trimIndent()

    // ── JS template to fill a single field with Angular/React change event triggers ──

    fun buildFillJS(fieldId: String, fieldName: String, value: String, tag: String, type: String): String {
        val escapedValue = value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

        val selector = when {
            fieldId.isNotBlank() -> "document.getElementById('$fieldId')"
            fieldName.isNotBlank() -> "document.querySelector('[name=\"$fieldName\"]')"
            else -> "null"
        }

        return when {
            tag == "select" || type == "select-one" -> """
                (function() {
                    var el = $selector;
                    if (!el) return 'NOT_FOUND';
                    try {
                        el.style.outline = '4px solid #16a34a';
                        el.style.backgroundColor = '#dcfce7';
                        el.style.boxShadow = '0 0 16px #22c55e';
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    } catch(e) {}
                    var val_ = '$escapedValue';
                    // Try exact value match first
                    for (var i = 0; i < el.options.length; i++) {
                        if (el.options[i].value === val_ || el.options[i].text.trim().toUpperCase() === val_.toUpperCase()) {
                            el.selectedIndex = i;
                            el.value = el.options[i].value;
                            break;
                        }
                    }
                    // Trigger Angular/React change detection
                    el.dispatchEvent(new Event('change', {bubbles: true}));
                    el.dispatchEvent(new Event('input', {bubbles: true}));
                    // Angular specific
                    if (typeof angular !== 'undefined') {
                        angular.element(el).triggerHandler('change');
                    }
                    // For Angular 2+
                    var ev = new Event('input', {bubbles: true});
                    Object.defineProperty(ev, 'target', {writable: false, value: el});
                    el.dispatchEvent(ev);
                    return 'OK:' + el.value;
                })();
            """.trimIndent()

            type == "checkbox" -> """
                (function() {
                    var el = $selector;
                    if (!el) return 'NOT_FOUND';
                    try {
                        el.style.outline = '4px solid #16a34a';
                        el.style.backgroundColor = '#dcfce7';
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    } catch(e) {}
                    if (!el.checked) {
                        el.click();
                    }
                    return 'OK:checked';
                })();
            """.trimIndent()

            else -> """
                (function() {
                    var el = $selector;
                    if (!el) return 'NOT_FOUND';
                    try {
                        el.style.outline = '4px solid #16a34a';
                        el.style.backgroundColor = '#dcfce7';
                        el.style.boxShadow = '0 0 16px #22c55e';
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    } catch(e) {}
                    // Use native input setter to bypass framework read-only
                    var nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype, 'value'
                    );
                    if (nativeInputValueSetter && nativeInputValueSetter.set) {
                        nativeInputValueSetter.set.call(el, '$escapedValue');
                    } else {
                        el.value = '$escapedValue';
                    }
                    // For textarea
                    if (el.tagName === 'TEXTAREA') {
                        var taSetter = Object.getOwnPropertyDescriptor(
                            window.HTMLTextAreaElement.prototype, 'value'
                        );
                        if (taSetter && taSetter.set) {
                            taSetter.set.call(el, '$escapedValue');
                        } else {
                            el.value = '$escapedValue';
                        }
                    }
                    el.dispatchEvent(new Event('input', {bubbles: true}));
                    el.dispatchEvent(new Event('change', {bubbles: true}));
                    el.dispatchEvent(new Event('blur', {bubbles: true}));
                    // Angular 2+ compat
                    el.dispatchEvent(new Event('keyup', {bubbles: true}));
                    return 'OK:' + el.value.substring(0, 30);
                })();
            """.trimIndent()
        }
    }

    // ── JS to click a button by text content ──

    fun buildClickJS(buttonText: String): String {
        val escaped = buttonText.replace("'", "\\'")
        return """
            (function() {
                var btns = document.querySelectorAll('button, input[type="submit"], input[type="button"], a.btn, .btn, a[role="button"]');
                for (var i = 0; i < btns.length; i++) {
                    var t = (btns[i].textContent || btns[i].value || '').trim();
                    if (t.toLowerCase().indexOf('$escaped'.toLowerCase()) >= 0) {
                        try {
                            btns[i].style.outline = '4px solid #16a34a';
                            btns[i].style.backgroundColor = '#dcfce7';
                            btns[i].style.boxShadow = '0 0 16px #22c55e';
                            btns[i].scrollIntoView({ behavior: 'smooth', block: 'center' });
                        } catch(e) {}
                        btns[i].click();
                        return 'CLICKED:' + t;
                    }
                }
                // Also try links with matching text
                var links = document.querySelectorAll('a');
                for (var j = 0; j < links.length; j++) {
                    var lt = (links[j].textContent || '').trim();
                    if (lt.toLowerCase().indexOf('$escaped'.toLowerCase()) >= 0) {
                        try {
                            links[j].style.outline = '4px solid #16a34a';
                            links[j].style.backgroundColor = '#dcfce7';
                            links[j].style.boxShadow = '0 0 16px #22c55e';
                            links[j].scrollIntoView({ behavior: 'smooth', block: 'center' });
                        } catch(e) {}
                        links[j].click();
                        return 'CLICKED_LINK:' + lt;
                    }
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }

    // ── JS to highlight a field that needs human input ──

    fun buildHighlightJS(fieldId: String, fieldName: String, hint: String): String {
        val selector = when {
            fieldId.isNotBlank() -> "document.getElementById('$fieldId')"
            fieldName.isNotBlank() -> "document.querySelector('[name=\"$fieldName\"]')"
            else -> "null"
        }
        val escapedHint = hint.replace("'", "\\'")
        return """
            (function() {
                var el = $selector;
                if (!el) return 'NOT_FOUND';
                el.style.border = '3px solid #FF5722';
                el.style.boxShadow = '0 0 10px rgba(255,87,34,0.5)';
                el.placeholder = '$escapedHint';
                el.focus();
                el.scrollIntoView({behavior: 'smooth', block: 'center'});
                return 'HIGHLIGHTED';
            })();
        """.trimIndent()
    }

    // ── JS to disable submit/preview buttons (VICTIM_LOCK enforcement) ──

    val LOCK_SUBMIT_JS = """
        (function() {
            var locked = [];
            var btns = document.querySelectorAll('button, input[type="submit"]');
            for (var i = 0; i < btns.length; i++) {
                var t = (btns[i].textContent || btns[i].value || '').trim().toLowerCase();
                if (t.indexOf('submit') >= 0 || t.indexOf('preview') >= 0 || t.indexOf('final') >= 0) {
                    btns[i].style.border = '3px solid #D32F2F';
                    btns[i].style.boxShadow = '0 0 12px rgba(211,47,47,0.6)';
                    locked.push(t);
                }
            }
            return JSON.stringify(locked);
        })();
    """.trimIndent()

    // ── Parse DOM scan result ──

    fun parsePageFields(scanJson: String): Triple<List<PageField>, List<JSONObject>, String> {
        return try {
            val root = JSONObject(scanJson)
            val fieldsArr = root.optJSONArray("fields") ?: JSONArray()
            val buttonsArr = root.optJSONArray("buttons") ?: JSONArray()
            val url = root.optString("url", "")

            val fields = (0 until fieldsArr.length()).mapNotNull { i ->
                val f = fieldsArr.getJSONObject(i)
                val id = f.optString("id")
                val name = f.optString("name")
                if (id.isBlank() && name.isBlank()) return@mapNotNull null
                val visible = f.optBoolean("visible", true)
                if (!visible) return@mapNotNull null
                val options = mutableListOf<Pair<String, String>>()
                val opts = f.optJSONArray("options")
                if (opts != null) {
                    for (j in 0 until opts.length()) {
                        val o = opts.getJSONObject(j)
                        options.add(o.optString("v") to o.optString("t"))
                    }
                }
                PageField(
                    id = id, name = name,
                    tag = f.optString("tag"),
                    type = f.optString("type"),
                    required = f.optBoolean("required"),
                    options = options,
                    currentValue = f.optString("value"),
                    label = f.optString("label"),
                )
            }
            val buttons = (0 until buttonsArr.length()).map { buttonsArr.getJSONObject(it) }
            Triple(fields, buttons, url)
        } catch (e: Exception) {
            Triple(emptyList(), emptyList(), "")
        }
    }

    // ── Build fill plan using Gemini ──

    fun buildFillPlanWithGemini(
        pageFields: List<PageField>,
        caseText: String,
        scenarioJson: String?,
        onPlan: (FillPlan) -> Unit,
    ) {
        Thread {
            try {
                val plan = buildFillPlanInternal(pageFields, caseText, scenarioJson)
                Handler(Looper.getMainLooper()).post { onPlan(plan) }
            } catch (e: Exception) {
                val fallback = buildDeterministicFillPlan(pageFields, caseText)
                Handler(Looper.getMainLooper()).post { onPlan(fallback) }
            }
        }.start()
    }

    private fun buildFillPlanInternal(
        pageFields: List<PageField>,
        caseText: String,
        scenarioJson: String?,
    ): FillPlan {
        // Classify fields by ActionPolicy first
        val aiFields = mutableListOf<PageField>()
        val humanFields = mutableListOf<FillAction>()
        val lockedButtons = mutableListOf<String>()

        for (f in pageFields) {
            val targetId = f.id.ifBlank { f.name }
            val owner = ActionPolicy.ownerOf(targetId, f.tag)
            when (owner) {
                StepOwner.VICTIM_LOCK -> lockedButtons.add(targetId)
                StepOwner.HUMAN -> humanFields.add(
                    FillAction(f.id, f.name, "", StepOwner.HUMAN,
                        hint = "Please enter ${f.label.ifBlank { f.name }} manually")
                )
                StepOwner.AI_AUTO -> aiFields.add(f)
            }
        }

        // Build Gemini prompt for AI_AUTO fields
        if (aiFields.isEmpty()) {
            return FillPlan(emptyList(), humanFields, lockedButtons, "No auto-fillable fields found on this page.")
        }

        val fieldsDesc = buildString {
            for (f in aiFields) {
                append("- Field: id='${f.id}' name='${f.name}' tag=${f.tag} type=${f.type}")
                append(" label='${f.label}' required=${f.required}")
                if (f.options.isNotEmpty()) {
                    append(" options=[")
                    append(f.options.take(40).joinToString(", ") { "(value='${it.first}', text='${it.second}')" })
                    append("]")
                }
                appendLine()
            }
        }

        val scenarioHint = if (!scenarioJson.isNullOrBlank()) {
            "\nMATCHED SCENARIO FROM KB:\n$scenarioJson\n"
        } else ""

        val prompt = """
            You are filling a cybercrime complaint form on https://cybercrime.gov.in for a victim.
            Using the victim's case facts, determine what value to put in each form field.
            $scenarioHint
            VICTIM'S CASE TEXT:
            $caseText

            FORM FIELDS TO FILL (ONLY these — never fill captcha/OTP/password/submit):
            $fieldsDesc

            Respond ONLY as a JSON array — no commentary:
            [{"id":"fieldId","name":"fieldName","value":"value to fill","reason":"why this value"}]

            Rules:
            - For SELECT fields, the value MUST be one of the provided option values (the 'v' part).
            - For date fields, use the format expected (datetime-local or dd/mm/yyyy).
            - If you don't have enough info to fill a field, set value to "" (empty).
            - For the crime description/additional info textarea, compose a clear factual summary from the case text.
            - Use EXACT option values for select dropdowns.
            - Never fill captcha, OTP, password, username, or login fields.
        """.trimIndent()

        val raw = com.cybervictim.assistant.ai.GeminiClient.generateWithFallback(prompt, maxTokens = 2000)
        val actions = parseGeminiFillResponse(raw, aiFields)

        return FillPlan(
            actions = actions,
            humanFields = humanFields,
            lockedButtons = lockedButtons,
            statusMessage = if (actions.isEmpty()) "Could not determine values for form fields."
            else "Ready to auto-fill ${actions.size} fields. ${humanFields.size} fields need your input."
        )
    }

    private fun parseGeminiFillResponse(raw: String?, fields: List<PageField>): List<FillAction> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            // Find JSON array in response
            val start = raw.indexOf('[')
            val end = raw.lastIndexOf(']')
            if (start < 0 || end <= start) return emptyList()
            val arr = JSONArray(raw.substring(start, end + 1))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                val name = o.optString("name")
                val value = o.optString("value")
                if (value.isBlank()) return@mapNotNull null
                // Verify field exists and is AI_AUTO
                val targetKey = id.ifBlank { name }
                if (targetKey.isBlank()) return@mapNotNull null
                val owner = ActionPolicy.ownerOf(targetKey)
                if (owner != StepOwner.AI_AUTO) return@mapNotNull null
                FillAction(id, name, value, StepOwner.AI_AUTO, o.optString("reason"))
            }
        } catch (_: Exception) { emptyList() }
    }

    /** Fallback: deterministic fill using just case text keyword extraction */
    private fun buildDeterministicFillPlan(pageFields: List<PageField>, caseText: String): FillPlan {
        val actions = mutableListOf<FillAction>()
        val humanFields = mutableListOf<FillAction>()
        val lockedButtons = mutableListOf<String>()

        for (f in pageFields) {
            val targetId = f.id.ifBlank { f.name }
            val owner = ActionPolicy.ownerOf(targetId, f.tag)
            when (owner) {
                StepOwner.VICTIM_LOCK -> lockedButtons.add(targetId)
                StepOwner.HUMAN -> humanFields.add(
                    FillAction(f.id, f.name, "", StepOwner.HUMAN,
                        hint = "Please enter ${f.label.ifBlank { f.name }} manually")
                )
                StepOwner.AI_AUTO -> {
                    // Simple deterministic fill for common fields
                    val label = (f.label + " " + f.name + " " + f.id).lowercase()
                    val value = when {
                        label.contains("additional") && f.tag == "textarea" -> caseText.take(4000)
                        label.contains("description") && f.tag == "textarea" -> caseText.take(4000)
                        label.contains("crimeadditionalinfo") -> caseText.take(4000)
                        else -> ""
                    }
                    if (value.isNotBlank()) {
                        actions.add(FillAction(f.id, f.name, value, StepOwner.AI_AUTO))
                    }
                }
            }
        }

        return FillPlan(actions, humanFields, lockedButtons,
            "Deterministic fill: ${actions.size} fields. ${humanFields.size} need your input.")
    }
}
