package com.tongtongstudio.ami.util

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.tongtongstudio.ami.R

/**
 * Navigate using an action ID with custom animations.
 *
 * @param directions The NavDirections to navigate to.
 * @param enterAnim Animation resource ID for the enter animation.
 * @param exitAnim Animation resource ID for the exit animation.
 * @param popEnterAnim Animation resource ID for the pop enter animation.
 * @param popExitAnim Animation resource ID for the pop exit animation.
 */
fun NavController.navigateWithAnim(
    directions: NavDirections,
    enterAnim: Int = R.anim.slide_in_right,
    exitAnim: Int = R.anim.slide_out_left,
    popEnterAnim: Int = R.anim.slide_in_right,
    popExitAnim: Int = R.anim.slide_out_left
) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(enterAnim)
        .setExitAnim(exitAnim)
        .setPopEnterAnim(popEnterAnim)
        .setPopExitAnim(popExitAnim)
        .build()

    this.navigate(directions, navOptions)
}