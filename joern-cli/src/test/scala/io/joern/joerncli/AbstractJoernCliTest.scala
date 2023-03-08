package io.joern.joerncli

import better.files.File
import io.joern.console.cpgcreation.CCpgGenerator
import io.joern.console.FrontendConfig
import io.joern.jssrc2cpg.JsSrc2Cpg
import io.joern.jssrc2cpg
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.Languages
import io.shiftleft.utils.ProjectRoot

trait AbstractJoernCliTest {

  protected def withTestCpg[T](file: File, language: String = Languages.C)(f: ((Cpg, String)) => T): T = {
    f(loadTestCpg(file, language))
  }

  private def loadTestCpg(file: File, language: String = Languages.C): (Cpg, String) = {
    val tmpFile        = File.newTemporaryFile("cpg", "bin")
    val cpgOutFileName = tmpFile.pathAsString
    tmpFile.delete()

    language match {
      case Languages.C | Languages.CSHARP         => withC2Cpg(file, cpgOutFileName)
      case Languages.JSSRC | Languages.JAVASCRIPT => withJsSrc2Cpg(file, cpgOutFileName)
      case _                                      => ???
    }
    // Link CPG fragments and enhance to create semantic CPG
    val cpg = DefaultOverlays.create(cpgOutFileName)
    (cpg, cpgOutFileName)
  }

  private def withC2Cpg(inputFile: File, outputPath: String): Unit = {
    CCpgGenerator(new FrontendConfig(), ProjectRoot.find.path)
      .generate(inputPath = inputFile.pathAsString, outputPath = outputPath)
  }

  private def withJsSrc2Cpg(inputFile: File, outputPath: String): Unit = {
    val frontend = new JsSrc2Cpg()
    val config   = jssrc2cpg.Config(inputPath = inputFile.pathAsString, outputPath = outputPath)
    frontend.run(config)
  }

}
