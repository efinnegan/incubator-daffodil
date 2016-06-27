package edu.illinois.ncsa.daffodil.processors

import edu.illinois.ncsa.daffodil.dsom._
import edu.illinois.ncsa.daffodil.processors.charset.DFDLCharset
import edu.illinois.ncsa.daffodil.processors.charset.CharsetUtils
import edu.illinois.ncsa.daffodil.exceptions.Assert

/*
 * The way encoding works, is if a EncodingChangeParser or Unparser is
 * added to the processor by the DFDL compiler, then when that processor is evaluated
 * it will invoke these EVs to obtain the right charset decoder and encoder.
 *
 * The encoder or decoder actually being used when characters are processed
 * is stored in the data stream by the change operation so that it is there when needed.
 *
 * Note that this implies for situations where backtracking can occur (parsing) or
 * the outputValueCalc with forward reference (unparsing), that the ChangeEncoding processor
 * must be re-evaluated (in general) if any change has occurred since, so that the
 * encoder/decoder we start using when we back up to the earlier position is the right
 * one.
 *
 * Often whole schemas will use only one encoding however, so the DFDL compiler may
 * optimize out the ChangeEncoding processors except the very first one.
 */

/**
 * Encoding is a string, so there is no converter.
 */
class EncodingEv(expr: CompiledExpression[String], trd: TermRuntimeData)
  extends EvaluatableConvertedExpression[String, String](
    expr,
    EncodingCooker, // cooker insures upper-case and trimmed of whitespace.
    trd)
  with InfosetCachedEvaluatable[String] {
  override lazy val runtimeDependencies = Nil

  override protected def compute(state: ParseOrUnparseState): String = {
    // compute via the cooker first
    val encoding = super.compute(state)

    if (encoding == "UTF-16" || encoding == "UTF-32") {
      // TODO: Use byte order mark (from P/UState or another EV?) to determine
      // if encoding should be BE or LE. Note that this means that if encoding
      // is UTF-16 or UTF-32, then we cannot calculate EncodingEv at compile
      // time since it maybe depend on the a parse value

      // If BOM doesn't exist, then default to BE.
      encoding + "BE"
    } else {
      encoding
    }
  }
}

class CharsetEv(encodingEv: EncodingEv, val trd: TermRuntimeData)
  extends Evaluatable[DFDLCharset](trd)
  with InfosetCachedEvaluatable[DFDLCharset] {

  override lazy val runtimeDependencies = Seq(encodingEv)

  override def compute(state: ParseOrUnparseState) = {
    val encString = encodingEv.evaluate(state)
    val cs = CharsetUtils.getCharset(encString)
    Assert.invariant(cs ne null)
    new DFDLCharset(encString)
  }
}