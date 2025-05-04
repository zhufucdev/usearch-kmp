import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*
import kotlin.io.path.Path

abstract class CMakeConfigureForKonanTask : CMakeConfigureTask() {
    @get:Input
    abstract val target: Property<KonanTarget>

    @get:Input
    abstract val buildType: Property<String>

    private fun readLocalProperties(): Properties {
        val p = Properties()
        File(project.rootDir, "local.properties").inputStream().use {
            p.load(it)
        }
        return p
    }

    init {
        buildType.set("RELEASE")

        def.value(target.zip(buildType) { t, b -> t to b }.map { (target, buildType) ->
            buildMap {
                put(
                    "CMAKE_SYSTEM_PROCESSOR",
                    when (target.architecture) {
                        Architecture.ARM64, Architecture.ARM32 -> "arm"
                        Architecture.X64, Architecture.X86 -> "x86"
                    },
                )
                put("CMAKE_BUILD_TYPE", buildType)

                when (target) {
                    KonanTarget.ANDROID_ARM32,
                    KonanTarget.ANDROID_ARM64,
                    KonanTarget.ANDROID_X64,
                    KonanTarget.ANDROID_X86 -> {
                        put("CMAKE_SYSTEM_NAME", "Android")
                        remove("CMAKE_SYSTEM_PROCESSOR")
                        put(
                            "CMAKE_ANDROID_ARCH_ABI", when (target.architecture) {
                                Architecture.X64 -> "x86_64"
                                Architecture.X86 -> "x86"
                                Architecture.ARM64 -> "arm64-v8a"
                                Architecture.ARM32 -> "armeabi-v7a"
                            }
                        )

                        val localProperties = readLocalProperties()
                        val sdkPath = localProperties.getProperty("sdk.dir")
                        val ndkVersion = localProperties.getOrElse("ndk") {
                            File(sdkPath).listFiles { it: File -> it.isDirectory }.maxBy { it.name }?.name
                        } as String?
                        if (ndkVersion == null) {
                            error("Cannot decide Android NDK version.")
                        }
                        put(
                            "CMAKE_ANDROID_NDK",
                            Path(sdkPath, "ndk", ndkVersion).toString()
                        )
                    }

                    KonanTarget.TVOS_ARM64,
                    KonanTarget.TVOS_SIMULATOR_ARM64,
                    KonanTarget.TVOS_X64,
                    KonanTarget.WATCHOS_ARM32,
                    KonanTarget.WATCHOS_ARM64,
                    KonanTarget.WATCHOS_DEVICE_ARM64,
                    KonanTarget.WATCHOS_SIMULATOR_ARM64,
                    KonanTarget.WATCHOS_X64,
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_SIMULATOR_ARM64,
                    KonanTarget.IOS_X64 -> {
                        put("CMAKE_SYSTEM_NAME", "iOS")
                        put(
                            "CMAKE_OSX_ARCHITECTURES", when (target.architecture) {
                                Architecture.X64 -> "x86_64"
                                Architecture.X86 -> "i386"
                                Architecture.ARM64 -> "arm64"
                                Architecture.ARM32 -> "armv7"
                            }
                        )
                        put("CMAKE_OSX_DEPLOYMENT_TARGET", "16.0")
                    }

                    KonanTarget.LINUX_ARM32_HFP,
                    KonanTarget.LINUX_ARM64,
                    KonanTarget.LINUX_X64 -> {
                        put("CMAKE_SYSTEM_NAME", "Linux")
                        val compilerSeries = when (target.architecture) {
                            Architecture.X64 -> "x86_64-linux-gnu"
                            Architecture.X86 -> "i686-linux-gnu"
                            Architecture.ARM64 -> "aarch64-linux-gnu"
                            Architecture.ARM32 -> "arm-linux-gnueabi-gnu"
                        }
                        put("CMAKE_C_COMPILER", "$compilerSeries-gcc")
                        put("CMAKE_CXX_COMPILER", "$compilerSeries-g++")
                        put("CMAKE_FIND_ROOT_PATH_MODE_PROGRAM", "NEVER")
                        put("CMAKE_FIND_ROOT_PATH_MODE_LIBRARY", "ONLY")
                        put("CMAKE_FIND_ROOT_PATH_MODE_INCLUDE", "ONLY")
                        put("CMAKE_FIND_ROOT_PATH_MODE_PACKAGE", "ONLY")
                    }

                    KonanTarget.MACOS_ARM64 -> {
                        put("CMAKE_SYSTEM_NAME", "Darwin")
                        put("CMAKE_OSX_ARCHITECTURES", "arm64")
                    }

                    KonanTarget.MACOS_X64 -> {
                        put("CMAKE_SYSTEM_NAME", "Darwin")
                        put("CMAKE_OSX_ARCHITECTURES", "x86_64")
                    }

                    KonanTarget.MINGW_X64 -> {
                        put("CMAKE_SYSTEM_NAME", "Windows")
                        val compilerSeries = "x86_64-w64-mingw32"
                        put("CMAKE_C_COMPILER", "$compilerSeries-gcc")
                        put("CMAKE_CXX_COMPILER", "$compilerSeries-g++")
                    }
                }

                when (target) {
                    KonanTarget.IOS_SIMULATOR_ARM64 -> {
                        put("CMAKE_OSX_SYSROOT", "iphonesimulator")
                    }

                    KonanTarget.TVOS_SIMULATOR_ARM64 -> {
                        put("CMAKE_OSX_SYSROOT", "appletvsimulator")
                    }

                    KonanTarget.WATCHOS_SIMULATOR_ARM64 -> {
                        put("CMAKE_OSX_SYSROOT", "watchsimulator")
                    }

                    else -> {}
                }
            }
        })
    }
}