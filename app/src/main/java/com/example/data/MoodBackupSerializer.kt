package com.example.data

import com.example.model.MoodEntry
import com.example.util.DateTimeUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface ImportValidationResult {
  data class Valid(val entries: List<MoodEntry>) : ImportValidationResult
  data class Invalid(val errorMessage: String) : ImportValidationResult
}

object MoodBackupSerializer {

  fun exportJson(entries: List<MoodEntry>, nowMillis: Long = System.currentTimeMillis()): String {
    val root = JSONObject().apply {
      put("app", "MOODY")
      put("version", 1)
      put("exportedAt", DateTimeUtils.formatIsoUtc(nowMillis))
      val array = JSONArray()
      entries.sortedByDescending { it.timestamp }.forEach { entry ->
        val obj = JSONObject().apply {
          put("id", entry.id)
          put("mood", entry.mood)
          put("timestamp", entry.timestamp)
          put("isoTimestamp", DateTimeUtils.formatIsoUtc(entry.timestamp))
          put("tags", JSONArray(entry.tags))
          put("note", entry.note)
          put("isDemo", entry.isDemo)
        }
        array.put(obj)
      }
      put("entries", array)
    }
    return root.toString(2)
  }

  fun exportCsv(entries: List<MoodEntry>): String {
    val sb = StringBuilder()
    sb.append("timestamp,mood,tags,note\n")
    entries.sortedByDescending { it.timestamp }.forEach { entry ->
      val iso = DateTimeUtils.formatIsoUtc(entry.timestamp)
      val tagsJoined = entry.tags.joinToString("; ")
      sb.append(escapeCsv(iso))
      sb.append(',')
      sb.append(entry.mood)
      sb.append(',')
      sb.append(escapeCsv(tagsJoined))
      sb.append(',')
      sb.append(escapeCsv(entry.note))
      sb.append('\n')
    }
    return sb.toString()
  }

  fun parseAndValidateJson(rawJson: String): ImportValidationResult {
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) {
      return ImportValidationResult.Invalid("The selected file is empty.")
    }

    val entriesArray: JSONArray = try {
      when {
        trimmed.startsWith("{") -> {
          val root = JSONObject(trimmed)
          if (!root.has("entries")) {
            return ImportValidationResult.Invalid("Invalid MOODY backup: missing 'entries' array.")
          }
          val candidate = root.optJSONArray("entries")
            ?: return ImportValidationResult.Invalid("Invalid MOODY backup: 'entries' must be a JSON array.")
          candidate
        }
        trimmed.startsWith("[") -> JSONArray(trimmed)
        else -> return ImportValidationResult.Invalid("File is not valid JSON.")
      }
    } catch (e: Exception) {
      return ImportValidationResult.Invalid("Malformed JSON: ${e.localizedMessage ?: "syntax error"}")
    }

    if (entriesArray.length() == 0) {
      return ImportValidationResult.Invalid("The backup file contains 0 mood entries.")
    }

    val parsedEntries = mutableListOf<MoodEntry>()
    val seenIds = mutableSetOf<String>()

    for (i in 0 until entriesArray.length()) {
      val obj = entriesArray.optJSONObject(i)
        ?: return ImportValidationResult.Invalid("Entry #${i + 1} is not a valid JSON object.")

      if (!obj.has("mood")) {
        return ImportValidationResult.Invalid("Entry #${i + 1} is missing the required 'mood' field.")
      }
      val moodRaw = obj.opt("mood")
      val mood = when (moodRaw) {
        is Int -> moodRaw
        is Number -> moodRaw.toInt()
        else -> return ImportValidationResult.Invalid("Entry #${i + 1} has a non-numeric 'mood' value.")
      }
      if (mood !in 1..5) {
        return ImportValidationResult.Invalid("Entry #${i + 1} has invalid mood $mood (must be between 1 and 5).")
      }

      val timestamp = extractTimestamp(obj)
        ?: return ImportValidationResult.Invalid("Entry #${i + 1} is missing a valid 'timestamp' or 'isoTimestamp'.")

      val rawId = obj.optString("id", "").trim()
      val id = if (rawId.isNotEmpty() && seenIds.add(rawId)) {
        rawId
      } else {
        UUID.randomUUID().toString().also { seenIds.add(it) }
      }

      val note = obj.optString("note", "")
      val isDemo = obj.optBoolean("isDemo", false)
      val tags = parseTagsField(obj.opt("tags"))

      parsedEntries.add(
        MoodEntry(
          id = id,
          mood = mood,
          timestamp = timestamp,
          note = note,
          tags = tags,
          isDemo = isDemo,
          createdAt = timestamp,
          updatedAt = System.currentTimeMillis(),
        ),
      )
    }

    return ImportValidationResult.Valid(parsedEntries)
  }

  private fun extractTimestamp(obj: JSONObject): Long? {
    if (obj.has("timestamp")) {
      val raw = obj.opt("timestamp")
      when (raw) {
        is Number -> {
          val v = raw.toLong()
          if (v > 0L) return v
        }
        is String -> {
          raw.toLongOrNull()?.let { if (it > 0L) return it }
          DateTimeUtils.parseIsoUtcOrNull(raw)?.let { return it }
        }
      }
    }
    if (obj.has("isoTimestamp")) {
      val iso = obj.optString("isoTimestamp", "")
      if (iso.isNotBlank()) {
        DateTimeUtils.parseIsoUtcOrNull(iso)?.let { return it }
      }
    }
    return null
  }

  private fun parseTagsField(raw: Any?): List<String> =
    when (raw) {
      is JSONArray -> buildList {
        for (i in 0 until raw.length()) {
          val tag = raw.optString(i, "").trim().lowercase()
          if (tag.isNotEmpty() && tag !in this) add(tag)
        }
      }
      is String -> raw.split(",", ";")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
      else -> emptyList()
    }

  private fun escapeCsv(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    if (!needsQuotes) return value
    return "\"${value.replace("\"", "\"\"")}\""
  }
}
