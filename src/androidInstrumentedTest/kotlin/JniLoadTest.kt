import org.junit.Test
import usearch.Index
import usearch.IndexOptions
import usearch.MetricKind
import usearch.ScalarKind
import usearch.platformLoadLib

class JniLoadTest {
    @Test
    fun shouldLoadJni() {
        platformLoadLib()
    }

    @Test
    fun shouldCreateIndex() {
        Index(IndexOptions(3u, MetricKind.Cos, ScalarKind.F32))
    }
}