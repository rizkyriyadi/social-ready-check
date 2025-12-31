# Social Ready Check

A social Android application built with Kotlin and Jetpack Compose, featuring real-time social features powered by Firebase.

## Features

- **Google Sign-In** - Secure authentication with Google accounts
- **User Profiles** - Customizable profiles with avatars, usernames, and bios
- **Friend System** - Add friends via QR code scanning or username search
- **Friend Requests** - Send, accept, and reject friend requests
- **Push Notifications** - Real-time FCM notifications for friend requests
- **Modern UI** - iOS-style design with Jetpack Compose

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern declarative UI
- **Firebase Auth** - Google Sign-In authentication
- **Cloud Firestore** - Real-time database for users, friends, requests
- **Cloud Functions** - Backend logic for reciprocal friend operations
- **Firebase Cloud Messaging** - Push notifications
- **CameraX** - QR code scanning
- **Coil** - Image loading

## Project Structure

```
app/src/main/java/com/example/tripglide/
├── data/
│   ├── model/          # Data models (User, Friend, FriendRequest)
│   └── repository/     # Firebase repositories
├── navigation/         # Compose Navigation
├── service/            # FCM Service
├── ui/
│   ├── components/     # Reusable UI components
│   ├── home/           # Home screen
│   ├── login/          # Authentication screens
│   ├── onboarding/     # User onboarding flow
│   ├── profile/        # Profile screens
│   ├── social/         # Social features (Friends, Requests, etc.)
│   └── theme/          # App theming
└── TripGlideApplication.kt
```

## Firebase Setup

1. Create a Firebase project
2. Add Android app with package name `com.yamdimologi.social_media_ready`
3. Download `google-services.json` to `app/` folder
4. Enable Authentication (Google Sign-In)
5. Enable Firestore Database
6. Deploy Cloud Functions: `firebase deploy --only functions`
7. Deploy Firestore rules: `firebase deploy --only firestore:rules`

## Building

```bash
./gradlew assembleDebug
```

## License

MIT License
