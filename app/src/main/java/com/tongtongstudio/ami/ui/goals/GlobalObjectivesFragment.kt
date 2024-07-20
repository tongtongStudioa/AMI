package com.tongtongstudio.ami.ui.goals

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.GoalsAdapter
import com.tongtongstudio.ami.adapter.GoalsListener
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.notification.SoundPlayer
import com.tongtongstudio.ami.ui.ADD_GOAL_RESULT_OK
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GlobalObjectivesFragment : Fragment(), GoalsListener {

    companion object {
        fun newInstance() = GlobalObjectivesFragment()
    }

    private val viewModel: GlobalObjectivesViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var goalsAdapter: GoalsAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        //view model, sound player and adapter
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        goalsAdapter = GoalsAdapter(this)
        soundPlayer = SoundPlayer(requireContext())

        // implement UI
        binding.apply {
            fabAddTask.setOnClickListener {
                viewModel.addGlobalGoal()
            }

            mainRecyclerView.apply {
                adapter = goalsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }

            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val goal = goalsAdapter.getGoalsList()[viewHolder.absoluteAdapterPosition]
                    if (direction == ItemTouchHelper.RIGHT) {
                        // delete task
                        goalsAdapter.notifyItemRemoved(viewHolder.absoluteAdapterPosition)
                        viewModel.deleteGoal(goal)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // edit task
                        goalsAdapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
                        viewModel.updateGoal(goal)
                    }
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    RecyclerViewSwipeDecorator.Builder(
                        context,
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                        .addSwipeLeftActionIcon(R.drawable.ic_baseline_edit_24)
                        .addSwipeRightActionIcon(R.drawable.ic_baseline_delete_24)
                        .setSwipeLeftActionIconTint(resources.getColor(R.color.md_theme_light_tertiary))
                        .setSwipeRightActionIconTint(resources.getColor(R.color.md_theme_light_error))
                        .create()
                        .decorate()
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }

            }).attachToRecyclerView(mainRecyclerView)
        }

        // listen to request's result
        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            val msg = if (result == ADD_GOAL_RESULT_OK)
                getString(R.string.goal_added)
            else getString(R.string.goal_updated)
            Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
        }

        // adapt data in recycler view
        viewModel.globalGoals.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getText(R.string.no_objectives_yet)
                binding.emptyRecyclerView.textViewActionText.text = ""
                binding.toolbar.collapseActionView()
            } else {
                goalsAdapter.submitList(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
            }
        }

        // respond to event
        // TODO: change this with the good life cycle state : if not all event respond to that collect method
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.goalsEvents.collect { event ->
                    when (event) {
                        is GlobalObjectivesViewModel.GoalsEvent.NavigateToAddGlobalGoalScreen -> {
                            val title = getString(R.string.fragment_title_add_global_goal)
                            val action =
                                GlobalObjectivesFragmentDirections.actionGlobalObjectivesFragmentToEditGoalFragment(
                                    title
                                )
                            findNavController().navigate(action)
                        }
                        is GlobalObjectivesViewModel.GoalsEvent.NavigateToEditGlobalGoalScreen -> {
                            val title = getString(R.string.fragment_title_edit_global_goal)
                            val action =
                                GlobalObjectivesFragmentDirections.actionGlobalObjectivesFragmentToEditGoalFragment(
                                    title,
                                    event.goal
                                )
                            findNavController().navigate(action)
                        }
                        is GlobalObjectivesViewModel.GoalsEvent.NavigateToDetailsGlobalGoalScreen -> {
                            // TODO: do something cool
                        }
                        is GlobalObjectivesViewModel.GoalsEvent.ShowUndoDeleteGlobalGoalMessage -> {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.msg_gloabl_goal_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(getText(R.string.msg_action_undo)) {
                                    viewModel.onUndoDeleteClick(event.goal)
                                }.show()
                        }
                        else -> {}// do nothing
                    }
                }
            }
        }
    }

    override fun onGoalClick(goal: Assessment) {
        viewModel.onGoalClick(goal)
    }

    override fun onGoalRightSwipe(goal: Assessment) {
        viewModel.deleteGoal(goal)
    }

    override fun onGoalLeftSwipe(goal: Assessment) {
        viewModel.updateGoal(goal)
    }

}