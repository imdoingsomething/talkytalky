package com.talkytalky.app

import com.talkytalky.app.RecordingStateMachine.Action
import com.talkytalky.app.RecordingStateMachine.State
import org.junit.Assert.assertEquals
import org.junit.Test

class StateMachineTest {
    @Test fun tapStartStopThenSuccess() {
        val m = RecordingStateMachine()
        assertEquals(Action.START, m.onTap())
        assertEquals(Action.STOP_AND_SEND, m.onTap())
        assertEquals(Action.NONE, m.onTap()) // ignored while sending
        m.onSuccess(); assertEquals(State.IDLE, m.state)
    }
    @Test fun timeoutActsLikeSecondTap() {
        val m = RecordingStateMachine(); m.onTap()
        assertEquals(Action.STOP_AND_SEND, m.onTimeout())
        assertEquals(Action.NONE, m.onTimeout())
    }
    @Test fun failureOffersRetry() {
        val m = RecordingStateMachine(); m.onTap(); m.onTap(); m.onFailure()
        assertEquals(State.FAILED, m.state)
        assertEquals(Action.RETRY, m.onTap())
    }
}
