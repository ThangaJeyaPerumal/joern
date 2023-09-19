package io.shiftleft.semanticcpg.language.nodemethods

import io.shiftleft.codepropertygraph.generated.v2.accessors.Lang.*
import io.shiftleft.codepropertygraph.generated.v2.nodes.CallBase
import io.shiftleft.semanticcpg.language.*

class CallMethods[NodeType <: CallBase](val node: NodeType) extends AnyVal {

  def isStatic: Boolean =
    node.dispatchType == "STATIC_DISPATCH"

  //  def isDynamic: Boolean = {
//    // TODO define as extension method in codegen so we don't need to invoke constructor here
//    new Accessors.Access_Property_DISPATCH_TYPE(node).dispatchType == "DYNAMIC_DISPATCH"
//  }
//
//  def receiver: Iterator[Expression] =
//    node.receiverOut
//
//  def arguments(index: Int): Iterator[Expression] =
//    node._argumentOut
//      .collect {
//        case expr: Expression if expr.argumentIndex == index => expr
//      }
//
//  def argument: Iterator[Expression] =
//    node._argumentOut.collectAll[Expression]
//
//  def argument(index: Int): Expression =
//    arguments(index).head
//
//  def argumentOption(index: Int): Option[Expression] =
//    node._argumentOut.collectFirst {
//      case expr: Expression if expr.argumentIndex == index => expr
//    }
//
//  override def location: NewLocation = {
//    LocationCreator(node, node.code, node.label, node.lineNumber, node.method)
//  }
}
