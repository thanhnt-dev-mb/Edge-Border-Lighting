import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

class AndroidBasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val allLibs = libs.libraryAliases
        allLibs.forEach { lib ->
            println("Available libraries: $lib")
        }
        
        target.dependencies.add("implementation", libs.findLibrary("androidx-core-ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.appcompat").get())
        target.dependencies.add("implementation", libs.findLibrary("material").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.constraintlayout").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.lifecycle.livedata.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.lifecycle.viewmodel.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.navigation.fragment.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.navigation.ui.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.work.runtime.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("timber").get())
        target.dependencies.add("implementation", libs.findLibrary("gson").get())
        target.dependencies.add("implementation", libs.findLibrary("glide").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.fragment.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.legacy.support.v4").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.recyclerview").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.activity").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.core.animation").get())
        target.dependencies.add("implementation", libs.findLibrary("locale.helper.android").get())
        target.dependencies.add("implementation", libs.findLibrary("lottie").get())
        target.dependencies.add("implementation", libs.findLibrary("firebase.analytics.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("firebase.crashlytics.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("firebase.config.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("firebase.messaging.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.webkit").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.room.runtime").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.room.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("hilt.android").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.hilt.work").get())
        target.dependencies.add("implementation", libs.findLibrary("logging.interceptor").get())
        target.dependencies.add("implementation", libs.findLibrary("retrofit").get())
        target.dependencies.add("implementation", libs.findLibrary("converter.gson").get())
        target.dependencies.add("implementation", libs.findLibrary("library").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.paging.runtime").get())
        target.dependencies.add("implementation", libs.findLibrary("androidx.room.paging").get())
        target.dependencies.add("implementation", libs.findLibrary("shimmer").get())
        target.dependencies.add("implementation", libs.findLibrary("sdp.android").get())
        target.dependencies.add("implementation", libs.findLibrary("ssp.android").get())
        target.dependencies.add("implementation", libs.findLibrary("billing.ktx").get())
        target.dependencies.add("implementation", libs.findLibrary("timber").get())

        target.dependencies.add("ksp", libs.findLibrary("compiler").get())
        target.dependencies.add("ksp", libs.findLibrary("androidx.hilt.compiler").get())
        target.dependencies.add("ksp", libs.findLibrary("hilt.android.compiler").get())
        target.dependencies.add("ksp", libs.findLibrary("androidx.room.compiler").get())

    }
}