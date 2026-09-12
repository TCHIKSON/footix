package com.footix.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footix.tv.core.AppError
import com.footix.tv.core.DataState
import com.footix.tv.core.Logger
import com.footix.tv.core.ServiceLocator
import com.footix.tv.data.repository.MatchRepository
import com.footix.tv.data.repository.StreamRepository
import com.footix.tv.domain.model.CompetitionSection
import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.PlayableStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeEvent {
    data class Resolving(val inProgress: Boolean) : HomeEvent
    data class Play(val match: Match, val stream: PlayableStream) : HomeEvent
    data class Failed(val error: AppError, val detail: String) : HomeEvent
}

class HomeViewModel(
    private val matchRepository: MatchRepository,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DataState<List<CompetitionSection>>>(DataState.Loading)
    val state: StateFlow<DataState<List<CompetitionSection>>> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var resolveJob: Job? = null

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (_state.value !is DataState.Success) _state.value = DataState.Loading
            try {
                _state.value = DataState.Success(matchRepository.sections(forceRefresh))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("Chargement du catalogue impossible", e)
                _state.value = DataState.Failure(AppError.from(e), AppError.detailOf(e))
            }
        }
    }

    /** Resout la source du match puis demande l'ouverture du lecteur. */
    fun open(match: Match) {
        if (resolveJob?.isActive == true) return
        resolveJob = viewModelScope.launch {
            _events.emit(HomeEvent.Resolving(true))
            val resolved = try {
                Result.success(streamRepository.resolve(match))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("Resolution du flux impossible pour ${match.slug}", e)
                Result.failure(e)
            }
            _events.emit(HomeEvent.Resolving(false))
            resolved
                .onSuccess { _events.emit(HomeEvent.Play(match, it)) }
                .onFailure { _events.emit(HomeEvent.Failed(AppError.from(it), AppError.detailOf(it))) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(ServiceLocator.matchRepository, ServiceLocator.streamRepository)
            }
        }
    }
}
