import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.logging.Logger
import java.io.*
import java.util.concurrent.*

class CMakeExecutor internal constructor(private val logger: Logger, private val taskName: String?) {
    fun exec(cmdLine: MutableList<String>, workingFolder: File) {
        // log command line parameters
        val sb = StringBuilder("  CMakePlugin.task $taskName - exec: ")
        for (s in cmdLine) {
            sb.append(s).append(" ")
        }
        logger.info(sb.toString())
        val pb = ProcessBuilder(cmdLine)
        pb.directory(workingFolder)
        try {
            Executors.newFixedThreadPool(2).use { executor ->
                // make sure working folder exists
                val b = workingFolder.mkdirs()
                // start
                val process = pb.start()
                val stdoutFuture = executor.submit<Void?> {
                    readStream(process.inputStream, true)
                    null
                }
                val stderrFuture = executor.submit<Void?> {
                    readStream(process.errorStream, false)
                    null
                }
                val retCode = process.waitFor()
                warnIfTimeout(
                    stdoutFuture,
                    "CMakeExecutor[$taskName]Warn: timed out waiting for stdout to be closed."
                )
                warnIfTimeout(
                    stderrFuture,
                    "CMakeExecutor[$taskName]Warn: timed out waiting for stderr to be closed."
                )
                if (retCode != 0) {
                    throw GradleException("[$taskName]Error: CMAKE returned $retCode")
                }
            }
        } catch (e: IOException) {
            throw GradleScriptException("CMakeExecutor[$taskName].", e)
        } catch (e: InterruptedException) {
            throw GradleScriptException("CMakeExecutor[$taskName].", e)
        } catch (e: ExecutionException) {
            throw GradleScriptException("CMakeExecutor[$taskName].", e)
        }
    }

    private fun readStream(inputStream: InputStream, isStdOut: Boolean) {
        val lines = BufferedReader(InputStreamReader(inputStream)).lines()
        if (isStdOut) {
            lines.forEach { s: String? -> logger.info(s) }
        } else {
            lines.forEach { s: String? -> logger.error(s) }
        }
    }

    private fun warnIfTimeout(future: Future<Void?>, message: String?) {
        try {
            future.get(3, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            logger.warn(message)
        }
    }
}

