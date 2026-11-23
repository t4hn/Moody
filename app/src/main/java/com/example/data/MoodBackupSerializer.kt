package com.example.data

import com.example.model.DailyContext
import com.example.model.MoodEntry
import com.example.model.PhysicalActivityLevel
import com.example.util.DateTimeUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface ImportValidationResult {
  data class Valid(
    val entries: List<MoodEntry>,
    val dailyContexts: List<DailyContext> = emptyList(),
  ) : ImportValidationResult

  data class Invalid(val errorMessage: String) : ImportValidationResult
}

object MoodBackupSerializer {

  fun exportJson(
    entries: List<MoodEntry>,
    dailyContexts: List<DailyContext> = emptyList(),
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val dailyContextMap = linkedMapOf<String, DailyContext>()
    dailyContexts.forEach { ctx ->
      if (ctx.hasAnyData) {
        dailyContextMap[ctx.dayKey] = ctx
      }
    }
    entries.forEach { entry ->
      if (entry.sleepDurationMinutes != null || entry.sleepQuality != null) {
        val dayKey = DateTimeUtils.formatFileDate(entry.timestamp)
        val existing = dailyContextMap[dayKey]
        dailyContextMap[dayKey] = DailyContext(
          dayKey = dayKey,
          sleepDurationMinutes = existing?.sleepDurationMinutes ?: entry.sleepDurationMinutes,
          sleepQuality = existing?.sleepQuality ?: entry.sleepQuality,
          updatedAt = existing?.updatedAt ?: entry.updatedAt,
        )
      }
    }

    val root = JSONObject().apply {
      put("app", "MOODY")
      put("version", 2)
      put("exportedAt", DateTimeUtils.formatIsoUtc(nowMillis))

      val contextsArray = JSONArray()
      dailyContextMap.values.sortedByDescending { it.dayKey }.forEach { ctx ->
        val ctxObj = JSONObject().apply {
          put("dayKey", ctx.dayKey)
          ctx.sleepDurationMinutes?.let { put("sleepDurationMinutes", it) }
          ctx.sleepQuality?.let { put("sleepQuality", it) }
        }
        contextsArray.put(ctxObj)
      }
      put("dailyContexts", contextsArray)

      val array = JSONArray()
      entries.sortedByDescending { it.timestamp }.forEach { entry ->
        val obj = JSONObject().apply {
          put("id", entry.id)
          put("mood", entry.mood)
          put("timestamp", entry.timestamp)
          put("isoTimestamp", DateTimeUtils.formatIsoUtc(entry.timestamp))
          put("tags", JSONArray(entry.tags))
          put("note", entry.note)
          entry.sleepDurationMinutes?.let { put("sleepDurationMinutes", it) }
          entry.sleepQuality?.let { put("sleepQuality", it) }
          entry.energyLevel?.let { put("energyLevel", it) }
          entry.stressLevel?.let { put("stressLevel", it) }
          entry.anxietyLevel?.let { put("anxietyLevel", it) }
          entry.physicalActivity?.let { put("physicalActivity", it.name) }
        }
        array.put(obj)
      }
      put("entries", array)
    }
    return root.toString(2)
  }

