package com.whereami.locus.data.sync

import com.whereami.locus.data.CompanyLinkStore
import com.whereami.locus.domain.TrackRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocationUploader(
    private val linkStore: CompanyLinkStore,
    private val trackRepository: TrackRepository
) {
    private val mutex = Mutex()

    suspend fun flushPending() {
        mutex.withLock {
            val link = linkStore.current() ?: return
            while (true) {
                val batch = trackRepository.getUnuploadedPoints(BATCH_SIZE)
                if (batch.isEmpty()) return
                when (val result = LocusApiClient.uploadPositions(link.serverUrl, link.token, batch)) {
                    is UploadResult.Success -> {
                        trackRepository.markPointsUploaded(batch.map { it.id })
                        if (batch.size < BATCH_SIZE) return
                    }
                    UploadResult.Unauthorized -> {
                        linkStore.clear()
                        return
                    }
                    is UploadResult.Failed -> return
                }
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
    }
}
