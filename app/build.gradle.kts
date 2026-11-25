import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Puerto del backend Spring Boot (server.port en backend/src/main/resources/application.properties).
val puertoBackend = 8000

// URL del backend. Por defecto 127.0.0.1 = "este mismo dispositivo"; la tarea adbReverse (abajo)
// conecta ese puerto del teléfono/emulador con el del PC a través de adb (cable USB o depuración
// inalámbrica). Así el servidor NO se expone en la red y no hay que tocar el firewall ni poner IPs.
// Se puede cambiar por computador en local.properties (archivo propio, no se sube a Git):
//   taskhabit.api.url=http://10.0.2.2:8000/api/
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val urlApi: String = propiedadesLocales.getProperty(
    "taskhabit.api.url", "http://127.0.0.1:$puertoBackend/api/")

/**
 * Ejecuta "adb reverse tcp:8000 tcp:8000" en cada dispositivo conectado: desde ese momento,
 * lo que la app mande a 127.0.0.1:8000 en el teléfono llega al 127.0.0.1:8000 del PC por el
 * túnel de adb. Nunca hace fallar la compilación: si no hay dispositivo, solo avisa.
 */
abstract class AdbReverseTask : DefaultTask() {

    @get:Input
    abstract val adb: Property<String>

    @get:Input
    abstract val puerto: Property<Int>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun conectar() {
        val dispositivos = listarDispositivos()
        if (dispositivos.isEmpty()) {
            logger.warn("adbReverse: no hay dispositivos conectados; se omite el túnel al backend.")
            return
        }
        val tcp = "tcp:${puerto.get()}"
        for (serie in dispositivos) {
            val resultado = exec.exec {
                commandLine(adb.get(), "-s", serie, "reverse", tcp, tcp)
                isIgnoreExitValue = true
            }
            if (resultado.exitValue == 0) {
                logger.lifecycle("adbReverse: $serie -> 127.0.0.1:${puerto.get()} del PC")
            } else {
                logger.warn("adbReverse: no se pudo crear el túnel en $serie")
            }
        }
    }

    /** Lee "adb devices": cada línea "serie<TAB>device" es un dispositivo listo. */
    private fun listarDispositivos(): List<String> {
        val salida = ByteArrayOutputStream()
        val resultado = try {
            exec.exec {
                commandLine(adb.get(), "devices")
                standardOutput = salida
                isIgnoreExitValue = true
            }
        } catch (e: Exception) {
            logger.warn("adbReverse: no se encontró adb (${e.message}).")
            return emptyList()
        }
        if (resultado.exitValue != 0) return emptyList()
        return salida.toString().lines()
            .drop(1)
            .map { it.trim().split("\t") }
            .filter { it.size == 2 && it[1] == "device" }
            .map { it[0] }
    }
}

val adbReverse = tasks.register<AdbReverseTask>("adbReverse") {
    description = "Conecta el puerto del backend del PC con los dispositivos conectados (adb reverse)."
    adb.set(androidComponents.sdkComponents.adb.map { it.asFile.absolutePath })
    puerto.set(puertoBackend)
}

// Se ejecuta en cada compilación (Android Studio compila antes de cada ▶ Run).
tasks.named("preBuild") {
    dependsOn(adbReverse)
}

android {
    namespace = "com.taskhabit.academic"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.taskhabit.academic"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Genera la constante BuildConfig.API_URL que usa ApiClient.
        buildConfigField("String", "API_URL", "\"$urlApi\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.retrofit.lib)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // --- Módulo de asistencia: cámara (QR) + GPS ---
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.guava)
    implementation(libs.play.services.location)
    implementation(libs.zxing.core)
    implementation(libs.recyclerview.lib)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
}
