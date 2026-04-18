package com.example.mynote.repository

import androidx.lifecycle.LiveData
import com.example.mynote.data.Note
import com.example.mynote.data.NoteDao

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note): Long = noteDao.insert(note)

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }


    fun searchNotes(query: String): LiveData<List<Note>> {
        // Corrected string template to use the variable value
        return noteDao.searchNotes("%$query%")
    }
}

