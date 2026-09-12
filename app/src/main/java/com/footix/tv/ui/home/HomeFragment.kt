package com.footix.tv.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.footix.tv.R
import com.footix.tv.core.DataState
import com.footix.tv.domain.model.CompetitionSection
import com.footix.tv.domain.model.Match
import com.footix.tv.ui.common.messageRes
import com.footix.tv.ui.player.PlayerActivity
import kotlinx.coroutines.launch

/**
 * Ecran d'accueil facon catalogue TV : une rangee par competition, une carte
 * par match.
 */
class HomeFragment : BrowseSupportFragment() {

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    private val cardPresenters = ClassPresenterSelector()
        .addClassPresenter(Match::class.java, MatchCardPresenter())
        .addClassPresenter(MessageCard::class.java, MessageCardPresenter())

    private val rowsAdapter = ArrayObjectAdapter(
        ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM)
    )

    private var renderedSections: List<CompetitionSection>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBrowse()
        adapter = rowsAdapter
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ -> onCardClicked(item) }
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Le depot decide s'il faut vraiment rappeler le reseau (TTL).
        viewModel.load()
    }

    private fun setupBrowse() {
        val context = requireContext()
        title = getString(R.string.app_name)
        badgeDrawable = ContextCompat.getDrawable(context, R.drawable.app_logo)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(context, R.color.brand_dark)
        progressBarManager.setInitialDelay(300L)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: DataState<List<CompetitionSection>>) {
        when (state) {
            is DataState.Loading -> progressBarManager.show()

            is DataState.Success -> {
                progressBarManager.hide()
                if (state.data.isEmpty()) {
                    showMessage(getString(R.string.empty_catalog))
                } else {
                    showSections(state.data)
                }
            }

            is DataState.Failure -> {
                progressBarManager.hide()
                showMessage(getString(state.error.messageRes()), state.detail)
            }
        }
    }

    private fun showSections(sections: List<CompetitionSection>) {
        // Evite de reconstruire les rangees (et de perdre le focus) sans changement.
        if (renderedSections == sections) return
        renderedSections = sections

        val previousRow = selectedPosition
        rowsAdapter.clear()
        sections.forEachIndexed { index, section ->
            val rowAdapter = ArrayObjectAdapter(cardPresenters)
            rowAdapter.addAll(0, section.matches)
            rowsAdapter.add(ListRow(HeaderItem(index.toLong(), section.title), rowAdapter))
        }
        if (previousRow in 0 until rowsAdapter.size()) {
            setSelectedPosition(previousRow, false)
        }
    }

    private fun showMessage(message: String, detail: String = "") {
        renderedSections = null
        rowsAdapter.clear()
        val rowAdapter = ArrayObjectAdapter(cardPresenters)
        rowAdapter.add(MessageCard(message, detail.ifBlank { getString(R.string.retry) }))
        rowsAdapter.add(ListRow(HeaderItem(0L, getString(R.string.app_name)), rowAdapter))
    }

    private fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Resolving ->
                if (event.inProgress) progressBarManager.show() else progressBarManager.hide()

            is HomeEvent.Play ->
                startActivity(PlayerActivity.intent(requireContext(), event.match, event.stream))

            is HomeEvent.Failed -> {
                val text = listOf(getString(event.error.messageRes()), event.detail)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onCardClicked(item: Any?) {
        when (item) {
            is Match -> viewModel.open(item)
            is MessageCard -> viewModel.load(forceRefresh = true)
        }
    }
}
