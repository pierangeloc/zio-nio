package zio.nio.file

import testz.{Harness, assert}
import zio.DefaultRuntime

object PathSuite extends DefaultRuntime {

  def tests[T](harness: Harness[T]): T = {
    import harness._

    section(

      test("Path construction") { () =>
        val p = Path("a", "b") / "c/d"
        val elements = p.elements.map(_.toString)
        assert(elements == List("a", "b", "c", "d"))
      }
    )

  }

}
