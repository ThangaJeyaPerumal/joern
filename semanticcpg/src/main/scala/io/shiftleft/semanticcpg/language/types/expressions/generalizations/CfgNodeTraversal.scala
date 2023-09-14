package io.shiftleft.semanticcpg.language.types.expressions.generalizations

import io.shiftleft.codepropertygraph.generated.v2.nodes.*
import io.shiftleft.codepropertygraph.generated.v2.neighboraccessors.Lang.*
import io.shiftleft.semanticcpg.language.*
// TODO bring back @Help
//import overflowdb.traversal.help
//import overflowdb.traversal.help.Doc

// TODO bring back @Help
//@help.Traversal(elementType = classOf[CfgNode])
class CfgNodeTraversal[A <: CfgNode](val traversal: Iterator[A]) extends AnyVal {

  /** Textual representation of CFG node
    */
  // TODO bring back @Doc(info = "Textual representation of CFG node")
  def repr: Iterator[String] =
    traversal.map(_.repr)

  /** Traverse to enclosing method
    */
  def method: Iterator[Method] =
    traversal.map(_.method) // refers to `semanticcpg.language.nodemethods.CfgNodeMethods.method`

  /** Traverse to next expression in CFG.
    */
  // TODO bring back @Doc(info = "Nodes directly reachable via outgoing CFG edges")
  def cfgNext: Iterator[CfgNode] =
    traversal._cfgOut
      .filterNot(_.isInstanceOf[MethodReturn])
      .cast[CfgNode]

  /** Traverse to previous expression in CFG.
    */
  // TODO bring back @Doc(info = "Nodes directly reachable via incoming CFG edges")
  def cfgPrev: Iterator[CfgNode] =
    traversal._cfgIn
      .filterNot(_.isInstanceOf[MethodReturn])
      .cast[CfgNode]

  /** All nodes reachable in the CFG by up to n forward expansions
    */
  def cfgNext(n: Int): Iterator[CfgNode] = traversal.flatMap(_.cfgNext(n))

  /** All nodes reachable in the CFG by up to n backward expansions
    */
  def cfgPrev(n: Int): Iterator[CfgNode] = traversal.flatMap(_.cfgPrev(n))

  /** Recursively determine all nodes on which any of the nodes in this traversal are control dependent
    */
  // TODO bring back @Doc(info = "All nodes on which this node is control dependent")
  def controlledBy: Iterator[CfgNode] =
    traversal.flatMap(_.controlledBy)

  /** Recursively determine all nodes which are control dependent on this node
    */
  // TODO bring back @Doc(info = "All nodes control dependent on this node")
  def controls: Iterator[CfgNode] =
    traversal.flatMap(_.controls)

  /** Recursively determine all nodes by which this node is dominated
    */
  // TODO bring back @Doc(info = "All nodes by which this node is dominated")
  def dominatedBy: Iterator[CfgNode] =
    traversal.flatMap(_.dominatedBy)

  /** Recursively determine all nodes which this node dominates
    */
  // TODO bring back @Doc(info = "All nodes that are dominated by this node")
  def dominates: Iterator[CfgNode] =
    traversal.flatMap(_.dominates)

  /** Recursively determine all nodes by which this node is post dominated
    */
  // TODO bring back @Doc(info = "All nodes by which this node is post dominated")
  def postDominatedBy: Iterator[CfgNode] =
    traversal.flatMap(_.postDominatedBy)

  /** Recursively determine all nodes which this node post dominates
    */
  // TODO bring back @Doc(info = "All nodes that are post dominated by this node")
  def postDominates: Iterator[CfgNode] =
    traversal.flatMap(_.postDominates)

  /** Obtain hexadecimal string representation of lineNumber field.
    */
  // TODO bring back @Doc(info = "Address of the code (for binary code)")
  def address: Iterator[Option[String]] =
    traversal.map(_.address)

  // TODO bring back @Doc(info = "Filters in paths that pass though the given traversal")
  def passes(included: Iterator[CfgNode]): Iterator[CfgNode] = {
    val in = included.toSet
    traversal.flatMap(_.passes(in))
  }

  // TODO bring back @Doc(info = "Filters out paths that pass though the given traversal")
  def passesNot(excluded: Iterator[CfgNode]): Iterator[CfgNode] = {
    val ex = excluded.toSet
    traversal.flatMap(_.passesNot(ex))
  }

}
