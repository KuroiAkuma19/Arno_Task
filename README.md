ArnoTask - Task Manager App

📌 Overview:

ArnoTask is a centralized task management application designed to help users efficiently organize daily tasks, notes, and reminders within a single interface. Developed using modern Android technologies like Kotlin, MVVM architecture, and Room Database, the app addresses the fragmentation of personal productivity tools. Key features include real-time location detection, personalized theme customization (dark/light modes), and an automated SMS alert system for timely task reminders.


🚀 Features:

	•	 Comprehensive Task Management: High-performance interface for creating, editing, searching, and deleting notes.  
	•	 Location Detection: Automatically retrieves geographical context to provide location stamps for tasks.  
	•	 Automated SMS Reminders: Integrated broadcast receiver to send SMS alerts to a designated phone number.  
	•	 System Notifications: High-priority alerts to ensure important tasks are never missed.  
	•	 Personalized Themes: Toggle between Dark and Light modes for a customized experience.  
	•	 Progress Tracking: Visual progress bar and percentage indicator showing task completion status.  
	
🛠️ Technical Stack:

	•	 Language: Kotlin   
	•	 Architecture: MVVM (Model-View-ViewModel)   
	•	 Database: Room Database with migration support.  
	•	 UI Components: ViewBinding, RecyclerView, and SwipeRefreshLayout.  
	
📁 Permissions Required:

	•	 ACCESS_FINE_LOCATION: For precise location context.  
	•	 SEND_SMS: To facilitate the automated reminder system.  
	•	 POST_NOTIFICATIONS: To display task alerts.  
	•	 SCHEDULE_EXACT_ALARM: Required for time-sensitive reminders.  

📦 APK & Installation:

How to Install
Via Android Studio :
	1	Connect your physical Android device via USB or start an Android Emulator.
	2	Ensure USB Debugging is enabled in your device's "Developer Options."
	3	Click the green Run button (Play icon) in Android Studio. This builds the APK and installs it automatically.
