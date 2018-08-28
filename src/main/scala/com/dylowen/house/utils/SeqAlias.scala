package com.dylowen.house.utils

/**
  * Alias Seq as immutable and include it by chaining package clauses:
  *
  * @example
  * {{{
  *   package object spf extends SeqAlias
  * }}}
  * {{{
  *   package com.workday.spf
  *   package subpackage
  * }}}
  * @author dylan.owen
  * @since Aug-2018
  */
trait SeqAlias {
  type Seq[+A] = scala.collection.immutable.Seq[A]

  val Seq: scala.collection.immutable.Seq.type = scala.collection.immutable.Seq
}
