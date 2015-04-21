package edu.illinois.ncsa.daffodil.grammar

import edu.illinois.ncsa.daffodil.dsom.SchemaComponent
import edu.illinois.ncsa.daffodil.util.Compile
import edu.illinois.ncsa.daffodil.compiler.ParserOrUnparser
import edu.illinois.ncsa.daffodil.processors.NadaParser
import edu.illinois.ncsa.daffodil.processors.NadaUnparser
import edu.illinois.ncsa.daffodil.compiler.ForUnparser
import edu.illinois.ncsa.daffodil.compiler.ForParser
import edu.illinois.ncsa.daffodil.compiler.BothParserAndUnparser
import edu.illinois.ncsa.daffodil.util.Debug

/**
 * Prod or Grammar Production
 *
 * Note the call by name on the GramArg. We don't evaluate the GramArg at all unless the guard is true.
 *
 * Guards are used so we can have grammars that include all possibilities,
 * but where examining the format properties specifically would indicate that some of those
 * possibilities are precluded. The guard causes that term to just splice itself out
 * of the grammar.
 *
 * Note that it is crucial that the guardArg is passed by value, and the gramArg is
 * passed by name.
 *
 * Prod objects are not required. They essentially provide some useful debug capability
 * because a grammar term object will display as it's name, not as some anonymous object.
 */
final class Prod(nameArg: String, val sc: SchemaComponent, guard: Boolean, gramArg: => Gram, override val forWhat: ParserOrUnparser)
  extends NamedGram(sc) {

  final override def deref = gram

  def SDE(str: String, args: Any*): Nothing = sc.SDE(str, args)

  final override def name = nameArg

  final override lazy val path = sc.path + "@@Prod(" + prettyName + ")"

  final override def gram: Gram = LV('gram) {
    guard match {
      case true => {
        val g = gramArg // exactly once.
        g match {
          case p: Prod => {
            p.gram // recursively force this
          }
          case _ => //ok
        }
        g
      }
      case false => {
        log(Debug("Prod %s removed.", name))
        EmptyGram
      }
    }
  }.value

  final override def isEmpty = gram.isEmpty

  final override lazy val parser = LV('parser) {
    forWhat match {
      case ForUnparser => new NadaParser(context.runtimeData) // TODO: detect this and remove from final parser
      case _ => gram.parser
    }
  }.value

  final override lazy val unparser = LV('unparser) {
    forWhat match {
      case ForParser => new NadaUnparser(context.runtimeData) // TODO: detect this and remove from final unparser
      case _ => gram.unparser
    }
  }.value
}

