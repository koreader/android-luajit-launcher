# proguard is used to shrink and optimize dependencies.
-keep class org.koreader.launcher.** { *; }

# keep kotlin.Metadata annotations
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
