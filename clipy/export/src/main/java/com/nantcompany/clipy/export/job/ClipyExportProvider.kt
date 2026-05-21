package com.nantcompany.clipy.export.job

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.nantcompany.clipy.export.output.LocalOutputRepository

/**
 * Manual Dependency Injection provider for the export module.
 * Ensures singletons for critical managers and repositories.
 */
@UnstableApi
object ClipyExportProvider {
    private var jobManager: ProcessingJobManager? = null
    private var outputRepository: LocalOutputRepository? = null

    fun getOutputRepository(): LocalOutputRepository {
        return outputRepository ?: synchronized(this) {
            outputRepository ?: LocalOutputRepository().also { outputRepository = it }
        }
    }

    fun getJobManager(context: Context): ProcessingJobManager {
        return jobManager ?: synchronized(this) {
            jobManager ?: ProcessingJobManager(
                context.applicationContext,
                getOutputRepository()
            ).also { jobManager = it }
        }
    }
}
