package com.unfold.core.data.repositoryimpl

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.unfold.core.domain.model.TimelineItem
import com.unfold.core.domain.model.TimelineItemType
import com.unfold.core.domain.repository.TimelineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import javax.inject.Inject

class TimelineRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TimelineRepository {

    override fun getTodayTimeline(): Flow<List<TimelineItem>> = flow {
        val items = mutableListOf<TimelineItem>()
        
        try {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfTomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION
            )

            val selection = "${CalendarContract.Instances.VISIBLE} = 1"
            val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, startOfDay)
            android.content.ContentUris.appendId(builder, endOfTomorrow)

            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val dtStartIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val dtEndIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val locationIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex).toString()
                    val title = it.getString(titleIndex) ?: "Untitled Event"
                    val startTime = it.getLong(dtStartIndex)
                    val endTime = if (!it.isNull(dtEndIndex)) it.getLong(dtEndIndex) else null
                    val isAllDay = it.getInt(allDayIndex) == 1
                    val location = it.getString(locationIndex)

                    items.add(
                        TimelineItem(
                            id = id,
                            title = title,
                            startTimeMillis = startTime,
                            endTimeMillis = endTime,
                            type = TimelineItemType.EVENT,
                            isAllDay = isAllDay,
                            location = location
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted, return empty or mock data
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emit(items)
    }.flowOn(Dispatchers.IO)
    
    override fun getTimeline(startTimeMillis: Long, endTimeMillis: Long): Flow<List<TimelineItem>> = flow {
        val items = mutableListOf<TimelineItem>()
        
        try {
            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION
            )

            val selection = "${CalendarContract.Instances.VISIBLE} = 1"
            val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, startTimeMillis)
            android.content.ContentUris.appendId(builder, endTimeMillis)

            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val dtStartIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val dtEndIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val locationIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex).toString()
                    val title = it.getString(titleIndex) ?: "Untitled Event"
                    val startTime = it.getLong(dtStartIndex)
                    val endTime = if (!it.isNull(dtEndIndex)) it.getLong(dtEndIndex) else null
                    val isAllDay = it.getInt(allDayIndex) == 1
                    val location = it.getString(locationIndex)

                    items.add(
                        TimelineItem(
                            id = id,
                            title = title,
                            startTimeMillis = startTime,
                            endTimeMillis = endTime,
                            type = TimelineItemType.EVENT,
                            isAllDay = isAllDay,
                            location = location
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emit(items)
    }.flowOn(Dispatchers.IO)
}
