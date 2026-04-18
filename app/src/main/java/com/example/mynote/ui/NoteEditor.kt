package com.example.mynote.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.mynote.R
import com.example.mynote.data.Note
import com.example.mynote.data.NoteDatabase
import com.example.mynote.databinding.ActivityNoteEditorBinding
import com.example.mynote.repository.NoteRepository
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class NoteEditor : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var viewModel: NoteViewModel

    private var currentNote: Note? = null
    private var selectedReminderAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        applyWindowInsets()
        setupViewModel()

        currentNote = intent.getParcelableExtra("NOTE")
        if(currentNote !=null){
            binding.etTitle.setText(currentNote!!.title)
            binding.etContent.setText(currentNote!!.content)
            binding.etReminderPhone.setText(currentNote!!.reminderPhone.orEmpty())
            selectedReminderAt = currentNote!!.reminderAt
            updateReminderText()
        }

        binding.btnPickReminder.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootEditor) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, imeInsets.bottom)
            val actionBarSize = resolveActionBarSize()

            binding.toolbar.setPadding(
                binding.toolbar.paddingLeft,
                systemBars.top,
                binding.toolbar.paddingRight,
                binding.toolbar.paddingBottom
            )
            binding.toolbar.layoutParams = binding.toolbar.layoutParams.apply {
                height = actionBarSize + systemBars.top
            }
            binding.rootEditor.setPadding(
                binding.rootEditor.paddingLeft,
                binding.rootEditor.paddingTop,
                binding.rootEditor.paddingRight,
                bottomInset
            )
            insets
        }
    }

    private fun resolveActionBarSize(): Int {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typedValue, true)) {
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } else {
            resources.getDimensionPixelSize(com.google.android.material.R.dimen.m3_appbar_size_medium)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupViewModel() {
        val dao = NoteDatabase.getDatabase(this).noteDao()
        val repo = NoteRepository(dao)
        val factory = NoteViewModelFactory(repo)

        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_editor, menu)
        menu.findItem(R.id.action_delete).isVisible = currentNote !=null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_delete -> {
                deleteNote()
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    private fun deleteNote() {
        if(currentNote !=null){
            viewModel.delete(currentNote!!)

        }
        finish()

    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()
        val reminderPhone = binding.etReminderPhone.text?.toString()?.trim().orEmpty()
        val currentTime = System.currentTimeMillis()
        val locationText = getCurrentLocationText()
        val reminderAt = selectedReminderAt

        if (title.isEmpty()){
            binding.etTitle.error = "Title is required"
            return
        }

        if (content.isEmpty()){
            binding.etContent.error = "Content is required"
            return
        }

        if (reminderPhone.isNotBlank() && !isValidInternationalPhone(reminderPhone)) {
            binding.etReminderPhone.error = "Use format like +91 9976541234"
            return
        }

        lifecycleScope.launch {
            if (currentNote == null){
                val note = Note(
                    id = 0,
                    title = title,
                    content = content,
                    timestamp = currentTime,
                    isCompleted = false,
                    location = locationText,
                    reminderAt = reminderAt,
                    reminderPhone = reminderPhone.ifBlank { null }
                )
                val noteId = viewModel.insertAndReturnId(note).toInt()
                scheduleReminder(noteId, title, reminderAt, reminderPhone)
            } else {
                val updatedNote = currentNote!!.copy(
                    title = title,
                    content = content,
                    timestamp = currentTime,
                    updatedAt = currentTime,
                    location = locationText,
                    reminderAt = reminderAt,
                    reminderPhone = reminderPhone.ifBlank { null }
                )
                viewModel.update(updatedNote)
                scheduleReminder(updatedNote.id, title, reminderAt, reminderPhone)
            }

            finish()
        }
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()
        val base = Calendar.getInstance().apply {
            timeInMillis = selectedReminderAt ?: System.currentTimeMillis()
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val selected = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (selected.before(now)) {
                            binding.tvReminderValue.text = "Reminder must be in the future"
                            selectedReminderAt = null
                        } else {
                            selectedReminderAt = selected.timeInMillis
                            updateReminderText()
                        }
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(this)
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateReminderText() {
        val value = selectedReminderAt
        if (value == null) {
            binding.tvReminderValue.text = "No reminder set"
            return
        }
        val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(value)
        binding.tvReminderValue.text = formatted
    }

    private fun getCurrentLocationText(): String {
        return try {
            val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) return "Location permission missing"

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: return "Location unavailable"

            if (!Geocoder.isPresent()) {
                return "Location unavailable"
            }

            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()
            formatAddress(address)
        } catch (_: SecurityException) {
            "Location permission missing"
        } catch (_: IOException) {
            "Location unavailable"
        } catch (_: IllegalArgumentException) {
            "Location unavailable"
        } catch (_: Exception) {
            "Location unavailable"
        }
    }

    private fun formatAddress(address: Address?): String {
        if (address == null) return "Location unavailable"

        val parts = listOfNotNull(
            address.subLocality?.takeIf { it.isNotBlank() },
            address.locality?.takeIf { it.isNotBlank() },
            address.adminArea?.takeIf { it.isNotBlank() },
            address.countryName?.takeIf { it.isNotBlank() }
        ).distinct()

        if (parts.isNotEmpty()) return parts.joinToString(", ")
        return address.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: "Location unavailable"
    }

    private fun scheduleReminder(noteId: Int, title: String, reminderAt: Long?, phone: String) {
        cancelReminder(noteId)
        if (reminderAt == null) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderSmsReceiver::class.java).apply {
            putExtra(ReminderSmsReceiver.EXTRA_MESSAGE, "Reminder: $title")
            putExtra(ReminderSmsReceiver.EXTRA_PHONE, phone)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            noteId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
            Toast.makeText(this, "Reminder saved (may be a little delayed)", Toast.LENGTH_SHORT).show()
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
            Toast.makeText(this, "Reminder set successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelReminder(noteId: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderSmsReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            noteId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun isValidInternationalPhone(rawPhone: String): Boolean {
        val normalized = rawPhone.replace("\\s+".toRegex(), "")
        return Pattern.compile("^\\+[1-9]\\d{7,14}$").matcher(normalized).matches()
    }
}