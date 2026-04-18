package com.example.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.data.Note
import com.example.mynote.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Locale

// 1. Updated constructor to take two functions: onClick and onTaskStatusChanged
class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onTaskStatusChanged: (Note) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val noteList = ArrayList<Note>()
    private val selectedIds = mutableSetOf<Int>()
    private var lastAnimatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]

        holder.binding.apply {
            // Note: Make sure these IDs (tvTitle, tvContent, tvDate) match your item_note.xml exactly
            tvTitle.text = note.title
            tvContent.text = note.content

            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val locationLabel = note.location ?: "Unknown location"
            tvDate.text = "Updated: ${sdf.format(note.updatedAt)} | $locationLabel"

            // 2. Handle the CheckBox (The "Task is Done" part)
            // Clear listener first to prevent recursive loops during scroll
            checkboxDone.setOnCheckedChangeListener(null)
            checkboxDone.isChecked = note.isCompleted

            checkboxDone.setOnCheckedChangeListener { _, isChecked ->
                note.isCompleted = isChecked
                onTaskStatusChanged(note)
            }

            root.setOnClickListener {
                if (selectedIds.isNotEmpty()) {
                    toggleSelection(note.id)
                } else {
                    onNoteClick(note)
                }
            }

            root.setOnLongClickListener {
                toggleSelection(note.id)
                true
            }

            root.alpha = if (selectedIds.contains(note.id)) 0.6f else 1.0f

            if (position > lastAnimatedPosition) {
                root.translationY = 18f
                root.alpha = 0f
                root.animate()
                    .translationY(0f)
                    .alpha(if (selectedIds.contains(note.id)) 0.6f else 1.0f)
                    .setDuration(220)
                    .start()
                lastAnimatedPosition = position
            }
        }
    }

    override fun getItemCount() = noteList.size

    inner class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    fun setNotes(newNotes: List<Note>) {
        val oldSize = noteList.size
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = noteList.size
            override fun getNewListSize() = newNotes.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
                noteList[oldPos].id == newNotes[newPos].id

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
                noteList[oldPos] == newNotes[newPos]
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        noteList.clear()
        noteList.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
        selectedIds.retainAll(noteList.map { it.id }.toSet())
        onSelectionChanged(selectedIds.size)
        if (newNotes.size < oldSize) {
            lastAnimatedPosition = -1
        }
    }

    fun getSelectedNotes(): List<Note> = noteList.filter { selectedIds.contains(it.id) }
    fun getCurrentNotes(): List<Note> = noteList.toList()

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    private fun toggleSelection(noteId: Int) {
        if (selectedIds.contains(noteId)) {
            selectedIds.remove(noteId)
        } else {
            selectedIds.add(noteId)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }
}