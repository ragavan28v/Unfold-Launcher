package com.ragavan.unfold.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.ragavan.unfold.data.apps.AppRepository
import com.ragavan.unfold.data.layout.HomeLayoutRepository
import com.ragavan.unfold.data.dock.DockRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.ragavan.unfold.viewmodel.state.HomeState

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val installedApps =
        repository.getInstalledApps()

    private val layoutRepository =
        HomeLayoutRepository(application)

    private val homeLayout =
        layoutRepository.createDefaultLayout(
            installedApps
        )

    private val pinnedApps =
        installedApps.filter { app ->

            homeLayout.items.any { homeItem ->

                homeItem.packageName == app.packageName

            }

        }

    private val _state =
        MutableStateFlow(

            HomeState(

                layout = homeLayout,

                pinnedApps = pinnedApps,

                dockLayout = DockRepository(application)
                    .createDefaultDock(installedApps),

                installedApps = installedApps

            )

        )

    val state: StateFlow<HomeState> =
        _state.asStateFlow()

}