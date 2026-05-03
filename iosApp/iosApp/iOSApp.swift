import SwiftUI
import FirebaseCore
import FirebaseFirestore
import GoogleMaps

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()

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
