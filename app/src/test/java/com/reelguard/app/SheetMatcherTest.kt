package com.reelguard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetMatcherTest {

    @Test
    fun `comment titles`() {
        assertTrue(SheetMatcher.isCommentsTitle("Comments"))
        assertTrue(SheetMatcher.isCommentsTitle("comment"))
        assertTrue(SheetMatcher.isCommentsTitle("Comments (12)"))
        assertTrue(SheetMatcher.isCommentsTitle("Comments · 1.2K"))
        assertTrue(SheetMatcher.isCommentsTitle("1,204 comments"))
        assertTrue(SheetMatcher.isCommentsTitle("12 comments"))
    }

    @Test
    fun `non-title labels`() {
        assertFalse(SheetMatcher.isCommentsTitle("Read the comments"))
        assertFalse(SheetMatcher.isCommentsTitle("Comment section"))
        assertFalse(SheetMatcher.isCommentsTitle("Add a comment"))
        assertFalse(SheetMatcher.isCommentsTitle(""))
    }

    @Test
    fun `composer labels`() {
        assertTrue(SheetMatcher.isCommentsComposer("Add a comment…"))
        assertTrue(SheetMatcher.isCommentsComposer("ADD A COMMENT"))
        assertTrue(SheetMatcher.isCommentsComposer("View all comments"))
        assertTrue(SheetMatcher.isCommentsComposer("View all 12 comments"))
        assertTrue(SheetMatcher.isCommentsComposer("Be the first to comment"))
    }

    @Test
    fun `non-composer labels`() {
        assertFalse(SheetMatcher.isCommentsComposer("Comments"))
        assertFalse(SheetMatcher.isCommentsComposer("Add yours"))
        assertFalse(SheetMatcher.isCommentsComposer(""))
    }

    @Test
    fun `sheet requires title and composer`() {
        assertTrue(
            SheetMatcher.hasCommentsSheet(
                listOf("Comments", "Add a comment…", "Nice reel"),
            ),
        )
        assertFalse(
            SheetMatcher.hasCommentsSheet(
                listOf("Add a comment…", "Nice reel"),
            ),
        )
        assertFalse(
            SheetMatcher.hasCommentsSheet(
                listOf("Comments", "Nice reel"),
            ),
        )
    }
}
