package com.mathapelo.keyboard

import org.junit.Assert.*
import org.junit.Test

class PredictionEngineTest {
    @Test fun predictsLikelyWords() {
        assertTrue(PredictionEngine.predictions("hel").contains("hello"))
        assertTrue(PredictionEngine.predictions("mar", listOf("marvelous")).contains("marvelous"))
    }

    @Test fun respectsCase() {
        assertEquals("Hello", PredictionEngine.matchCase("hello", "Hel"))
        assertEquals("HELLO", PredictionEngine.matchCase("hello", "HEL"))
    }

    @Test fun correctionIsConservative() {
        assertEquals("Hello", PredictionEngine.correction("Helo"))
        assertNull(PredictionEngine.correction("hello"))
        assertNull(PredictionEngine.correction("1234"))
        assertNull(PredictionEngine.correction("ab"))
    }

    @Test fun threeSuggestionSlotsAlwaysFilled() {
        assertEquals(3, PredictionEngine.threeSuggestions("").size)
        assertEquals(3, PredictionEngine.threeSuggestions("hel").size)
        assertEquals(3, PredictionEngine.threeSuggestions("xzzqu").size)
        assertTrue(PredictionEngine.threeSuggestions("hel").contains("hello"))
    }

    @Test fun personalizedWordsNotOverridden() {
        assertNull(PredictionEngine.correction("Mathapelo", listOf("Mathapelo")))
        assertTrue(PredictionEngine.predictions("Zyx", listOf("zyxophone")).contains("Zyxophone"))
    }
}
