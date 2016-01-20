package edu.illinois.ncsa.daffodil.processors

import edu.illinois.ncsa.daffodil.processors.dfa.CreateFieldDFA
import edu.illinois.ncsa.daffodil.processors.dfa.DFADelimiter
import edu.illinois.ncsa.daffodil.processors.dfa.CreateDelimiterDFA
import edu.illinois.ncsa.daffodil.util.Maybe
import edu.illinois.ncsa.daffodil.util.Maybe._
import edu.illinois.ncsa.daffodil.schema.annotation.props.gen.GenerateEscape
import edu.illinois.ncsa.daffodil.util.MaybeChar

sealed abstract class EscapeSchemeParserHelper
case class EscapeSchemeCharParserHelper(private val escChar: Maybe[String], private val escEscChar: Maybe[String])
  extends EscapeSchemeParserHelper {
  val ec: MaybeChar = if (escChar.isDefined) MaybeChar(escChar.get.charAt(0)) else MaybeChar.Nope
  val eec: MaybeChar = if (escEscChar.isDefined) MaybeChar(escEscChar.get.charAt(0)) else MaybeChar.Nope

  override def toString() = "<EscapeSchemeChar escapeChar='" + escChar.getOrElse("") +
    "' escapeEscapeChar='" + escEscChar.getOrElse("") + "'/>"
}
case class EscapeSchemeBlockParserHelper(private val escEscChar: Maybe[String],
  blockStart: String,
  blockEnd: String)
  extends EscapeSchemeParserHelper {
  // Should note there that fieldDFA (not here) is dependent on
  // the whether or not the delimiters are constant or not.
  // As a result, it cannot be generated here.

  val eec: MaybeChar = if (escEscChar.isDefined) MaybeChar(escEscChar.get.charAt(0)) else MaybeChar.Nope
  val blockStartDFA: DFADelimiter = CreateDelimiterDFA(blockStart)
  val blockEndDFA: DFADelimiter = CreateDelimiterDFA(blockEnd)
  val fieldEscDFA = CreateFieldDFA(blockEndDFA, eec)

  override def toString() = "<EscapeSchemeBlock escapeEscapeChar='" + escEscChar.getOrElse("") +
    "' blockStart='" + blockStart + "' blockEnd='" + blockEnd + "'/>"
}

sealed abstract class EscapeSchemeUnparserHelper {
  def lookingFor: Array[DFADelimiter]
}
case class EscapeSchemeCharUnparserHelper(private val escChar: Maybe[String], private val escEscChar: Maybe[String], private val extraEscChar: Maybe[Seq[String]])
  extends EscapeSchemeUnparserHelper {

  // For unparsing, we need to have the characters to insert for escaping
  // the escapeCharacter and extraEscapedCharacters if they are found.
  //
  val ec: MaybeChar = if (escChar.isDefined) MaybeChar(escChar.get.charAt(0)) else MaybeChar.Nope
  val eec: MaybeChar = if (escEscChar.isDefined) MaybeChar(escEscChar.get.charAt(0)) else MaybeChar.Nope

  // We need to look for the escapeCharacter and the extraEscapedCharacters
  //
  val escCharDFA: DFADelimiter = CreateDelimiterDFA(escChar.get)
  val escEscCharDFA: Maybe[DFADelimiter] = if (escEscChar.isDefined) One(CreateDelimiterDFA(escEscChar.get)) else Nope
  val extraEscCharsDFAs: Array[DFADelimiter] = CreateDelimiterDFA(extraEscChar.getOrElse(Seq.empty))

  override val lookingFor = {
    val res: Array[DFADelimiter] =
      if (escEscCharDFA.isDefined) escCharDFA +: escCharDFA +: escEscCharDFA.get +: escEscCharDFA.get +: extraEscCharsDFAs
      else escCharDFA +: escCharDFA +: extraEscCharsDFAs
    res
  }

  override def toString() = "<EscapeSchemeChar escapeChar='" + escChar.getOrElse("") +
    "' escapeEscapeChar='" + escEscChar.getOrElse("") + "' extraEscapedChars='" + 
    (
        if (extraEscChar.isEmpty) Nope else One(extraEscChar.value.mkString(" ") )
        ) + "'/>"
}
case class EscapeSchemeBlockUnparserHelper(private val escEscChar: Maybe[String],
  blockStart: String,
  blockEnd: String,
  private val extraEscChar: Maybe[Seq[String]],
  generateEscapeBlock: GenerateEscape)
  extends EscapeSchemeUnparserHelper {

  // For unparsing we need to have the character to insert for escaping
  // the blockEnd if it's found.
  //
  val eec: MaybeChar = if (escEscChar.isDefined) MaybeChar(escEscChar.get.charAt(0)) else MaybeChar.Nope

  // We need to look for the blockEnd
  //
  val blockEndDFA: DFADelimiter = CreateDelimiterDFA(blockEnd)
  val blockStartDFA: DFADelimiter = CreateDelimiterDFA(blockStart)
  val fieldEscDFA = CreateFieldDFA(blockEndDFA, eec)
  val extraEscCharsDFAs: Array[DFADelimiter] = CreateDelimiterDFA(extraEscChar.getOrElse(Seq.empty))

  override val lookingFor = blockStartDFA +: extraEscCharsDFAs

  override def toString() = "<EscapeSchemeBlock escapeEscapeChar='" + escEscChar.getOrElse("") +
    "' blockStart='" + blockStart + "' blockEnd='" + blockEnd + "' generateEscapeBlock='" + generateEscapeBlock + "'/>"
}