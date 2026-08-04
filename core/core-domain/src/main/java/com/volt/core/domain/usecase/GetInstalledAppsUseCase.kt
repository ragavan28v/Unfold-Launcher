package com.volt.core.domain.usecase

import com.volt.core.domain.model.AppInfo
import com.volt.core.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val repo: AppRepository
) {
    operator fun invoke(includeHidden: Boolean = false): Flow<List<AppInfo>> {
        return repo.observeApps(includeHidden)
    }
}
