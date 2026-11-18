import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.uk2solutions.sudoku"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uk2solutions.sudoku"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        // 개인용 빌드(personal)만 true. 스토어에 올리는 debug/release에서는 항상 false.
        buildConfigField("boolean", "ALL_UNLOCKED", "false")
        manifestPlaceholders["appLabel"] = "@string/app_name"
    }

    // 릴리스 서명: 저장소 루트의 keystore.properties(커밋 금지, .gitignore 등록)가 있을 때만 적용한다.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // 개인용: 프리미엄 기능 전부 열림 + 다른 패키지(.full)라 정식 앱과 함께 설치 가능. 디버그 키로 서명. 스토어 업로드 금지.
        create("personal") {
            initWith(getByName("release"))
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "ALL_UNLOCKED", "true")
            manifestPlaceholders["appLabel"] = "sudoku7979 Full"
            matchingFallbacks += "release"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
    lint { warningsAsErrors = false; abortOnError = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.android.billingclient:billing:9.1.0")

    testImplementation("junit:junit:4.13.2")
    // 화면 스크린샷(설치 없이 PC에서 렌더링)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// 스크린샷 테스트는 ui-test-manifest(debug 전용)가 필요하므로 릴리스 변형에서는 제외한다.
tasks.withType<Test>().configureEach {
    if (name.contains("Release")) exclude("**/screens/**", "**/a11y/SemanticsTest*")
    if (name.contains("Personal")) enabled = false // 개인용 빌드는 단위 테스트를 debug/release에서만 돌린다
}
