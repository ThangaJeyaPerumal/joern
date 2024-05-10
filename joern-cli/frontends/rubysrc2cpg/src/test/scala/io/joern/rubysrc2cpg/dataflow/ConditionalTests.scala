package io.joern.rubysrc2cpg.dataflow

import io.joern.dataflowengineoss.language.*
import io.joern.rubysrc2cpg.testfixtures.RubyCode2CpgFixture
import io.shiftleft.semanticcpg.language.*

class ConditionalTests extends RubyCode2CpgFixture(withPostProcessing = true, withDataFlow = true) {

  "flow through both branches of a ternary `.. ? .. : ..` operator" in {
    val cpg = code("""
        |x = 1
        |y = 2
        |z = foo ? x : y
        |puts z
        |""".stripMargin)
    val source = cpg.literal
    val sink   = cpg.method.name("puts").callIn.argument
    val flows  = sink.reachableByFlows(source)
    flows.map(flowToResultPairs).sortBy(_.headOption.map(_._1)).toSet shouldBe
      Set(List(("x = 1", 2), ("x", 4), ("z = foo ? x : y", 4), ("puts z", 5)), List(("y = 2", 3), ("y", 4), ("z = foo ? x : y", 4), ("puts z", 5)))
  }

  // Works in deprecated
  "flow through statement with ternary operator with multiple line" ignore {
    val cpg = code("""
                     |x = 2
                     |y = 3
                     |z = 4
                     |
                     |w = x == 2 ?
                     | y
                     | : z
                     |puts y
                     |""".stripMargin)

    val source = cpg.identifier.name("y").l
    val sink   = cpg.call.name("puts").l
    sink.reachableByFlows(source).size shouldBe 2
  }
}