  fun exportCsv(entries: List<MoodEntry>): String {
    val sb = StringBuilder()
    sb.append("timestamp,mood,tags,note,sleep_duration_minutes,sleep_quality,energy_level,stress_level,anxiety_level,physical_activity\n")
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
      sb.append(',')
      sb.append(entry.sleepDurationMinutes?.toString().orEmpty())
      sb.append(',')
      sb.append(entry.sleepQuality?.toString().orEmpty())
      sb.append(',')
      sb.append(entry.energyLevel?.toString().orEmpty())
      sb.append(',')
      sb.append(entry.stressLevel?.toString().orEmpty())
      sb.append(',')
      sb.append(entry.anxietyLevel?.toString().orEmpty())
      sb.append(',')
      sb.append(escapeCsv(entry.physicalActivity?.name.orEmpty()))
      sb.append('\n')
    }
    return sb.toString()
  }

  fun parseAndValidateJson(rawJson: String): ImportValidationResult {
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) {
      return ImportValidationResult.Invalid("The selected file is empty.")
    }

    var dailyContextsArray: JSONArray? = null
    val entriesArray: JSONArray = try {
      when {
        trimmed.startsWith("{") -> {
          val root = JSONObject(trimmed)
          if (!root.has("entries")) {
            return ImportValidationResult.Invalid("Invalid MOODY backup: missing 'entries' array.")
          }
          dailyContextsArray = root.optJSONArray("dailyContexts")
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

    val dailyContextMap = linkedMapOf<String, DailyContext>()
    if (dailyContextsArray != null) {
      for (i in 0 until dailyContextsArray.length()) {
        val obj = dailyContextsArray.optJSONObject(i) ?: continue
        val dayKey = if (obj.isNull("dayKey")) "" else obj.optString("dayKey", "").trim()
        if (dayKey.isEmpty()) continue
        val sleepMins = extractOptionalInt(obj, "sleepDurationMinutes")?.takeIf { it in 0..(24 * 60) }
        val sleepQual = extractOptionalInt(obj, "sleepQuality")?.takeIf { it in 1..5 }
        if (sleepMins != null || sleepQual != null) {
          dailyContextMap[dayKey] = DailyContext(
            dayKey = dayKey,
            sleepDurationMinutes = sleepMins,
            sleepQuality = sleepQual,
          )
        }
      }
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

      val rawId = if (obj.isNull("id")) "" else obj.optString("id", "").trim()
      val id = if (rawId.isNotEmpty() && seenIds.add(rawId)) {
        rawId
      } else {
        UUID.randomUUID().toString().also { seenIds.add(it) }
      }

      val note = if (obj.isNull("note")) "" else obj.optString("note", "")
      val tags = parseTagsField(obj.opt("tags"))

      val sleepDurationMinutes = extractOptionalInt(obj, "sleepDurationMinutes")?.takeIf { it in 0..(24 * 60) }
      val sleepQuality = extractOptionalInt(obj, "sleepQuality")?.takeIf { it in 1..5 }
      val energyLevel = extractOptionalInt(obj, "energyLevel")?.takeIf { it in 1..5 }
      val stressLevel = extractOptionalInt(obj, "stressLevel")?.takeIf { it in 1..5 }
      val anxietyLevel = extractOptionalInt(obj, "anxietyLevel")?.takeIf { it in 1..5 }
      val physicalActivity = PhysicalActivityLevel.fromNameOrNull(
        if (obj.isNull("physicalActivity")) null else obj.optString("physicalActivity", ""),
      )

      if (sleepDurationMinutes != null || sleepQuality != null) {
        val dayKey = DateTimeUtils.formatFileDate(timestamp)
        val existing = dailyContextMap[dayKey]
        dailyContextMap[dayKey] = DailyContext(
          dayKey = dayKey,
          sleepDurationMinutes = existing?.sleepDurationMinutes ?: sleepDurationMinutes,
          sleepQuality = existing?.sleepQuality ?: sleepQuality,
        )
      }

      parsedEntries.add(
        MoodEntry(
          id = id,
          mood = mood,
          timestamp = timestamp,
          note = note,
          tags = tags,
          sleepDurationMinutes = sleepDurationMinutes,
          sleepQuality = sleepQuality,
          energyLevel = energyLevel,
          stressLevel = stressLevel,
          anxietyLevel = anxietyLevel,
          physicalActivity = physicalActivity,
          createdAt = timestamp,
          updatedAt = System.currentTimeMillis(),
        ),
      )
    }

    return ImportValidationResult.Valid(
      entries = parsedEntries,
      dailyContexts = dailyContextMap.values.toList(),
    )
  }

  fun parseAndValidateCsv(rawCsv: String): ImportValidationResult {
    val cleaned = rawCsv.removePrefix("\uFEFF").trim()
    if (cleaned.isEmpty()) {
      return ImportValidationResult.Invalid("The selected CSV file is empty.")
    }

    val rows = parseCsvRows(cleaned)
    if (rows.size < 2) {
      return ImportValidationResult.Invalid("CSV file must contain a header row and at least one entry row.")
    }

    val header = rows.first().map { it.trim().lowercase() }
    val tsIndex = header.indexOf("timestamp")
    val moodIndex = header.indexOf("mood")
    if (tsIndex == -1 || moodIndex == -1) {
      return ImportValidationResult.Invalid("Invalid MOODY CSV: missing required 'timestamp' or 'mood' column.")
    }

    val tagsIndex = header.indexOf("tags")
    val noteIndex = header.indexOf("note")
    val sleepDurIndex = header.indexOf("sleep_duration_minutes")
    val sleepQualIndex = header.indexOf("sleep_quality")
    val energyIndex = header.indexOf("energy_level")
    val stressIndex = header.indexOf("stress_level")
    val anxietyIndex = header.indexOf("anxiety_level")
    val activityIndex = header.indexOf("physical_activity")

    val parsedEntries = mutableListOf<MoodEntry>()
    val dailyContextMap = linkedMapOf<String, DailyContext>()

    for (rowIdx in 1 until rows.size) {
      val row = rows[rowIdx]
      if (row.all { it.isBlank() }) continue

      val tsRaw = row.getOrNull(tsIndex)?.trim().orEmpty()
      val timestamp = tsRaw.toLongOrNull()?.takeIf { it > 0L }
        ?: DateTimeUtils.parseIsoUtcOrNull(tsRaw)
        ?: return ImportValidationResult.Invalid("CSV row ${rowIdx + 1} has an invalid timestamp '$tsRaw'.")

      val moodRaw = row.getOrNull(moodIndex)?.trim().orEmpty()
      val mood = moodRaw.toIntOrNull()
        ?: return ImportValidationResult.Invalid("CSV row ${rowIdx + 1} has an invalid mood '$moodRaw'.")
      if (mood !in 1..5) {
        return ImportValidationResult.Invalid("CSV row ${rowIdx + 1} has out-of-range mood $mood (must be 1..5).")
      }

      val tagsRaw = tagsIndex.takeIf { it >= 0 }?.let { row.getOrNull(it) }.orEmpty()
      val tags = parseTagsField(tagsRaw)
      val note = noteIndex.takeIf { it >= 0 }?.let { row.getOrNull(it) }.orEmpty()

      val sleepDurationMinutes = sleepDurIndex.takeIf { it >= 0 }
        ?.let { row.getOrNull(it)?.trim()?.toIntOrNull() }
        ?.takeIf { it in 0..(24 * 60) }
      val sleepQuality = sleepQualIndex.takeIf { it >= 0 }
        ?.let { row.getOrNull(it)?.trim()?.toIntOrNull() }
        ?.takeIf { it in 1..5 }
      val energyLevel = energyIndex.takeIf { it >= 0 }
        ?.let { row.getOrNull(it)?.trim()?.toIntOrNull() }
        ?.takeIf { it in 1..5 }
      val stressLevel = stressIndex.takeIf { it >= 0 }
        ?.let { row.getOrNull(it)?.trim()?.toIntOrNull() }
        ?.takeIf { it in 1..5 }
      val anxietyLevel = anxietyIndex.takeIf { it >= 0 }
        ?.let { row.getOrNull(it)?.trim()?.toIntOrNull() }
        ?.takeIf { it in 1..5 }
      val physicalActivity = activityIndex.takeIf { it >= 0 }
        ?.let { PhysicalActivityLevel.fromNameOrNull(row.getOrNull(it)) }

      if (sleepDurationMinutes != null || sleepQuality != null) {
        val dayKey = DateTimeUtils.formatFileDate(timestamp)
        val existing = dailyContextMap[dayKey]
        dailyContextMap[dayKey] = DailyContext(
          dayKey = dayKey,
          sleepDurationMinutes = existing?.sleepDurationMinutes ?: sleepDurationMinutes,
          sleepQuality = existing?.sleepQuality ?: sleepQuality,
        )
      }

      parsedEntries.add(
        MoodEntry(
          id = UUID.randomUUID().toString(),
          mood = mood,
          timestamp = timestamp,
          note = note,
          tags = tags,
          sleepDurationMinutes = sleepDurationMinutes,
          sleepQuality = sleepQuality,
          energyLevel = energyLevel,
          stressLevel = stressLevel,
          anxietyLevel = anxietyLevel,
          physicalActivity = physicalActivity,
          createdAt = timestamp,
          updatedAt = System.currentTimeMillis(),
        ),
      )
    }

    if (parsedEntries.isEmpty()) {
      return ImportValidationResult.Invalid("The CSV file contains 0 valid mood entries.")
    }

    return ImportValidationResult.Valid(
      entries = parsedEntries,
      dailyContexts = dailyContextMap.values.toList(),
    )
  }

  private fun parseCsvRows(input: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var currentRow = mutableListOf<String>()
    val currentCell = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < input.length) {
      val c = input[i]
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < input.length && input[i + 1] == '"') {
            currentCell.append('"')
            i++
          } else {
            inQuotes = false
          }
        } else {
          currentCell.append(c)
        }
      } else {
        when (c) {
          '"' -> inQuotes = true
          ',' -> {
            currentRow.add(currentCell.toString())
            currentCell.clear()
          }
          '\r' -> {
            if (i + 1 < input.length && input[i + 1] == '\n') i++
            currentRow.add(currentCell.toString())
            currentCell.clear()
            rows.add(currentRow)
            currentRow = mutableListOf()
          }
          '\n' -> {
            currentRow.add(currentCell.toString())
            currentCell.clear()
            rows.add(currentRow)
            currentRow = mutableListOf()
          }
          else -> currentCell.append(c)
        }
      }
      i++
    }
    if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
      currentRow.add(currentCell.toString())
      rows.add(currentRow)
    }
    return rows
  }

  private fun extractOptionalInt(obj: JSONObject, key: String): Int? {
    if (!obj.has(key) || obj.isNull(key)) return null
    return when (val raw = obj.opt(key)) {
      is Int -> raw
      is Number -> raw.toInt()
      is String -> raw.trim().toIntOrNull()
      else -> null
    }
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
