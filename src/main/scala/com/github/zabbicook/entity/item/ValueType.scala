package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.prop._

sealed abstract class ValueType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object ValueType extends IntEnumPropCompanion[ValueType] {
  override val values: Seq[ValueType] = Seq(
    float,character,log,unsigned,text,unknown
  )
  override val description: String = "Type of information of the item."
  case object float extends ValueType(0, "numeric float")
  case object character extends ValueType(1, "character")
  case object log extends ValueType(2, "log")
  case object unsigned extends ValueType(3, "numeric unsigned")
  case object text extends ValueType(4, "text")
  case object unknown extends ValueType(-1, "unknown")
}
