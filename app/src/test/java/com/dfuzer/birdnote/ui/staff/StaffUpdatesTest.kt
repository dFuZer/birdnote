package com.dfuzer.birdnote.ui.staff

import com.dfuzer.birdnote.domain.ClefMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffUpdatesTest {
    private val config = StaffRenderConfig(ClefMode.SOL, 1, 3, 1500, 0.05f)

    @Test
    fun mailboxKeepsEveryRapidAdvanceAndSuppressesOnlyDuplicatePublications() {
        val mailbox = StaffMailbox<Int>()
        val queues = listOf(listOf(1, 2, 3), listOf(2, 3, 4), listOf(3, 4, 5))
        queues.forEach { notes ->
            mailbox.submit(StaffUpdate(notes, config))
            mailbox.submit(StaffUpdate(notes.toList(), config))
        }
        val updates = mutableListOf<StaffUpdate<Int>>()
        mailbox.drainInto(updates)
        assertEquals(queues, updates.map { it.notes })

        val belt = StaffBelt<Int>()
        updates.forEach { belt.offer(it.notes, it.config.visibleCount, 0.0, 0.0) }
        assertEquals(listOf(1, 2, 3, 4, 5), belt.notes)
        assertEquals(2, belt.highlightIndex)
        belt.advance(20.0, 1.5f, 0.05f)
        assertEquals(2f, belt.shift, 0f)

        mailbox.drainInto(updates)
        assertTrue(updates.isEmpty())
        assertEquals(queues.last(), mailbox.latest?.notes)
    }

    @Test
    fun configurationTravelsWithItsNotesAndLatestStateSurvivesRendererRecreation() {
        val mailbox = StaffMailbox<Int>()
        val first = StaffUpdate(listOf(1, 2, 3), config)
        val changed = first.copy(config = config.copy(clefMode = ClefMode.FA, difficulty = 4))
        mailbox.submit(first)
        mailbox.submit(changed)
        val updates = mutableListOf<StaffUpdate<Int>>()
        mailbox.drainInto(updates)
        assertEquals(listOf(first, changed), updates)
        assertEquals(changed, mailbox.latest)
        mailbox.submit(changed)
        mailbox.drainInto(updates)
        assertTrue(updates.isEmpty())

        val freshBelt = StaffBelt<Int>()
        val restored = checkNotNull(mailbox.latest)
        freshBelt.offer(restored.notes, restored.config.visibleCount, 0.0, 0.0)
        assertEquals(restored.notes, freshBelt.notes)
    }

    @Test
    fun resetAndPruningKeepTheCurrentQuestionAndVisibleSuffix() {
        val belt = StaffBelt<Int>()
        belt.offer(listOf(1, 2, 3), 3, 0.0, 0.0)
        belt.advance(20.0, 1.5f, 0.05f)
        belt.offer(listOf(2, 3, 4), 3, 20.0, 0.0)
        belt.advance(40.0, 1.5f, 0.05f)
        belt.offer(listOf(3, 4, 5), 3, 40.0, 0.0)
        assertEquals(listOf(2, 3, 4, 5), belt.notes)
        assertEquals(1, belt.origin)
        assertEquals(3, belt.notes[belt.highlightIndex])
        belt.offer(listOf(100, 101, 102), 3, 40.0, 0.0)
        assertEquals(0, belt.origin)
        assertEquals(0, belt.highlightIndex)
        assertEquals(-2f, belt.shift, 0f)
        belt.offer(emptyList(), 3, 40.0, 0.0)
        assertTrue(belt.notes.isEmpty())
        assertEquals(0f, belt.shift, 0f)
    }
}
