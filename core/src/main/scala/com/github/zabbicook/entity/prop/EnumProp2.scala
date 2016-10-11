package com.github.zabbicook.entity.prop

import play.api.libs.json._

trait EnumProp2[V] {
  def zabbixValue: V
  def desc: String
}

trait EnumProp2Companion[V, T <: EnumProp2[V]] {
  def values: Set[T]

  def description: String

  def unknown: T

  def possibleValues: Set[T] = values - unknown

  def meta(name: String)(aliases: String*): EnumMeta =
    Meta.enum(name, possibleValues)(aliases:_*)(description)

  def metaWithDesc(name: String)(aliases: String*)(overrideDescription: String): EnumMeta =
    Meta.enum(name, possibleValues)(aliases:_*)(overrideDescription)
}

trait StringEnumProp2Companion[T <: EnumProp2[String]] extends EnumProp2Companion[String, T] {
  implicit val format: Format[T] = Format(
    Reads.StringReads.map(n => possibleValues.find(_.zabbixValue == n).getOrElse(unknown)),
    Writes(v => JsString(v.zabbixValue))
  )
}

trait IntEnumProp2Companion[T <: EnumProp2[IntProp]] extends EnumProp2Companion[IntProp, T] {
  implicit val format: Format[T] = Format(
    implicitly[Reads[IntProp]].map(n => possibleValues.find(_.zabbixValue.value == n.value).getOrElse(unknown)),
    Writes(v => JsNumber(v.zabbixValue.value))
  )
}

/**
  * Zabbix represents two patterns of 'enabled' flag... Be careful!
  */
sealed abstract class EnabledEnum(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

object EnabledEnum extends IntEnumProp2Companion[EnabledEnum] {
  override val values: Set[EnabledEnum] = Set(`false`, `true`, unknown)
  override val description: String = "Enabled status"
  case object `false` extends EnabledEnum(0, "Disable")
  case object `true` extends EnabledEnum(1, "Enable")
  case object unknown extends EnabledEnum(-1, "Unknown")
  val enabled = `true`
  val disabled = `false`

  implicit def boolean2enum(b: Boolean): EnabledEnum = if (b) enabled else disabled
  implicit def boolean2enum(b: Option[Boolean]): Option[EnabledEnum] = b.map(boolean2enum)
}

sealed abstract class EnabledEnumZeroPositive(val zabbixValue: IntProp, val desc: String) extends EnumProp2[IntProp]

object EnabledEnumZeroPositive extends IntEnumProp2Companion[EnabledEnumZeroPositive] {
  override val values: Set[EnabledEnumZeroPositive] = Set(`true`, `false`, unknown)
  override val description: String = "Enabled status"
  case object `true` extends EnabledEnumZeroPositive(0, "Enabled")
  case object `false` extends EnabledEnumZeroPositive(1, "Disabled")
  case object unknown extends EnabledEnumZeroPositive(-1, "Unknown")
  val enabled = `true`
  val disabled = `false`

  implicit def boolean2enum(b: Boolean): EnabledEnumZeroPositive = if (b) enabled else disabled
  implicit def boolean2enum(b: Option[Boolean]): Option[EnabledEnumZeroPositive] = b.map(boolean2enum)
}
