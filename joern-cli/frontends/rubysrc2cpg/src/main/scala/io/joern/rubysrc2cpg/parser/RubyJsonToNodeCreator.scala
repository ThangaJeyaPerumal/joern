package io.joern.rubysrc2cpg.parser

import io.joern.rubysrc2cpg.astcreation.RubyIntermediateAst.*
import io.joern.rubysrc2cpg.parser.RubyJsonHelpers.*
import io.joern.rubysrc2cpg.passes.Defines
import io.joern.rubysrc2cpg.passes.Defines.getBuiltInType
import io.joern.rubysrc2cpg.utils.FreshNameGenerator
import org.slf4j.LoggerFactory
import ujson.{Obj, Value}

class RubyJsonToNodeCreator(
  variableNameGen: FreshNameGenerator[String] = FreshNameGenerator(id => s"<tmp-$id>"),
  procParamGen: FreshNameGenerator[Left[String, Nothing]] = FreshNameGenerator(id => Left(s"<proc-param-$id>"))
) {

  private val logger       = LoggerFactory.getLogger(getClass)
  private val classNameGen = FreshNameGenerator(id => s"<anon-class-$id>")

  private implicit val implVisit: ujson.Value => RubyExpression = (x: ujson.Value) => visit(x)

  protected def freshClassName(span: TextSpan): SimpleIdentifier = {
    SimpleIdentifier(None)(span.spanStart(classNameGen.fresh))
  }

  private def defaultTextSpan(code: String = ""): TextSpan = TextSpan(None, None, None, None, None, code)

  private def defaultResult: RubyExpression = Unknown()(defaultTextSpan())

  private def visit(v: ujson.Value): RubyExpression = {
    v match {
      case obj: ujson.Obj => visit(obj)
      case ujson.Null     => defaultResult
      case ujson.Str(x)   => StaticLiteral(getBuiltInType(Defines.String))(defaultTextSpan(x))
      case x =>
        logger.warn(s"Unhandled ujson type ${x.getClass}")
        defaultResult
    }
  }

  /** Main entrypoint of JSON deserialization.
    */
  def visitProgram(obj: ujson.Value): StatementList = {
    visit(obj.obj) match {
      case x: StatementList => x
      case x                => StatementList(x :: Nil)(x.span)
    }
  }

  private def visit(obj: ujson.Obj): RubyExpression = {
    val astTypeStr = obj(ParserKeys.Type).str
    AstType.fromString(astTypeStr) match {
      case Some(AstType.Array)               => visitArray(obj)
      case Some(AstType.Begin)               => visitBegin(obj)
      case Some(AstType.TopLevelConstant)    => visitTopLevelConstant(obj)
      case Some(AstType.ClassDefinition)     => visitClassDefinition(obj)
      case Some(AstType.ScopedConstant)      => visitScopedConstant(obj)
      case Some(AstType.MethodDefinition)    => visitMethodDefinition(obj)
      case Some(AstType.DynamicString)       => visitDynamicString(obj)
      case Some(AstType.DynamicSymbol)       => visitDynamicSymbol(obj)
      case Some(AstType.Hash)                => visitHash(obj)
      case Some(AstType.Int)                 => visitInt(obj)
      case Some(AstType.InstanceVariable)    => visitInstanceVariable(obj)
      case Some(AstType.LocalVariableAssign) => visitLocalVariableAssign(obj)
      case Some(AstType.Pair)                => visitPair(obj)
      case Some(AstType.Send)                => visitSend(obj)
      case Some(AstType.Splat)               => visitSplat(obj)
      case Some(AstType.StaticString)        => visitStaticString(obj)
      case Some(AstType.StaticSymbol)        => visitStaticSymbol(obj)
      case _ =>
        logger.warn(s"Unhandled `parser` type '$astTypeStr'")
        defaultResult
    }
  }

  private def visitArray(obj: Obj): RubyExpression = ArrayLiteral(obj.visitArray(ParserKeys.Children))(obj.toTextSpan)

  private def visitBegin(obj: Obj): RubyExpression = StatementList(obj.visitArray(ParserKeys.Body))(obj.toTextSpan)

  private def visitTopLevelConstant(obj: Obj): RubyExpression = {
    val identifier = obj(ParserKeys.Name).str
    SimpleIdentifier()(obj.toTextSpan.spanStart(identifier))
  }

  private def visitClassDefinition(obj: Obj): RubyExpression = {
    val name      = visit(obj(ParserKeys.Name))
    val baseClass = if obj.contains(ParserKeys.SuperClass) then Option(visit(obj(ParserKeys.SuperClass))) else None
    val body      = visit(obj(ParserKeys.Body))
    // TODO: Handle rest of the fields
    ClassDeclaration(name, baseClass, body, fields = Nil, bodyMemberCall = None, namespaceParts = None)(obj.toTextSpan)
  }

  private def visitScopedConstant(obj: Obj): RubyExpression = {
    val identifier = obj(ParserKeys.Name).str
    if (obj.contains(ParserKeys.Base)) {
      val target = visit(obj(ParserKeys.Base))
      MemberAccess(target, ".", identifier)(obj.toTextSpan)
    } else {
      SimpleIdentifier()(obj.toTextSpan)
    }
  }

  private def visitMethodDefinition(obj: Obj): RubyExpression = {
    val name       = obj(ParserKeys.Name).str
    val parameters = obj(ParserKeys.Arguments).asInstanceOf[ujson.Obj].visitArray(ParserKeys.Children)
    val body = if (obj.contains(ParserKeys.Body)) visit(obj(ParserKeys.Body)) else StatementList(Nil)(obj.toTextSpan)
    MethodDeclaration(name, parameters, body)(obj.toTextSpan)
  }

  private def visitDynamicString(obj: Obj): RubyExpression = {
    val typeFullName = getBuiltInType(Defines.String)
    val expressions  = obj.visitArray(ParserKeys.Children)
    DynamicLiteral(typeFullName, expressions)(obj.toTextSpan)
  }

  private def visitDynamicSymbol(obj: Obj): RubyExpression = {
    val typeFullName = getBuiltInType(Defines.Symbol)
    val expressions  = obj.visitArray(ParserKeys.Children)
    DynamicLiteral(typeFullName, expressions)(obj.toTextSpan)
  }

  private def visitHash(obj: Obj): RubyExpression = HashLiteral(obj.visitArray(ParserKeys.Children))(obj.toTextSpan)

  private def visitInt(obj: Obj): RubyExpression = {
    val typeFullName = getBuiltInType(Defines.Integer)
    StaticLiteral(typeFullName)(obj.toTextSpan)
  }

  private def visitInstanceVariable(obj: Obj): RubyExpression = SimpleIdentifier()(obj.toTextSpan)

  private def visitLocalVariableAssign(obj: Obj): RubyExpression = {
    val lhs = visit(obj(ParserKeys.Lhs))
    val rhs = visit(obj(ParserKeys.Rhs))
    SingleAssignment(lhs, "=", rhs)(obj.toTextSpan)
  }

  private def visitPair(obj: Obj): RubyExpression = defaultResult

  private def visitSend(obj: Obj): RubyExpression = {
    val callName  = obj(ParserKeys.Name).str
    val target    = SimpleIdentifier()(obj.toTextSpan.spanStart(callName))
    val arguments = obj.visitArray(ParserKeys.Arguments)
    if (obj.contains(ParserKeys.Receiver)) {
      val base = visit(obj(ParserKeys.Receiver))
      MemberCall(base, ".", callName, arguments)(obj.toTextSpan)
    } else {
      SimpleCall(target, arguments)(obj.toTextSpan)
    }
  }

  private def visitSplat(obj: Obj): RubyExpression = SplattingRubyNode(visit(obj(ParserKeys.Value)))(obj.toTextSpan)

  private def visitStaticString(obj: Obj): RubyExpression = {
    val typeFullName = getBuiltInType(Defines.String)
    StaticLiteral(typeFullName)(obj.toTextSpan)
  }

  private def visitStaticSymbol(obj: Obj): RubyExpression = {
    val typeFullName = getBuiltInType(Defines.Symbol)
    StaticLiteral(typeFullName)(obj.toTextSpan)
  }

}
