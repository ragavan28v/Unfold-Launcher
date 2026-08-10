package com.unfold.core.domain.usecase

import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val repo: AppRepository
) {
    operator fun invoke(includeHidden: Boolean = false): Flow<List<AppInfo>> {
        return repo.observeApps(includeHidden)
    }
}

