package com.unfold.core.data.repositoryimpl

import com.unfold.core.data.local.dao.GestureDao
import com.unfold.core.data.local.entity.GestureEntity
import com.unfold.core.domain.model.GestureBinding
import com.unfold.core.domain.model.GestureType
import com.unfold.core.domain.repository.GestureRepository
import com.unfold.core.domain.navigation.UnfoldRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureRepositoryImpl @Inject constructor(
    private val gestureDao: GestureDao
) : GestureRepository {

    override fun observeBindings(): Flow<List<GestureBinding>> {
        return gestureDao.observeAllBindings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBinding(gestureType: GestureType): GestureBinding? {
        return gestureDao.getBinding(gestureType.name)?.toDomain()
    }

    override suspend fun setBinding(binding: GestureBinding) {
        gestureDao.insertBinding(GestureEntity.fromDomain(binding))
    }

    override suspend fun resetToDefaults() {
        gestureDao.clearAll()
        val defaultGestures = listOf(
            GestureEntity("SWIPE_LEFT_1F", "OPEN_INTENT", targetIntentUri = "tel:"),
            GestureEntity("SWIPE_RIGHT_1F", "LAUNCH_APP", targetPackage = "com.whatsapp"),
            GestureEntity("SWIPE_LEFT_2F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.HiddenSpace.route),
            GestureEntity("DOCK_SWIPE_HOLD", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.HiddenSpace.route),
            GestureEntity("SWIPE_RIGHT_2F", "OPEN_INTENT", targetIntentUri = "market://details?id="),
            GestureEntity("SWIPE_DOWN_1F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.UniversalSearch.route),
            GestureEntity("EDGE_SWIPE", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.Home.route)
        )
        gestureDao.insertAll(defaultGestures)
    }
}


