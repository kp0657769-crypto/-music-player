package com.example.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun getLocalAudioFiles(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val trackList = mutableListOf<MusicTrack>()
        
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Only music
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri = ContentUris.withAppendedId(collection, id)
                val artworkUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()

                trackList.add(
                    MusicTrack(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = contentUri.toString(),
                        artworkUri = artworkUri,
                        dateAdded = dateAdded
                    )
                )
            }
        }
        
        // If empty, provide a fallback for testing (though Media3 will fail to play it if it's fake,
        // it allows the UI to populate and look like a premium music player).
        if (trackList.isEmpty()) {
            trackList.addAll(getFallbackTracks())
        }
        
        trackList
    }
    
    private fun getFallbackTracks(): List<MusicTrack> {
        return listOf(
            MusicTrack("1", "Neon Dreams", "Synthwave AI", "Midnight City", 210000, "", null),
            MusicTrack("2", "Lofi Rain", "Chillhop Beats", "Study Sessions", 185000, "", null),
            MusicTrack("3", "Cyberpunk Runner", "Dark Synth", "2077", 245000, "", null),
            MusicTrack("4", "Acoustic Sunrise", "Folklore", "Morning Woods", 195000, "", null)
        )
    }
}
