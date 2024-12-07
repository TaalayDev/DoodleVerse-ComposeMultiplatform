import SwiftUI
import Firebase
import FirebaseAnalytics

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
