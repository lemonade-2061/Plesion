package jp.plesion.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationAccessTest {
    @Test
    fun fineGranted_serviceOn_isGranted() {
        assertEquals(LocationAccess.GRANTED, locationAccessOf(fine = true, coarse = true, serviceEnabled = true))
    }

    @Test
    fun coarseOnly_isNotEnough() {
        assertEquals(LocationAccess.COARSE_ONLY, locationAccessOf(fine = false, coarse = true, serviceEnabled = true))
    }

    @Test
    fun nothingGranted_isDenied() {
        assertEquals(LocationAccess.DENIED, locationAccessOf(fine = false, coarse = false, serviceEnabled = true))
    }

    @Test
    fun fineGranted_serviceOff_isServiceOff() {
        assertEquals(LocationAccess.SERVICE_OFF, locationAccessOf(fine = true, coarse = true, serviceEnabled = false))
    }

    @Test
    fun denied_takesPriorityOverServiceOff() {
        assertEquals(LocationAccess.DENIED, locationAccessOf(fine = false, coarse = false, serviceEnabled = false))
    }
}
