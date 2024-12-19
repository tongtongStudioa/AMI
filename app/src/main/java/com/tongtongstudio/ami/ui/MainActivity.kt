package com.tongtongstudio.ami.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.tongtongstudio.ami.NavigationGraphDirections
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.receiver.ASSESSMENT_ID
import dagger.hilt.android.AndroidEntryPoint
import hotchemi.android.rate.AppRate


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var toolbar: Toolbar

    val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        AppRate.with(this)
            .setInstallDays(10) // default 10, 0 means install day.
            .setLaunchTimes(3) // default 10
            .setRemindInterval(10) // default 1
            .setShowLaterButton(true) // default true
            .monitor()

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navController = navHostFragment.findNavController()
        drawerLayout = findViewById(R.id.drawer_layout)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.todayTasksFragment,
                R.id.othersTasksFragment,
                R.id.achievementsFragment,
                R.id.draftsFragment,
                R.id.habitsFragment,
                R.id.completedThingToDoFragment,
                R.id.projectFragment,
                R.id.globalObjectivesFragment
            ), drawerLayout
        )
        navView.setupWithNavController(navController)

        // implementation for specific actions
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about_item -> {
                    aboutProject()
                    true
                }
                R.id.share_item -> {
                    shareContent()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.rate_item -> {
                    rateMe()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.support -> {
                    openBuyMeCoffeePage()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }

        // Check at the opening of the app
        viewModel.lookForMissedRecurringTasks()
        viewModel.updateTasksUrgency()
        intent?.let {
            if (intent.hasExtra(ASSESSMENT_ID)) {
                showCompleteAssessmentDialog(intent)
            }
        }
    }

    // Check if app has been open with assessment notification
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null) {
            if (intent.hasExtra(ASSESSMENT_ID)) {
                showCompleteAssessmentDialog(intent)
            }
        }
    }

    private fun showCompleteAssessmentDialog(intent: Intent) {
        val assessment = intent.getParcelableExtra<Assessment>(ASSESSMENT_ID)
        if (assessment != null) {
            val action =
                NavigationGraphDirections.actionGlobalCompleteAssessmentDialogFragment(
                    assessment
                )
            navController.navigate(action)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun shareContent() {
        val appLink = "https://play.google.com/store/apps/details?id=$packageName"
        val shareMessage = getString(R.string.share_message, appLink)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
    }

    private fun aboutProject() {
        val gitHubUrl = "https://github.com/tongtongStudioa/AMI"

        val webIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(gitHubUrl)
        }
        startActivity(webIntent)
    }

    private fun rateMe() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun openBuyMeCoffeePage() {
        val url = "https://www.buymeacoffee.com/tongtongStudioa"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    showPermissionRationale()
                }
                return
            }
        }
    }*/

    /*fun onAddEditResult(result: Int, stringsAdded: Array<String>, stringsUpdated: Array<String>) {
        when (result) {
            ADD_TASK_RESULT_OK ->
        }
    }*/
}

const val PERMISSION_REQUEST_CODE = 101
const val ADD_TASK_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_TASK_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val EDIT_GOAL_RESULT_OK = Activity.RESULT_FIRST_USER + 2
const val ADD_GOAL_RESULT_OK = Activity.RESULT_FIRST_USER + 3
const val ADD_DRAFT_TASK_OK = Activity.RESULT_FIRST_USER + 4

// Define each preference key for each tutorial step
const val PREF_TUTORIAL = "tutorial_preferences"
const val KEY_DIALOG_SHOWN = "welcome_dialog_shown"
const val KEY_APP_BAR = "tutorial_app_bar"
const val KEY_TUTORIAL_FAB_ADD_TASK = "tutorial_fab_add_task"
const val KEY_TUTORIAL_CLICK_TASK_DETAILS = "tutorial_click_task_details"
const val KEY_TUTORIAL_SWIPE_MODIFY_DELETE_TASK = "tutorial_swipe_modify_delete_task"
const val KEY_TUTORIAL_MENU_SORT_OPTIONS = "tutorial_menu_sort_options"
const val KEY_TUTORIAL_DRAFT_TASKS_ICON = "tutorial_draft_tasks_icon"
const val KEY_TUTORIAL_DUE_DATE = "tutorial_due_date"
const val KEY_TUTORIAL_PRIORITY = "tutorial_priority"
const val KEY_TUTORIAL_ESTIMATED_WORK_TIME = "tutorial_estimated_work_time"
const val KEY_TUTORIAL_DEPENDENCY_PERSON = "tutorial_dependency_person"
const val KEY_TUTORIAL_SKILL_LEVEL = "tutorial_skill_level"
const val KEY_TUTORIAL_STATS_COMPLETION_RATE = "tutorial_stats_completion_rate"
const val KEY_TUTORIAL_STATS_TIME_ESTIMATION_ACCURACY = "tutorial_stats_time_estimation_accuracy"
