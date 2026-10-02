# UltraLink

UltraLink is a native Android university project exploring the acquisition, annotation and sharing of images and videos in a point-of-care ultrasound workflow. It connects Bluetooth file transfer, local media editing, a personal gallery and clinician-to-clinician conversations in one application.

This is an academic prototype. The repository implements generic Bluetooth media transfer; it does not contain a vendor-specific ultrasound acquisition SDK or evidence of clinical validation.

## Features

- **Accounts and profiles:** Firebase Authentication sign-in/registration, with profile information stored in Cloud Firestore.
- **Bluetooth acquisition:** device discovery, connection and transfer of text, images and videos over classic Bluetooth sockets, with transfer progress and received media opened in the editors.
- **Image editing:** freehand drawing and erasing, draggable text and shape annotations, size adjustments, cropping and exporting the composed image.
- **Video editing:** playback, selecting a clip range, trimming and extracting a frame for image editing.
- **Conversations:** Firestore-backed direct messages, media sharing and audio recording/upload. Images appear directly inside the conversation, while videos have thumbnails that open the editor.
- **Gallery:** previously saved media associated with the signed-in user, retrieved through Firestore metadata and Firebase Storage URLs.
- **Cloud media storage:** image/video uploads and metadata linking media to users and conversations.

## Team and contribution

Developed by Bernardo Godinho and Tiago. Bernardo was responsible for Bluetooth transfer, image editing, work on the main page, the initial Firebase connection and storage integration, inline image previews in chat, and the gallery of previous images. The application was developed collaboratively using Git.

## Technology

| Layer | Implementation |
| --- | --- |
| Android application | Java, Android XML layouts, AndroidX and Material components |
| Build | Gradle Kotlin DSL, Gradle wrapper 8.9, Android Gradle Plugin 8.7.1 |
| Android configuration | Minimum SDK 26, target SDK 33, compile SDK 35; Java source/target level 11 |
| Cloud services | Firebase Authentication, Cloud Firestore, Firebase Storage, Firebase Cloud Messaging |
| Media | Glide, uCrop, AndroidX Media3 and mobile-ffmpeg |
| Device communication | Android classic Bluetooth client/server sockets |

The Kotlin DSL build files do not imply that the application itself is written in Kotlin.

## Source map

Application Java sources are under `app/src/main/java/org/feup/apm/cmeb_login/`.

| File or directory | Purpose |
| --- | --- |
| `LoginActivity.java`, `RegisterActivity.java` | Authentication and account setup |
| `HomeFragment.java` | Main acquisition and media-selection entry points |
| `AcquireFile.java` | Bluetooth UI, discovery, transfer and received-file handling |
| `BluetoothConnectionService.java` | Accept/connect threads, socket I/O and transfer framing |
| `ImageEditorActivity.java` | Image annotations, cropping, saving and sharing |
| `VideoEditorActivity.java` | Video playback, clipping, frame capture and sharing |
| `GalleryActivity.java` | Media associated with the current user |
| `ChatActivity.java` | Conversations, media/audio upload and notification integration |
| `adapter/ChatRecyclerAdapter.java` | Inline media previews and opening shared media |
| `UploadService.java` | Background media upload |
| `util/FirebaseUtil.java` | Shared Firebase collection and user helpers |
| `app/src/main/res/` | Layouts, strings, drawables and other resources |

## Data flow

1. Sign in and acquire a file through Bluetooth or select local media.
2. Open the file in the image or video editor.
3. Annotate/crop an image, trim a video or extract a still frame.
4. Upload media to Firebase Storage and save its metadata in Firestore.
5. Save it to the user's gallery or share it in a conversation.
6. Open shared images/videos directly from their chat preview.

Firestore uses `users`, `chatrooms` (with a `chats` subcollection), and `media` collections. Media metadata includes a URL, media type and associations with user/chat IDs. Storage paths include user-specific image, video and profile-image locations.

Bluetooth peers must use the same service UUID and framing protocol. This is not a generic Android file-sharing receiver: text and media-size messages use a `TYPE:length\n` header, and image/video payload lengths are exchanged before the binary content. See the sender in `AcquireFile.java` and receiver in `BluetoothConnectionService.java` together when implementing another peer.

## Development setup

1. Clone the repository and open its root in Android Studio.
2. Install Android SDK 35 and use a Gradle JDK compatible with Android Gradle Plugin 8.7.1. The Java 11 source setting is not the Gradle runtime requirement.
3. Create your own Firebase project and register the Android application ID `org.feup.apm.cmeb_login`.
4. Place your Firebase Android client configuration at `app/google-services.json`; this file is not included in the repository.
5. Enable email/password Authentication, Firestore and Storage, and configure appropriate access rules. Backend rules/index definitions are not supplied here.
6. Review the notification implementation described below before enabling that feature.
7. Sync Gradle, then build/run the `app` module. For a command-line debug build on Windows:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

Use a physical Bluetooth-capable Android device for transfer testing. Grant the Bluetooth/media/microphone permissions requested by the relevant flows. A second compatible peer is needed for Bluetooth transfer, and separate signed-in users are needed to exercise conversations. Use copies of non-sensitive sample images and videos.

## Prototype limitations and verification

- **Device interoperability:** the code demonstrates a custom Bluetooth transport; the tested ultrasound hardware or peer device is not documented.
- **Android compatibility:** although minimum SDK is 26, Bluetooth header decoding is currently inside an Android 13/API 33 check. Earlier Android versions need a code fix before this receive path can work reliably.
- **Notifications:** `ChatActivity.sendNotification` expects a service-account JSON asset and includes a project-specific FCM endpoint. That asset is not in this repository. Do not add service-account credentials to an Android app; move privileged FCM sending to a trusted backend and update the integration.
- **Backend reproducibility:** Firebase configuration, security rules and deployment setup must be supplied separately. Authentication alone does not establish correct Firestore/Storage authorization.
- **Dependency reproducibility:** the project uses pinned media dependencies, including `mobile-ffmpeg-full:4.4`; dependency resolution should be checked when restoring the development environment.
- **Transfer robustness:** the custom protocol and socket lifecycle need testing for interrupted transfers, malformed headers, large payloads and reconnection.
- **Validation:** this README was checked against source code. It does not claim a successful fresh build, end-to-end cloud test, ultrasound-device test or clinical suitability.

A useful manual check is: register two users → transfer a sample file → annotate/crop → upload → confirm gallery persistence → send in chat → open the recipient's inline preview. Repeat for video clipping and interrupted Bluetooth connections.
