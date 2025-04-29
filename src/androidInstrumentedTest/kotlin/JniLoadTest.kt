import org.junit.Test
import usearch.platformLoadLib

class JniLoadTest {
    @Test
    fun shouldOutputPlatformName() {
        platformLoadLib()
    }
}