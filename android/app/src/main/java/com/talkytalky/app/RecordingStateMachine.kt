package com.talkytalky.app

/** Pure logic (unit-tested): IDLE -tap-> RECORDING -tap|timeout-> SENDING -> IDLE | FAILED -tap-> retry. */
class RecordingStateMachine {
    enum class State { IDLE, RECORDING, SENDING, FAILED }
    enum class Action { START, STOP_AND_SEND, RETRY, NONE }

    var state = State.IDLE
        private set

    fun onTap(): Action = when (state) {
        State.IDLE -> { state = State.RECORDING; Action.START }
        State.RECORDING -> { state = State.SENDING; Action.STOP_AND_SEND }
        State.FAILED -> { state = State.SENDING; Action.RETRY }
        State.SENDING -> Action.NONE
    }

    fun onTimeout(): Action = if (state == State.RECORDING) onTap() else Action.NONE
    fun onSuccess() { state = State.IDLE }
    fun onFailure() { state = State.FAILED }
    fun dismissFailure() { if (state == State.FAILED) state = State.IDLE }
    fun onRecorderError() { state = State.IDLE }

    companion object { const val MAX_RECORDING_MS = 90_000L }
}
