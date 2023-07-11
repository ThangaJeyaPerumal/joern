package io.joern.jimple2cpg

import better.files.File
import io.joern.jimple2cpg.passes.{AstCreationPass, SootAstCreationPass}
import io.joern.jimple2cpg.util.ProgramHandlingUtil.{ClassFile, extractClassesInPackageLayout}
import io.joern.x2cpg.X2Cpg.withNewEmptyCpg
import io.joern.x2cpg.datastructures.Global
import io.joern.x2cpg.passes.frontend.{MetaDataPass, TypeNodePass}
import io.joern.x2cpg.{SourceFiles, X2CpgFrontend}
import io.shiftleft.codepropertygraph.Cpg
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import soot.options.Options
import soot.{G, PackManager, Scene}

import java.nio.file.Paths
import scala.jdk.CollectionConverters.{EnumerationHasAsScala, SeqHasAsJava}
import scala.language.postfixOps
import scala.util.Try

object Jimple2Cpg {
  val language = "JAVA"

  def apply(): Jimple2Cpg = new Jimple2Cpg()
}

class Jimple2Cpg extends X2CpgFrontend[Config] {

  import Jimple2Cpg.*

  private val logger = LoggerFactory.getLogger(classOf[Jimple2Cpg])

  def sootLoadApk(input: File, framework: Option[String] = None): Unit = {
    Options.v().set_process_dir(List(input.canonicalPath).asJava)
    framework match {
      case Some(value) if value.nonEmpty => {
        Options.v().set_src_prec(Options.src_prec_apk)
        Options.v().set_force_android_jar(value)
      }
      case _ => {
        Options.v().set_src_prec(Options.src_prec_apk_c_j)
      }
    }
    Options.v().set_process_multiple_dex(true)
    // workaround for Soot's bug while parsing large apk.
    // see: https://github.com/soot-oss/soot/issues/1256
    Options.v().setPhaseOption("jb", "use-original-names:false")
  }

  def sootLoadClass(inputDir: String): Unit = {
    Options.v().set_process_dir(List(inputDir).asJava)
    Options.v().set_src_prec(Options.src_prec_class)
  }

  /** Load all class files from archives or directories recursively
    * @return
    *   The list of extracted class files and the components of their fully qualified class names
    */
  private def loadClassFiles(src: File, tmpDir: File): List[ClassFile] = {
    val archiveFileExtensions = Set(".jar", ".war", ".zip")
    extractClassesInPackageLayout(
      src,
      tmpDir,
      isClass = e => e.extension.contains(".class"),
      isArchive = e => e.extension.exists(archiveFileExtensions.contains)
    )
  }
  private def sootLoadRecursively(input: File, tmpDir: File, cpg: Cpg, config: Config): List[ClassFile] = {
    Options.v().set_soot_classpath(tmpDir.canonicalPath)
    Options.v().set_prepend_classpath(true)
    val classFiles = loadClassFiles(input, tmpDir)
    val fqcns      = classFiles.flatMap(_.fqcn)
    logger.info(s"Loading ${classFiles.size} program files")
    logger.debug(s"Source files are: ${classFiles.map(_.file.canonicalPath)}")
    fqcns.foreach { fqcn =>
      Scene.v().addBasicClass(fqcn)
      Scene.v().loadClassAndSupport(fqcn)
    }
    classFiles
  }

  private def cpgApplyPasses(cpg: Cpg, config: Config, tmpDir: File): Unit = {
    val input = File(config.inputPath)
    configureSoot(config, tmpDir)
    new MetaDataPass(cpg, language, config.inputPath).createAndApply()

    val globalFromAstCreation: () => Global = input.extension match {
      case Some(".apk" | ".dex") if input.isRegularFile =>
        sootLoadApk(input, config.android)
        { () =>
          val astCreator = SootAstCreationPass(cpg)
          astCreator.createAndApply()
          astCreator.global
        }
      case _ =>
        val classFiles = sootLoadRecursively(input, tmpDir, cpg, config)
        { () =>
          val astCreator = AstCreationPass(classFiles, cpg)
          astCreator.createAndApply()
          astCreator.global
        }
    }

    logger.info("Loading classes to soot")
    Scene.v().loadNecessaryClasses()
    logger.info(s"Loaded ${Scene.v().getApplicationClasses().size()} classes")

    val global = globalFromAstCreation()
    TypeNodePass
      .withRegisteredTypes(global.usedTypes.keys().asScala.toList, cpg)
      .createAndApply()
  }

  def createCpg(config: Config): Try[Cpg] =
    try {
      withNewEmptyCpg(config.outputPath, config: Config) { (cpg, config) =>
        File.temporaryDirectory("jimple2cpg-").apply { tmpDir =>
          cpgApplyPasses(cpg, config, tmpDir)
        }
      }
    } finally {
      G.reset()
    }

  private def configureSoot(config: Config, outDir: File): Unit = {
    // set application mode
    Options.v().set_app(false)
    Options.v().set_whole_program(false)
    // keep debugging info
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
    // ignore library code
    Options.v().set_no_bodies_for_excluded(true)
    Options.v().set_allow_phantom_refs(true)
    // keep variable names
    Options.v().setPhaseOption("jb.sils", "enabled:false")
    Options.v().setPhaseOption("jb", "use-original-names:true")
    // output jimple
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_output_dir(outDir.canonicalPath)

    Options.v().set_dynamic_dir(config.dynamicDirs.asJava)
    Options.v().set_dynamic_package(config.dynamicPkgs.asJava)

    if (config.fullResolver) {
      // full transitive resolution of all references
      Options.v().set_full_resolver(true)
    }
  }
}
