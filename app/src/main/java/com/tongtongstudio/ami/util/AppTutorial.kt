package com.tongtongstudio.ami.util

import android.app.Activity
import android.content.SharedPreferences
import android.view.View
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

/**
 * This class manage all the sub methods to display tutorial steps and guidance through the app.
 */
class AppTutorial(val activity: Activity, private val sharedPreferences: SharedPreferences) {

    /**
     * Manage tutorial sequences.
     */
    fun startTutorialSequence(steps: List<TutorialStep>) {
        if (steps.isEmpty()) return
        executeStep(steps, 0)
    }

    /**
     * Execute a specific tutorial step.
     */
    private fun executeStep(steps: List<TutorialStep>, currentIndex: Int) {
        if (currentIndex >= steps.size) return // Fin de la séquence

        val step = steps[currentIndex]
        val isStepCompleted = sharedPreferences.getBoolean(step.key, false)

        if (!isStepCompleted) {
            MaterialTapTargetPrompt.Builder(activity)
                .setTarget(step.target)
                .setPrimaryText(step.primaryText)
                .setSecondaryText(step.secondaryText)
                .setPromptStateChangeListener { _, state ->
                    when (state) {
                        MaterialTapTargetPrompt.STATE_DISMISSED -> {
                            // Mark step as finish state
                            sharedPreferences.edit().putBoolean(step.key, true).apply()

                            // Go to next step
                            executeStep(steps, currentIndex + 1)
                        }

                        MaterialTapTargetPrompt.STATE_FOCAL_PRESSED -> {
                            // Mark step as finish state
                            sharedPreferences.edit().putBoolean(step.key, true).apply()
                        }
                    }
                }
                .show()
        } else {
            // If this step is already complete, go to the next one
            executeStep(steps, currentIndex + 1)
        }
    }

    /**
     * Reset all preferences key.
     */
    fun resetTutorial(keys: List<String>) {
        val editor = sharedPreferences.edit()
        keys.forEach { key ->
            editor.putBoolean(key, false)
        }
        editor.apply()
    }

    /**
     * Reset state specific tutorial step.
     */
    fun resetStep(key: String) {
        sharedPreferences.edit().putBoolean(key, false).apply()
    }

    /**
     * Check if a step is completed.
     */
    fun isStepCompleted(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }
}

/**
 * Data class for Tutorial Step.
 */
data class TutorialStep(
    val key: String, // Clé unique pour cette étape
    val target: View, // Vue cible
    val primaryText: String, // Texte principal
    val secondaryText: String // Texte secondaire
)