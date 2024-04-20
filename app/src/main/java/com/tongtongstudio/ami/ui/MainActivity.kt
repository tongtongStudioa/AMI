package com.tongtongstudio.ami.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.tongtongstudio.ami.R
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
                R.id.completedThingToDoFragment,
                R.id.projectFragment
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

        viewModel.lookForMissedRecurringTasks()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

const val ADD_TASK_RESULT_OK = Activity.RESULT_FIRST_USER
const val ADD_PROJECT_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val ADD_EVENT_RESULT_OK = Activity.RESULT_FIRST_USER + 2
const val EDIT_TASK_RESULT_OK = Activity.RESULT_FIRST_USER + 3
const val EDIT_PROJECT_RESULT_OK = Activity.RESULT_FIRST_USER + 4
const val EDIT_EVENT_RESULT_OK = Activity.RESULT_FIRST_USER + 5

