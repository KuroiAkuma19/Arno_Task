package com.example.mynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mynote.data.Note
import com.example.mynote.repository.NoteRepository
// ADDED: This import is required to use .launch{} with coroutines
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository): ViewModel() {

    val allNotes: LiveData<List<Note>> = repository.allNotes

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    suspend fun insertAndReturnId(note: Note): Long {
        return repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }

    fun searchNotes(query: String): LiveData<List<Note>> {
        return if(query.isEmpty()){
            allNotes
        }else{
            repository.searchNotes(query)
        }
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }


}