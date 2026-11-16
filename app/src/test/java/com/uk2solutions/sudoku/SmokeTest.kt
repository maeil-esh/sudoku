package com.uk2solutions.sudoku

import com.uk2solutions.sudoku.ui.Tab
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test fun threeTabs() = assertEquals(3, Tab.entries.size)
}
