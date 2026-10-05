# Project-specific R8 rules for the release build.

# ML Kit discovers its component registrars by class name from manifest metadata and
# creates them by reflection, so R8 cannot see that their constructors are used.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
