package com.tongtongstudio.ami.util

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.KEY_APP_BAR
import com.tongtongstudio.ami.ui.KEY_DIALOG_SHOWN
import com.tongtongstudio.ami.ui.KEY_TUTORIAL_FAB_ADD_TASK
import com.tongtongstudio.ami.ui.todaytasks.TodayTasksFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.STATE_BACK_BUTTON_PRESSED
import androidx.core.content.edit

/**
 * This class manage all the sub methods to display tutorial steps and guidance through the app.
 */
class AppTutorial(val activity: FragmentActivity,
                  private val lifecycleOwner: LifecycleOwner,
                  private val sharedPreferences: SharedPreferences) {

    private val pendingSteps = mutableListOf<Fragment>()

    private val todayFragmentSteps = listOf(
        FragmentSteps(
            KEY_APP_BAR, R.id.app_bar,
            activity.getString(R.string.primary_text_tuto_step1_frag1),
            activity.getString(R.string.secondary_text_tuto_step1_frag1)
        ),
        FragmentSteps(
            KEY_TUTORIAL_FAB_ADD_TASK, R.id.fab_add_task,
            activity.getString(R.string.primary_text_tuto_step2_frag1),
            activity.getString(R.string.secondary_text_tuto_step2_frag1)
        ),
    )

    fun startTutorialForFragment(fragment: Fragment) {
        when (fragment) {
            is TodayTasksFragment -> {
                startTutorialSequence(fragment,todayFragmentSteps)
            }
        }
    }

    /**
     * Manage tutorial sequences.
     */
    private fun startTutorialSequence(fragment: Fragment,steps: List<FragmentSteps>) {
        if (steps.isEmpty()) return
        executeStep(fragment,steps,0)
    }

    /**
     * Execute a specific fragment's tutorial step.
     */
    private fun executeStep(fragment: Fragment, steps: List<FragmentSteps>,currentIndex: Int) {
        if (currentIndex >= steps.size) return
        val step = steps[currentIndex]
        sharedPreferences.getBoolean(step.key, false)

        waitForViewInFragment(fragment, step.targetId) { targetId ->
            MaterialTapTargetPrompt.Builder(activity)
                .setTarget(targetId)
                .setPrimaryText(step.primaryText)
                .setSecondaryText(step.secondaryText)
                .setPromptStateChangeListener { _, state ->
                    when (state) {
                        STATE_BACK_BUTTON_PRESSED -> {
                            if (currentIndex <= 0) return@setPromptStateChangeListener
                            executeStep(fragment,steps, currentIndex - 1)
                        }
                        MaterialTapTargetPrompt.STATE_DISMISSED -> {
                            // Mark step as finish state
                            //sharedPreferences.edit().putBoolean(step.key, true).apply()

                            // Go to next step
                            executeStep(fragment,steps, currentIndex + 1)
                        }

                        MaterialTapTargetPrompt.STATE_FOCAL_PRESSED -> {
                            // Mark step as finish state
                            //sharedPreferences.edit().putBoolean(step.key, true).apply()
                        }
                    }
                }
                .show()
        }
    }

    fun maybeShowTutorialFor(fragName: String, fragment: Fragment) {
        if (!sharedPreferences.getBoolean("tutorial_shown_$fragName", false) && sharedPreferences.getBoolean("tutorial_enabled", false)) {
            sharedPreferences.edit { putBoolean("tutorial_shown_$fragName", true) }
            startTutorialForFragment(fragment)
        } else if (!sharedPreferences.getBoolean(KEY_DIALOG_SHOWN, false)) {
            // On retarde, en mémoire
            pendingSteps.add(fragment)
        }
    }

    fun handleWelcomeResult(accepted: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_DIALOG_SHOWN, true)
                .putBoolean("tutorial_enabled", accepted)
        }

        if (accepted) {
            playPendingSteps()
        } else {
            pendingSteps.clear()
        }
    }

    private fun playPendingSteps() {
        pendingSteps.forEach { fragment ->
            startTutorialForFragment(fragment)
        }
        pendingSteps.clear()
    }

    /**
     * Reset all preferences fragment's key.
     */
    fun resetTutorial(keys: List<String>) {
        sharedPreferences.edit {
            keys.forEach { key ->
                putBoolean(key, false)
            }
        }
    }

    /**
     * Reset state specific tutorial step.
     */
    fun resetStep(key: String) {
        sharedPreferences.edit { putBoolean(key, false) }
    }

    /**
     * Check if a step is completed.
     */
    fun isStepCompleted(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    private fun waitForViewInFragment(fragment: Fragment, viewId: Int, onViewReady: (View) -> Unit) {
        activity.lifecycleScope.launch {
            while (true) {
                val view = fragment.view?.findViewById<View>(viewId)
                if (view != null && view.isShown) {
                    delay(300)
                    onViewReady(view)
                    break
                }
                delay(100)
            }
        }
    }

}

/**
 * Data class for Tutorial Step.
 */
data class FragmentSteps(
    val key: String, // Clé unique pour cette étape
    val targetId: Int, // Vue cible
    val primaryText: String, // Texte principal
    val secondaryText: String // Texte secondaire
)

interface TutorialTrigger {
    fun triggerTutorialFor(fragment: Fragment)
}