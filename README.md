🚨 OfflineSOS

### Offline Emergency Communication System

OfflineSOS is an Android-based emergency communication application designed to help people send SOS information when normal internet-based communication may be unavailable.

The application uses **GPS, SMS, Bluetooth/Wi-Fi based Nearby Connections, and phone-to-phone communication** to provide an additional communication channel during emergency situations.

---

## 🎯 Project Objective

During disasters such as floods, earthquakes, cyclones, or network outages, conventional internet communication may become unreliable.

OfflineSOS aims to provide a communication mechanism where nearby smartphones can discover and connect with each other and exchange emergency messages without requiring an internet connection for the nearby communication layer.

---

## ✨ Features

- 🚨 One-tap SOS emergency button
- 📍 GPS location detection
- 📡 Nearby phone-to-phone communication
- 📱 Nearby device discovery
- 🔗 Automatic connection between nearby OfflineSOS devices
- 📊 Connected phone counter
- 📩 Emergency SMS through cellular network
- 🔋 Battery level included in SOS message
- 🆔 Unique incident ID for SOS messages
- 🎯 Location accuracy included in emergency information
- ⚙️ Emergency number configuration
- 💾 Emergency number stored locally
- 🌐 Nearby communication does not require internet access

---

## 🏗️ Current Architecture

                    ┌──────────────────────┐
                    │      OfflineSOS      │
                    │    Android Device    │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
         ┌─────────┐      ┌──────────┐     ┌──────────┐
         │   GPS   │      │  Nearby  │     │   SMS    │
         │Location │      │Connection│     │ Cellular │
         └─────────┘      └────┬─────┘     └────┬─────┘
                               │                │
                               ▼                ▼
                        Nearby OfflineSOS   Emergency
                             Phones           Number

📡 Communication Technology

OfflineSOS uses Google Nearby Connections for nearby device discovery and communication.

The nearby communication layer can work without requiring an internet connection.

The application uses:

Bluetooth
Wi-Fi
Nearby Connections
GPS
SMS
🚨 SOS Information

An SOS message can contain:

Incident ID
Timestamp
Latitude
Longitude
GPS Accuracy
Battery Level
Emergency Message

Example:

🚨 OFFLINESOS EMERGENCY ALERT 🚨

Incident ID: A83F91D2

Location:
Latitude: 10.123456
Longitude: 76.123456

Accuracy: 5.2 meters

Battery: 78%

Emergency assistance required.
🛠️ Technologies Used
Technology	Purpose
Kotlin	Android application development
Android	Mobile platform
Jetpack Compose	User interface
Google Nearby Connections	Nearby device communication
GPS / LocationManager	Location detection
SMS Manager	Emergency SMS
Bluetooth	Nearby communication
Wi-Fi	Nearby communication
Gradle	Build system
Git & GitHub	Version control
📱 Required Permissions

The application uses Android permissions for:

Location
SMS
Bluetooth scanning
Bluetooth connection
Bluetooth advertising
Nearby Wi-Fi devices
Local network access on supported Android versions

Permissions are requested at runtime where required.

🧪 Current Testing

The current prototype has been tested using two Android smartphones.

Verified functionality includes:

✅ Application launches successfully
✅ GPS location detection
✅ Emergency SMS functionality
✅ Nearby permission handling
✅ Nearby device discovery
✅ Connection between two OfflineSOS phones
✅ Connected phone count
✅ Nearby message communication
🚀 Future Development

The current version is a working prototype.

Future versions will implement:

🔁 Multi-Hop SOS Relay

Allow an emergency message to travel through multiple smartphones.

Victim Phone
     ↓
 Phone B
     ↓
 Phone C
     ↓
 Phone D
     ↓
Gateway Phone
     ↓
Emergency SMS
🆔 Duplicate Message Prevention

Use the unique Incident ID to prevent the same SOS message from being processed repeatedly.

⏳ Hop Limit / TTL

Limit the number of times an SOS message can be forwarded.

📡 Gateway Detection

Identify a phone with cellular connectivity that can forward an emergency message.

💾 Local Emergency Storage

Store received SOS messages locally.

🔄 Automatic Reconnection

Reconnect devices when nearby connections are temporarily lost.

📊 Emergency Dashboard

Display:

Connected devices
Discovered devices
Received SOS messages
Relay status
Gateway status
Network status
🔐 Privacy & Security

OfflineSOS is designed to minimize dependence on internet-based communication.

Future versions will improve:

Message authentication
Secure communication
Duplicate protection
SOS message integrity
Local data protection
💻 How to Run
Requirements
Android Studio
Android SDK
Android smartphone
Bluetooth support
Wi-Fi support
GPS/location support
Installation
Clone the repository:
git clone https://github.com/YOUR-USERNAME/OfflineSOS.git
Open the project in Android Studio.
Allow Gradle to synchronize.
Connect an Android smartphone.
Build and install the application.
Grant the required permissions.
Install the application on another nearby Android phone.
Test Nearby Connections between the devices.
📂 Project Structure
OfflineSOS/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── joyeeta/
│           │           └── offlinesos/
│           │               └── MainActivity.kt
│           │
│           ├── res/
│           └── AndroidManifest.xml
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
📌 Project Status

🟢 Working Prototype

The current version successfully demonstrates nearby communication between Android devices.

The next major development phase is:

Multi-hop mesh SOS relay + duplicate prevention + gateway-based emergency delivery.

🎓 Project Type

Project: OfflineSOS
Type: Android Application / Emergency Communication System
Language: Kotlin
UI: Jetpack Compose
Communication: Google Nearby Connections + SMS
Location: GPS
Platform: Android

This project demonstrates practical knowledge of:

Android development
Kotlin
Jetpack Compose
Runtime permissions
GPS/location services
Bluetooth communication
Wi-Fi communication
SMS APIs
Mobile networking
Emergency-system design
👩‍💻 Developer

Joyeeta

Computer Science & Engineering

⭐ Future Vision

OfflineSOS aims to evolve into a resilient emergency communication system where nearby smartphones can relay emergency information even when conventional internet connectivity is unavailable.

          🚨 EMERGENCY
               │
               ▼
          ┌─────────┐
          │ Phone A │
          └────┬────┘
               │
               ▼
          ┌─────────┐
          │ Phone B │
          └────┬────┘
               │
               ▼
          ┌─────────┐
          │ Phone C │
          └────┬────┘
               │
               ▼
          ┌─────────┐
          │ Gateway │
          └────┬────┘
               │
               ▼
        📩 Emergency SMS
