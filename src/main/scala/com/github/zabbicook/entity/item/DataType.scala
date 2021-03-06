package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.prop._

sealed abstract class DataType(val zabbixValue: IntProp, val desc: String) extends EnumProp[IntProp]

object DataType extends IntEnumPropCompanion[DataType] {
  override val values: Seq[DataType] = Seq(
    decimal,octal,hexadecimal,boolean,unknown
  )
  override val description: String = "Data type of the item."
  case object decimal extends DataType(0, "(default) decimal")
  case object octal extends DataType(1, "octal")
  case object hexadecimal extends DataType(2, "hexadecimal")
  case object boolean extends DataType(3, "boolean")
  case object unknown extends DataType(-1, "unknown")
}
