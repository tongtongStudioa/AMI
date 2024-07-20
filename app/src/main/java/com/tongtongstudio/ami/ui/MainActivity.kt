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


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    lateinit var drawerLayout: DrawerLayout
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var toolbar: Toolbar

    val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
                R.id.habitsFragment,
                R.id.completedThingToDoFragment,
                R.id.projectFragment,
                R.id.globalObjectivesFragment
            ), drawerLayout
        )

        // change status color bar
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = this.window

            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // finally change the color's status bar
            window.statusBarColor = ContextCompat.getColor(this, R.color.md_theme_light_surface)
        }*/

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
                else -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }
        viewModel.lookForMissedRecurringTasks()

        intent?.let {
            if (intent.hasExtra(ASSESSMENT_ID)) {
                showCompleteAssessmentDialog(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Check if app has been open with assessment notification
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
        // TODO: create rate dialog fragment
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

    /*fun onAddEditResult(result: Int, stringsAdded: Array<String>, stringsUpdated: Array<String>) {
        when (result) {
            ADD_TASK_RESULT_OK ->
        }
    }*/
}

const val ADD_TASK_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_TASK_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val EDIT_GOAL_RESULT_OK = Activity.RESULT_FIRST_USER + 2
const val ADD_GOAL_RESULT_OK = Activity.RESULT_FIRST_USER + 3

