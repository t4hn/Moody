package com.example.data

import com.example.model.MoodEntry
import java.util.Calendar
import java.util.UUID

object DemoDataSeeder {

  private data class SeedSpec(
    val daysAgo: Int,
    val hour: Int,
    val minute: Int,
    val mood: Int,
    val tags: List<String>,
    val note: String,
  )

  fun createSeedEntries(nowMillis: Long = System.currentTimeMillis()): List<MoodEntry> {
    val specs = listOf(
      SeedSpec(0, 8, 25, 4, listOf("coffee", "calm"), "Slow morning light and quiet playlist before opening emails."),
      SeedSpec(1, 21, 40, 4, listOf("grateful", "calm"), "Long evening walk cooled my head after a busy week."),
      SeedSpec(1, 13, 15, 3, listOf("focused", "tired"), "Deep work sprint; energy dipped right after lunch."),
      SeedSpec(2, 19, 50, 5, listOf("social", "energized"), "Dinner with old friends—laughing felt effortless."),
      SeedSpec(2, 9, 10, 3, listOf("coffee", "focused"), "Steady start, chipping away at the backlog."),
      SeedSpec(3, 22, 15, 2, listOf("anxious", "tired"), "Doomscrolled too late and felt restless."),
      SeedSpec(3, 15, 30, 3, listOf("rainy", "calm"), "Rain against the window made the afternoon feel slower."),
      SeedSpec(4, 18, 5, 4, listOf("walk", "energized"), "Caught the sunset glow on the way back home."),
      SeedSpec(5, 11, 20, 5, listOf("focused", "grateful"), "Everything clicked in the studio today."),
      SeedSpec(6, 20, 45, 2, listOf("tired", "overwhelmed"), "Too many tabs open mentally. Need an early night."),
      SeedSpec(7, 14, 10, 4, listOf("social", "coffee"), "Caught up over espresso at the corner cafe."),
      SeedSpec(9, 8, 45, 1, listOf("anxious", "tired"), "Woke up heavy after barely sleeping."),
      SeedSpec(9, 19, 30, 3, listOf("walk", "calm"), "Forced myself outside for twenty minutes—helped more than expected."),
      SeedSpec(11, 17, 0, 4, listOf("focused", "calm"), "Wrapped a tricky problem cleanly."),
      SeedSpec(13, 21, 10, 5, listOf("social", "grateful"), "Late night music session and good conversation."),
      SeedSpec(15, 10, 30, 2, listOf("rainy", "tired"), "Grey skies all morning, sluggish start."),
      SeedSpec(17, 16, 40, 4, listOf("energized", "walk"), "Crisp air and clear headspace."),
      SeedSpec(19, 22, 0, 3, listOf("calm"), "Winding down with tea and a book."),
    )

    val cal = Calendar.getInstance()
    return specs.mapIndexed { index, spec ->
      cal.timeInMillis = nowMillis
      cal.add(Calendar.DAY_OF_YEAR, -spec.daysAgo)
      cal.set(Calendar.HOUR_OF_DAY, spec.hour)
      cal.set(Calendar.MINUTE, spec.minute)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      val ts = minOf(cal.timeInMillis, nowMillis - (index + 1) * 60_000L)
      MoodEntry(
        id = UUID.nameUUIDFromBytes("moody-demo-$index".toByteArray()).toString(),
        mood = spec.mood,
        timestamp = ts,
        note = spec.note,
        tags = spec.tags,
        isDemo = true,
        createdAt = ts,
        updatedAt = ts,
      )
    }
  }
}
