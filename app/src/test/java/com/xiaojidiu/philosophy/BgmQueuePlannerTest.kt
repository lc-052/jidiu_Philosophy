package com.xiaojidiu.philosophy
import com.xiaojidiu.philosophy.domain.usecase.BgmQueuePlanner
import org.junit.Assert.*
import org.junit.Test
class BgmQueuePlannerTest {
    @Test fun emptyAndInvalidCurrentHaveNoNext() {assertNull(BgmQueuePlanner.next(0,0,"list",true));assertNull(BgmQueuePlanner.next(3,-1,"list",true))}
    @Test fun listWrapsAtEnd() {assertEquals(0,BgmQueuePlanner.next(3,2,"list",true))}
    @Test fun sequentialStopsOnlyAtEnd() {assertEquals(1,BgmQueuePlanner.next(3,0,"sequential",true));assertNull(BgmQueuePlanner.next(3,2,"sequential",true))}
    @Test fun singleStaysOnCurrent() {assertEquals(1,BgmQueuePlanner.next(3,1,"single",true))}
    @Test fun manualNextCanLeaveSingleOrSequential() {assertEquals(2,BgmQueuePlanner.next(3,1,"single",false));assertEquals(0,BgmQueuePlanner.next(3,2,"sequential",false))}
    @Test fun oneTrackListLoopsAndSequentialStops() {assertEquals(0,BgmQueuePlanner.next(1,0,"list",true));assertNull(BgmQueuePlanner.next(1,0,"sequential",true))}
}
