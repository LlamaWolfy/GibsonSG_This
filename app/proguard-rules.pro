# Add project specific ProGuard rules here.
# Keep Room entities/DAOs generated code and kotlinx.serialization models.
-keep class com.gibsonsg.todo.core.data.db.entity.** { *; }
-keep class com.gibsonsg.todo.core.domain.model.** { *; }
-keepattributes *Annotation*
