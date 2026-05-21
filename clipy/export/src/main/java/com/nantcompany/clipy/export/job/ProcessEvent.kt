package com.nantcompany.clipy.export.job

import com.nantcompany.clipy.export.output.OutputMedia

sealed class ProcessEvent {
    data class Started(val command: String) : ProcessEvent()
    data class ProgressUpdate(val percent: Int, val timeMs: Long, val status: String) : ProcessEvent()
    data class Completed(val output: OutputMedia) : ProcessEvent()
    data object Cancelled : ProcessEvent()
    data class Failed(val error: Throwable) : ProcessEvent()
}
