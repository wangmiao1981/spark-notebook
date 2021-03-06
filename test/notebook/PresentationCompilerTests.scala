package notebook

import notebook.util.Match
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class PresentationCompilerTests extends Specification {
  def complete(pc:PresentationCompiler)(s:String, i:Int) = {
    val (st, com) = pc.complete(s, i)
    (st, com.toSet)
  }
  "complete" should {
    val cz = """|
      |class AnExample(val aField:Int, notAField:Boolean=true) {
      |  def testVar:String = ""
      |  def testMethod(a:String):String = ""
      |  def testMethod(a:String, b:String):String = ""
      |  def testMethod(a:Int, optionalB: String = ""):String = ""
      |  lazy val toSchwarz:Float = 1f
      |}
      |implicit class AnExampleWithImplicits(cls: AnExample) {
      |  def implicitMethod(a: Int): Int = 1
      |}
      |""".stripMargin

    val newInst = "val test = new AnExample(123)"
    val newLine = "\n"

    "return the correct completions" in {
      val line = "test.toS"
      val code = List(newInst, newLine, line).mkString

      val pc = new PresentationCompiler(Nil)
      pc.addScripts(cz)

      val c = complete(pc) _
      c(code, code.size) must beEqualTo("toS", Set(
        Match("toSchwarz", Map("display_text" -> "toSchwarz: Float")),
        Match("toString", Map("display_text" -> "toString: String"))
      ))
      c(code + "\nval testAsSt$ring = test.toString()", code.size) must beEqualTo("toS", Set(
        Match("toSchwarz", Map("display_text" -> "toSchwarz: Float")),
        Match("toString", Map("display_text" -> "toString: String"))
      ))
    }

    "lists all overrided method versions, indicating optional parameters if any" in {
      val line = "test.testMeth"
      val code = List(newInst, newLine, line).mkString

      val pc = new PresentationCompiler(Nil)
      pc.addScripts(cz)
      val c = complete(pc) _

      c(code, code.size) must beEqualTo("testMeth", Set(
        Match("testMethod(a: Int, [optionalB: String])",
          Map("display_text" -> "testMethod(a: Int, [optionalB: String]): String")),
        Match("testMethod(a: String)", Map("display_text" -> "testMethod(a: String): String")),
        Match("testMethod(a: String, b: String)", Map("display_text" -> "testMethod(a: String, b: String): String"))
      ))
    }

    "lists the methods inherited and the implicit methods" in {
      val pc = new PresentationCompiler(Nil)
      pc.addScripts(cz)

      val c = complete(pc) _

      val code1 = List(newInst, newLine, "test.").mkString
      val suggestions: Set[String] = c(code1, code1.size)._2.map {case Match(s, _) => s }
      println(suggestions.map(s=> s""""${s}""""))

      suggestions must containAllOf(Seq(
        "+(other: String)",
        "clone",
        "hashCode",
        "asInstanceOf",
        "getClass",
        "isInstanceOf",
        "implicitMethod(a: Int)"
      ))
    }
  }
}
