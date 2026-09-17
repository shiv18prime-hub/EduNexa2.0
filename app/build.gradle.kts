plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("com.google.gms.google-services") }
android { namespace="com.edunexa.app";compileSdk=35;defaultConfig{applicationId="com.edunexa.app";minSdk=23;targetSdk=35;versionCode=1;versionName="1.0.0"};buildFeatures{viewBinding=true};compileOptions{sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17};kotlinOptions{jvmTarget="17"} }
dependencies {
 implementation(platform("com.google.firebase:firebase-bom:33.7.0"));implementation("com.google.firebase:firebase-auth");implementation("com.google.firebase:firebase-firestore");implementation("com.google.firebase:firebase-messaging")
 implementation("com.google.android.gms:play-services-auth:21.2.0");implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
 implementation("androidx.core:core-ktx:1.15.0");implementation("androidx.appcompat:appcompat:1.7.0");implementation("com.google.android.material:material:1.12.0");implementation("androidx.constraintlayout:constraintlayout:2.2.0");implementation("androidx.documentfile:documentfile:1.0.1");implementation("com.tom-roush:pdfbox-android:2.0.27.0");implementation("com.google.zxing:core:3.5.3")
}
