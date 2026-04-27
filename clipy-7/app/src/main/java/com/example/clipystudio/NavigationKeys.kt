package com.example.clipystudio

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Intro : NavKey
@Serializable data object Language : NavKey
@Serializable data object Dashboard : NavKey
@Serializable data object Import : NavKey
@Serializable data object Editor : NavKey
@Serializable data object Export : NavKey
@Serializable data object Settings : NavKey
