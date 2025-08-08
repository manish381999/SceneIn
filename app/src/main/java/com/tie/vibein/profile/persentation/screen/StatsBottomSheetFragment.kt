package com.tie.vibein.profile.persentation.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tie.vibein.databinding.BottomSheetProfileStatsBinding
import com.tie.vibein.profile.data.models.StatItem
import com.tie.vibein.profile.persentation.adapter.StatsAdapter

class StatsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetProfileStatsBinding? = null
    private val binding get() = _binding!!
    private val statsAdapter = StatsAdapter()

    // These will be passed from UserProfileActivity
    private var sheetTitle: String? = null
    private var statsList: ArrayList<StatItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sheetTitle = it.getString(ARG_TITLE)
            statsList = it.getParcelableArrayList(ARG_STATS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetProfileStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSheetTitle.text = sheetTitle

        binding.rvStats.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = statsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        statsAdapter.submitList(statsList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_STATS = "arg_stats"

        // Use this to create a new instance of the fragment
        fun newInstance(title: String, stats: List<StatItem>): StatsBottomSheetFragment {
            return StatsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putParcelableArrayList(ARG_STATS, ArrayList(stats))
                }
            }
        }
    }
}