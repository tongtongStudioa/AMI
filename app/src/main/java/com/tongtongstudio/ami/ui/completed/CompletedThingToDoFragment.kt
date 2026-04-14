package com.tongtongstudio.ami.ui.completed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.thingToDo.InteractionListener
import com.tongtongstudio.ami.adapter.thingToDo.ThingToDoAdapter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CompletedThingToDoFragment : Fragment(R.layout.fragment_main),
    InteractionListener {

    private val viewModel: CompletedThingToDoViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private val sharedViewModel: MainViewModel by viewModels()

    private lateinit var completedAdapter: ThingToDoAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainViewModel.SharedEvent.ShowUndoDeleteTaskMessage -> {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.msg_thing_to_do_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(getString(R.string.msg_action_undo)) {
                                    sharedViewModel.onUndoDeleteClick(event.thingToDo)
                                }.show()
                        }

                        is MainViewModel.SharedEvent.NavigateToTaskDetailsScreen -> {
                            val action =
                                CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToDetailsFragment(
                                    event.task.id
                                )
                            val extras =
                                FragmentNavigatorExtras(event.sharedView to event.sharedView.transitionName)
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            findNavController().navigate(action, extras)
                        }

                        is MainViewModel.SharedEvent.NavigateToProjectDetailsScreen -> {
                            val action =
                                CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToLocalProjectStatsFragment2(
                                    event.project.taskRelations.mainTask.id
                                )
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToAddScreenWithParentTask -> {
                            val action = CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToAddEditTaskFragment(
                                title = getString(R.string.fragment_title_add_thing_to_do),
                                thingToDo = null,
                                parentTask = event.parentTask
                            )
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            findNavController().navigate(action)
                        }

                        else -> {
                            //do nothing
                        }
                    }
                }
            }
        }

        completedAdapter = ThingToDoAdapter(this)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMainBinding.inflate(inflater)
        // to make toolbar appear
        setUpToolbar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddTask.isVisible = false

        binding.apply {
            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = completedAdapter
            }

            val callback = object : ThingToDoItemCallback<ThingToDoAdapter>(
                completedAdapter,
                ItemTouchHelper.RIGHT,
                requireContext()
            ) {
                override fun actionOnRightSwiped(thingToDo: ThingToDo, position: Int) {
                    sharedViewModel.deleteTask(thingToDo, requireContext())
                }

            }
            ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)
        }

        viewModel.thingsToDoCompleted.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getString(R.string.text_explication_no_tasks_completed)
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_tasks_completed)
                binding.textSup.text = getString(R.string.nb_job_completed_info, 0)
            } else {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
                completedAdapter.submitList(it)
                binding.textSup.text = getString(R.string.nb_job_completed_info, it.size)
            }
        }

        viewModel.filteredTasks.observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                completedAdapter.submitList(it)
        }


        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.completed_tasks_menu, menu)
                setupSearch(menu.findItem(R.id.action_search).actionView as SearchView)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return true
            }

        }, viewLifecycleOwner)
    }

    private fun setupSearch(searchView: SearchView) {
        searchView.queryHint = "Rechercher une tâche..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterTasks(newText.orEmpty())
                return true
            }
        })

        searchView.findViewById<ImageView>(R.id.search_close_btn).setOnClickListener {
            searchView.isIconified = true
            searchView.clearFocus()
        }
    }

    // function to set up toolbar with collapse toolbar and link to drawer layout
    private fun setUpToolbar() {
        val mainActivity = activity as MainActivity
        // imperative to see option menu and navigation icon (hamburger)
        mainActivity.setSupportActionBar(binding.toolbar)

        val navController = findNavController()
        // retrieve app bar configuration : see MainActivity.class
        val appBarConfiguration = mainActivity.appBarConfiguration

        // to set hamburger menu work and open drawer layout
        binding.collapseToolbar.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )
        binding.toolbar.setNavigationOnClickListener {
            exitTransition = MaterialFadeThrough().apply {
                duration = resources.getInteger(R.integer.middle_duration).toLong()
            }
            navController.navigateUp(appBarConfiguration)
        }
        binding.toolbar.subtitle = getString(R.string.completed_tasks_subtitle)
    }

    override fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
    }

    override fun onProjectClick(thingToDo: ThingToDo, itemView: View, position: Int) {
        sharedViewModel.navigateToProjectDetailsScreen(thingToDo,itemView
        )
    }

    override fun onTaskClick(thingToDo: Task, itemView: View, position: Int) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(thingToDo: ThingToDo) {
        sharedViewModel.addSubThingTodo(thingToDo.taskRelations.mainTask)
    }

}