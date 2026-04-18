package com.example.mynote.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mynote.R
import com.example.mynote.data.Note
import com.example.mynote.data.NoteDatabase
import com.example.mynote.databinding.ActivityMainBinding
import com.example.mynote.repository.NoteRepository
import com.example.mynote.ui.adapter.NoteAdapter
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private val PERMISSION_REQUEST_CODE = 123
    private var selectedCount = 0
    private var currentNotesSource: LiveData<List<Note>>? = null
    private val notesObserver = Observer<List<Note>> { notes ->
        adapter.setNotes(notes)
        updateProgress(notes)
        binding.rvNotes.post { binding.rvNotes.requestLayout() }
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Theme & UI Setup
        updateStatusBarColor()
        applyWindowInsets()
        setupViewModel()
        setupAdapter()
        setupRecyclerView()
        setupSwipeRefresh()
        observeNotesForQuery("")
        setupSearch()
        requestAppPermissions()

        // --- BOTTOM NAVIGATION ---
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_categories -> {
                    showThemeChooserDialog()
                    true
                }
                R.id.nav_trash -> {
                    deleteSelectedNotes()
                    true
                }
                else -> false
            }
        }

        // FAB Logic
        binding.fabAddNote.setOnClickListener {
            startActivity(Intent(this, NoteEditor::class.java))
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMain) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerBar.setPadding(
                binding.headerBar.paddingLeft,
                systemBars.top + 20,
                binding.headerBar.paddingRight,
                binding.headerBar.paddingBottom
            )
            binding.bottomNavigationView.setPadding(
                binding.bottomNavigationView.paddingLeft,
                binding.bottomNavigationView.paddingTop,
                binding.bottomNavigationView.paddingRight,
                systemBars.bottom
            )
            insets
        }
    }

    private fun setupAdapter() {
        adapter = NoteAdapter(
            onNoteClick = { note ->
                val intent = Intent(this, NoteEditor::class.java)
                intent.putExtra("NOTE", note)
                startActivity(intent)
            },
            onTaskStatusChanged = { note ->
                viewModel.update(note)
            },
            onSelectionChanged = { count ->
                selectedCount = count
                if (count > 0) {
                    binding.progressText.text = "$count selected"
                } else {
                    updateProgress(adapter.getCurrentNotes())
                }
            }
        )
    }

    // --- PROGRESS LOGIC ---
    private fun updateProgress(notes: List<Note>) {
        if (selectedCount > 0) {
            binding.progressText.text = "$selectedCount selected"
            return
        }
        val totalTasks = notes.size
        val completedTasks = notes.count { it.isCompleted }

        // 1. Change 'tvProgressCount' to 'progressText'
        binding.progressText.text = "Tasks: $completedTasks/$totalTasks Done"

        // 2. Change 'taskProgressBar' to 'taskProgressBar' (matching your XML underscore style)
        if (totalTasks > 0) {
            val progressPercent = (completedTasks * 100) / totalTasks
            binding.taskProgressBar.progress = progressPercent
            binding.progressPercentText.text = "$progressPercent%"
        } else {
            binding.taskProgressBar.progress = 0
            binding.progressPercentText.text = "0%"
        }
    }

    private fun observeNotesForQuery(query: String) {
        currentNotesSource?.removeObservers(this)
        currentNotesSource = viewModel.searchNotes(query)
        currentNotesSource?.observe(this, notesObserver)
    }

    private fun deleteSelectedNotes() {
        val selectedNotes = adapter.getSelectedNotes()
        if (selectedNotes.isEmpty()) {
            Toast.makeText(this, "Long press tasks to select, then tap Trash", Toast.LENGTH_SHORT).show()
            return
        }
        selectedNotes.forEach { viewModel.delete(it) }
        adapter.clearSelection()
        Toast.makeText(this, "Deleted ${selectedNotes.size} selected tasks", Toast.LENGTH_SHORT).show()
    }

    // --- THEME & UI FUNCTIONS ---
    private fun showThemeChooserDialog() {
        val themes = arrayOf("Light Mode", "Dark Mode", "System Default")
        AlertDialog.Builder(this)
            .setTitle("Select App Theme")
            .setItems(themes) { _, which ->
                when (which) {
                    0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            .show()
    }

    private fun updateStatusBarColor() {
        val isDarkMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        window.statusBarColor = if (isDarkMode) {
            ContextCompat.getColor(this, R.color.dark_bg)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }
    }

    // --- SETUP & PERMISSIONS ---
    private fun setupViewModel() {
        val dao = NoteDatabase.getDatabase(this).noteDao()
        val repo = NoteRepository(dao)
        val factory = NoteViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]
    }

    private fun setupRecyclerView() {
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter
        binding.rvNotes.setHasFixedSize(false)
        binding.rvNotes.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 180
            removeDuration = 140
            moveDuration = 160
            changeDuration = 120
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            observeNotesForQuery(binding.etSearch.text?.toString().orEmpty())
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            observeNotesForQuery(text.toString())
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            getGPSLocation()
        }
    }

    private fun getGPSLocation() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                val placeName = if (Geocoder.isPresent()) {
                    try {
                        Geocoder(this, Locale.getDefault())
                            .getFromLocation(location.latitude, location.longitude, 1)
                            ?.firstOrNull()
                            ?.let { address ->
                                listOfNotNull(
                                    address.subLocality?.takeIf { it.isNotBlank() },
                                    address.locality?.takeIf { it.isNotBlank() },
                                    address.adminArea?.takeIf { it.isNotBlank() },
                                    address.countryName?.takeIf { it.isNotBlank() }
                                ).distinct().joinToString(", ")
                            }
                            ?.ifBlank { "Location unavailable" }
                            ?: "Location unavailable"
                    } catch (_: Exception) {
                        "Location unavailable"
                    }
                } else {
                    "Location unavailable"
                }

                Toast.makeText(this, "Location: $placeName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }
}