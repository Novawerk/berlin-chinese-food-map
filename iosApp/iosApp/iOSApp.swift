import SwiftUI
import FirebaseCore
import FirebaseFirestore
import GoogleMaps

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        configureFirebase()

        // Persistent disk cache for Firestore so cold starts paint from
        // the last sync before the network round-trip. MUST run before
        // anything else touches Firestore — once the instance has been
        // "started" (which gitlive's Firebase.firestore getter does
        // implicitly on first access from Kotlin), assigning settings
        // throws FIRIllegalStateException and crashes the process.
        // We do it here so the Kotlin DI graph can construct
        // FirestoreRestaurantRepository freely afterward.
        let firestoreSettings = FirestoreSettings()
        firestoreSettings.cacheSettings = PersistentCacheSettings()
        Firestore.firestore().settings = firestoreSettings

        // Google Maps SDK init. Key is sourced from
        // Configuration/Config.xcconfig (MAPS_API_KEY=…), surfaced into
        // Info.plist via $(MAPS_API_KEY), and read here at runtime.
        // Crashing fast on a missing key is intentional — silently falling
        // back to "no map tiles" hides the misconfig until QA.
        guard let mapsApiKey = Bundle.main.object(forInfoDictionaryKey: "MAPS_API_KEY") as? String,
              !mapsApiKey.isEmpty else {
            fatalError("MAPS_API_KEY is missing — set it in iosApp/Configuration/Config.xcconfig.")
        }
        GMSServices.provideAPIKey(mapsApiKey)

        return true
    }

    // Uses GoogleService-Info.plist when bundled, otherwise falls back to
    // in-code FirebaseOptions. Bare FirebaseApp.configure() raises an
    // NSException and kills the app on launch when the plist isn't in the
    // .app — Xcode Cloud archives have shipped without it (TestFlight
    // build 27, App Review build 69). Per Firebase guidance these
    // client-config values aren't secrets; the Bundle ID gate and
    // Firestore Security Rules are the actual perimeter. Keep these in
    // sync with iosApp/iosApp/GoogleService-Info.plist.
    private func configureFirebase() {
        let options: FirebaseOptions = {
            if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
               let bundled = FirebaseOptions(contentsOfFile: path) {
                return bundled
            }
            let fallback = FirebaseOptions(
                googleAppID: "1:364041827824:ios:1ca70ae91bc43be6165935",
                gcmSenderID: "364041827824"
            )
            fallback.apiKey = "AIzaSyAy8HvQyj1gqKxMIKTjrIr5tYaVF9U4SY8"
            fallback.projectID = "novawerk-7dd18"
            fallback.storageBucket = "novawerk-7dd18.firebasestorage.app"
            fallback.bundleID = "com.novawerk.berlinfoodmap"
            return fallback
        }()
        FirebaseApp.configure(options: options)
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
