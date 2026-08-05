package com.volt.core.data.repositoryimpl

import com.volt.core.data.local.dao.GestureDao
import com.volt.core.data.local.entity.GestureEntity
import com.volt.core.domain.model.GestureBinding
import com.volt.core.domain.model.GestureType
import com.volt.core.domain.repository.GestureRepository
import com.volt.core.domain.navigation.VoltRoute
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
            GestureEntity("SWIPE_LEFT_2F", "OPEN_SCREEN", targetScreenRoute = "hidden_space"),
            GestureEntity("SWIPE_RIGHT_2F", "OPEN_INTENT", targetIntentUri = "market://details?id="),
            GestureEntity("SWIPE_DOWN_1F", "OPEN_SCREEN", targetScreenRoute = VoltRoute.UniversalSearch.route)
        )
        gestureDao.insertAll(defaultGestures)
    }
}
