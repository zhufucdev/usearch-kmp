# A template for Kotlin multiplatform projects that interoperate with Rust

This is a template for a Kotlin Multiplatform project, thoughtfully designed to seamlessly incorporate Rust within the
codebase.

## Structure

This project is structured in the following way:

- The Rust code is managed using Cargo, and is placed in the `rustMain` directory.
  Note that this is not a Kotlin MP sourceSet. It is just a way to organize code for the better.
- Every sourceSet can be used to store the code relative to the specific platform as usual.

Gradle is used as the main build tool for the whole project.
The Cargo commands that must be used for building the Rust library are encapsulated in buildSrc
as a plugin `crab-mulitiplatform` too.

### Interoperability in Kotlin/Native

Here's how it is possible to use Rust in Kotlin thanks to this configuration:

- The `Cargo.toml` file is used to store the configuration of the Rust project.
  Pay attention to the line `crate_type = ["cdylib"]`.
  This means that the Rust project does not contain a main entrypoint, but a collection of possible APIs.
- Our Rust project is a library: the file `lib.rs` can expose some operations and structure.
  In this case, just a simple `plus` method is implemented.
  The `#[no_mangle]` and `pub extern "C"` lines are important, but we won't explain their meaning in this README.
- Kotlin is unable to directly use the Rust library,
  but we can create a header file (`.h`) using [cbindgen](https://github.com/mozilla/cbindgen) and our Rust code.
  cbindgen is, in fact, able to create C/C++11 headers for Rust libraries which expose a public C API.
  The `cbindgen.toml` file in the root contains some configuration for the tool.
- The `nativeInterop` folder is used to automatically create a bridge between the Rust library and Kotlin Native.
  using cinterop, a `.def` file is used to specify some configuration for the C compiler and linker.
  Some configuration are also added in `build.gradle.kts`.
  Check [C Interoperability](https://kotlinlang.org/docs/native-c-interop.html) for more info.
- Some additional configuration are added to the `build.gradle.kts`.
  Using `linkerOpts` the effective library (`.a` in Linux and macOS, `.lib ` in Windows) is linked during the build
  process.
  This could also be configured in the `.def` file, but I'm not doing it because
  of [KT-48082 issue](https://youtrack.jetbrains.com/issue/KT-48082).
- A corresponding `plus` function is automatically created by Kotlin and can be used in the native sourceSet.

### Interoperability with Android & JVM

- JVM and Android come with different JNI loading mechanisms, while the statically-linked binary is shared.
  This brings a `jvmCommonMain` sources set, where the JNI method is declared as `external` and the platforms
  both implements their own `platformLoadLib` procedure.
- JVM implements a way of packing up commonly used binaries for macOS, Windows and Linux systems into the
  jniResources directory, and unpacking to the temporary directory when loading.
- Android packs up binaries for common ABIs in the jniLibs directory, and incorporates the corresponding
  syscall to load.

## Building and running the example

It is possible to launch the example with the command of you specific platform:

```shell
gradle runDebugExecutableLinuxX64 # Linux
gradle runDebugExecutableMacosX64 # MacOS
gradle runDebugExecutableMingwX64 # Windows
gradle jvmRun -DmainClass=HelloWorldKt --quiet # JVM
```

For Android, refer to the androidInstrumentedTest, which should run via Android Studio or Intellij IDEA
on a real phone or an emulator.

The build is configured to automatically generate the Rust library artifacts when building the Kotlin Multiplatform
project,
and copying to platform specific locations as mentioned above.
However, it is possible to manually generate them using the following command:

```shell
gradle cargoBuildRelease
```

The header file will also be generated inside the `target` folder.

## Incorporating the template
You can either use this project as a submodule or a standalone library.
For personal reasons, this library is named as `vectoria`, which you can reverse in simple steps:

- Modify fields under `buildSrc/src/main/kotlin/Library.kt`. Namespace is an android library and maven publish requirement. 
- Change the `LIB_NAME` constant under `src/jvmCommonMain/kotlin/Lib.kt`, which is supposed to be
the same as the one under `Library.kt`, which I failed to make dry enough, but whatever.
- Package and lib name under `Cargo.toml`, same as above.
- Either change the POM info in `build.gradle.kts` or remove the publishing block if you don't need it.
- Project name under `settings.gradle.kts`.

# Some Credits

Started from [AngeloFilaseta](https://github.com/AngeloFilaseta)'s
[template-for-kotlin-multiplatform-rust-interoperability](https://github.com/AngeloFilaseta/template-for-kotlin-multiplatform-rust-interoperability)
repository.