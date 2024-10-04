package io.joern.kotlin2cpg.querying

import io.joern.kotlin2cpg.testfixtures.KotlinCode2CpgFixture
import io.shiftleft.codepropertygraph.generated.DispatchTypes
import io.shiftleft.semanticcpg.language.*

class StdLibTests extends KotlinCode2CpgFixture(withOssDataflow = false) {
  "CPG for code with call to `takeIf`" should {
    val cpg = code("""
        |  package mypkg
        |
        |  import kotlin.random.Random
        |  import java.util.UUID
        |
        |  fun main() {
        |    val r = Random.nextInt(0, 100)
        |    val x =
        |      if(r < 50) {
        |        null
        |      } else {
        |        UUID.randomUUID()
        |      }
        |    val p = x.takeIf { it != null }
        |    println(p)
        |  }
        |""".stripMargin)

    "should contain a CALL node with the correct METHOD_FULL_NAME for `takeIf`" in {
      val List(c) = cpg.call.code("x.takeIf.*").l
      c.methodFullName shouldBe "kotlin.takeIf:java.lang.Object(java.lang.Object,kotlin.Function1)"
      c.dispatchType shouldBe DispatchTypes.STATIC_DISPATCH
      c.signature shouldBe "java.lang.Object(java.lang.Object,kotlin.Function1)"
      c.typeFullName shouldBe "java.util.UUID"
    }
  }

  "CPG for code with a single call to println and a corresponding import" should {
    val cpg = code("""
        |package mypkg
        |
        |import kotlin.io.println
        |
        |fun foo() {
        |  println("bar")
        |}
        |""".stripMargin)

    "should contain a CALL node with the correct METHOD_FULL_NAME" in {
      val List(c) = cpg.call.code("println.*").l
      c.methodFullName shouldBe "kotlin.io.println:void(java.lang.Object)"
    }
  }

  "CPG for code with a single call to println, a corresponding import and a locally defined println method" should {
    val cpg = code("""
        |package mypkg
        |
        |fun println(baz: String) {
        |  print("prefix: " + baz + "\n")
        |}
        |
        |fun foo() {
        |  println("bar")
        |}
        |""".stripMargin)

    "should contain a CALL node with the correct METHOD_FULL_NAME" in {
      val List(c) = cpg.call.code("println.*").l
      c.methodFullName shouldBe "mypkg.println:void(java.lang.String)"
    }
  }

  "CPG for code with a call to static class method of imported class" should {
    val cpg = code("""
        |package mypkg
        |
        |fun foo() {
        |  val runtime = Runtime.getRuntime()
        |  runtime.exec("ls")
        |}
        |""".stripMargin)

    "should contain a CALL node for call to static class method" in {
      cpg.call.code("Runtime.getRuntime\\(\\)").size shouldBe 1
    }

    "should contain a CALL node for call to instance method" in {
      cpg.call.code("runtime.exec.*").size shouldBe 1
    }

    "should contain a LOCAL node with an inferred TYPE_FULL_NAME set" in {
      val List(l) = cpg.local.code(".*runtime.*").l
      l.typeFullName shouldBe "java.lang.Runtime"
    }
  }

  "CPG for code with a chained call to static class method of imported class" should {
    val cpg = code("""
        |package mypkg
        |
        |fun foo() {
        |  Runtime.getRuntime().exec("ls")
        |}
        |""".stripMargin)

    "should contain a CALL node for call to instance method" in {
      val List(c) = cpg.call.code(".*exec.*").l
      c.methodFullName shouldBe "java.lang.Runtime.exec:java.lang.Process(java.lang.String)"
    }
  }

  "CPG for code with a call to infix fn `to`" should {
    val cpg = code("""
        |package mypkg
        |
        |fun foo() {
        |  val numbersMap = mapOf("key1" to 1, "key2" to 2)
        |  println(numbersMap)
        |}
        |""".stripMargin)

    "should contain CALL nodes for calls to infix fn `to`" in {
      val List(c1) = cpg.call.code("\"key1.*").l
      c1.methodFullName shouldBe "kotlin.to:kotlin.Pair(java.lang.Object,java.lang.Object)"
      c1.dispatchType shouldBe DispatchTypes.STATIC_DISPATCH

      val List(c2) = cpg.call.code("\"key2.*").l
      c2.methodFullName shouldBe "kotlin.to:kotlin.Pair(java.lang.Object,java.lang.Object)"
    }

    "CPG for code with calls to stdlib's `split`s" should {
      val cpg = code("""
          |package mypkg
          |
          |fun main() {
          |    val foo = "one,two,three".split(",")
          |    println(foo)
          |
          |    val bar = "one,two,three".split(",", "t", ignoreCase = false)
          |    println(bar)
          |}
          |""".stripMargin)

      "should contain CALL nodes for `split` with the correct MFNs set" in {
        inside(cpg.call.methodFullName(".*split.*").l) { case List(call1, call2) =>
          call1.methodFullName shouldBe "kotlin.text.split:java.util.List(java.lang.CharSequence,kotlin.Array,boolean,int)"
          call2.methodFullName shouldBe call1.methodFullName
        }
      }
    }

    "CPG for code with calls to stdlib's `trim`s" should {
      val cpg = code("""
          |package mypkg
          |
          |fun trimParam(p: String): String {
          |    val y = p.trim()
          |    return y
          |}
          |
          |fun main() {
          |    val out = trimParam(" hello ")
          |    println(out)
          |}
          |
          |""".stripMargin)

      "should contain a CALL node for `trim` with the correct props set" in {
        val List(c) = cpg.call.code("p.trim.*").l
        c.methodFullName shouldBe "kotlin.text.trim:java.lang.String(java.lang.String)"
        c.signature shouldBe "java.lang.String(java.lang.String)"
        c.typeFullName shouldBe "java.lang.String"
        c.dispatchType shouldBe DispatchTypes.STATIC_DISPATCH
        c.lineNumber shouldBe Some(5)
        c.columnNumber shouldBe Some(12)
      }

      "should contain a CALL node for `trim` a receiver arg with the correct props set" in {
        val List(receiverArg) = cpg.call.code("p.trim.*").argument.isIdentifier.l
        receiverArg.argumentIndex shouldBe 1
        receiverArg.name shouldBe "p"
        receiverArg.code shouldBe "p"
        receiverArg.typeFullName shouldBe "java.lang.String"
        receiverArg.lineNumber shouldBe Some(5)
        receiverArg.columnNumber shouldBe Some(12)
      }
    }
  }
}
