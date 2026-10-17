package com.mathapelo.keyboard

import java.util.Locale

/**
 * Lightweight on-device predictions. No network calls, telemetry or permissions.
 * Designed so the matching and corrections are easy to unit-test.
 */
object PredictionEngine {
    private val common = ("the:800 to:760 and:740 a:720 I:700 you:680 is:650 in:630 it:620 " +
        "that:600 for:580 on:550 with:520 of:510 this:490 be:480 are:470 was:450 have:440 " +
        "not:430 but:420 we:410 they:400 my:395 your:390 me:380 do:370 can:360 at:350 " +
        "as:340 if:330 so:320 what:310 when:300 where:295 how:290 why:285 who:280 " +
        "hello:500 hey:480 hi:470 thanks:460 thank:450 please:440 good:430 morning:420 " +
        "night:415 today:410 tomorrow:405 later:400 okay:490 yes:470 no:465 love:455 " +
        "home:445 work:435 school:425 time:415 there:405 here:395 about:385 just:375 " +
        "really:365 want:355 need:345 know:335 think:325 going:315 coming:305 " +
        "see:295 say:285 said:275 one:265 two:255 three:245 very:235 much:225 " +
        "more:215 some:205 any:195 also:185 from:175 been:165 will:155 would:145 " +
        "could:135 should:125 because:115 before:105 after:95 then:85 than:75 " +
        "something:90 someone:80 people:70 friend:60 family:50 beautiful:45 " +
        "message:42 phone:40 picture:38 screenshot:36 keyboard:34 Mathapelo:32 " +
        "ke:95 le:90 go:85 ka:80 ya:75 ba:70 re:65 o:60 aowa:55 dumela:50 " +
        "sharp:45 eish:40 wena:35 nna:30").split(" ").mapNotNull { entry ->
        val index = entry.lastIndexOf(':')
        entry.takeIf { index > 0 }?.let {
            entry.substring(0, index).lowercase(Locale.ROOT) to entry.substring(index + 1).toInt()
        }
    }.toMap()

    private fun normalized(word: String) = word.trim().lowercase(Locale.ROOT)
    private fun dictionary(personal: Collection<String>, learned: Collection<String>): Map<String, Int> {
        val result = common.toMutableMap()
        personal.map(::normalized).filter { it.matches(Regex("[\\p{L}'-]{2,32}")) }
            .forEach { result[it] = 1800 }
        learned.map(::normalized).filter { it.matches(Regex("[\\p{L}'-]{2,32}")) }
            .forEach { result[it] = (result[it] ?: 0) + 700 }
        return result
    }

    fun predictions(prefix: String, personal: Collection<String> = emptyList(),
                    learned: Collection<String> = emptyList(), max: Int = 3): List<String> {
        val query = normalized(prefix)
        if (query.length < 1 || query.length > 32) return emptyList()
        return dictionary(personal, learned).entries.asSequence()
            .filter { it.key.startsWith(query) && it.key != query }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(max.coerceIn(0, 5))
            .map { matchCase(it.key, prefix) }
            .toList()
    }

    /** Conservative correction: only a *unique* known word at edit distance 1. */
    fun correction(word: String, personal: Collection<String> = emptyList(),
                   learned: Collection<String> = emptyList()): String? {
        val query = normalized(word)
        if (query.length !in 4..20 || !query.all { it.isLetter() }) return null
        val words = dictionary(personal, learned)
        if (words.containsKey(query)) return null
        val matches = words.entries.filter { (w, score) ->
            score >= 120 && kotlin.math.abs(w.length - query.length) <= 1 && distanceAtMostOne(query, w)
        }.sortedByDescending { it.value }
        // Avoid a dubious correction if candidates are similarly likely.
        if (matches.isEmpty() || matches.size > 1 && matches[0].value - matches[1].value < 150) return null
        return matchCase(matches[0].key, word)
    }

    fun matchCase(candidate: String, source: String): String = when {
        source.isNotEmpty() && source.all { it.isUpperCase() } -> candidate.uppercase(Locale.ROOT)
        source.firstOrNull()?.isUpperCase() == true ->
            candidate.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> candidate
    }

    private fun distanceAtMostOne(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                b.length > a.length -> j++
                else -> { i++; j++ }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }
}
