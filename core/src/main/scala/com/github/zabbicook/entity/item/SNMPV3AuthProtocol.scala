package com.github.zabbicook.entity.item

import com.github.zabbicook.entity.{NumProp, NumberEnumDescribedWithString, NumberEnumDescribedWithStringCompanion, ValidationResult}

sealed abstract class SNMPV3AuthProtocol(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = SNMPV3AuthProtocol.validate(this)
}

object SNMPV3AuthProtocol extends NumberEnumDescribedWithStringCompanion[SNMPV3AuthProtocol] {
  override val all: Set[SNMPV3AuthProtocol] = Set(
    MD5,SHA,unknown
  )
  case object MD5 extends SNMPV3AuthProtocol(0)
  case object SHA extends SNMPV3AuthProtocol(1)
  case object unknown extends SNMPV3AuthProtocol(-1)
}

sealed abstract class SNMPV3PrivProtocol(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = SNMPV3PrivProtocol.validate(this)
}

object SNMPV3PrivProtocol extends NumberEnumDescribedWithStringCompanion[SNMPV3PrivProtocol] {
  override val all: Set[SNMPV3PrivProtocol] = Set(
    DES,AES,unknown
  )
  case object DES extends SNMPV3PrivProtocol(0)
  case object AES extends SNMPV3PrivProtocol(1)
  case object unknown extends SNMPV3PrivProtocol(-1)
}

sealed abstract class SNMPV3SecurityLevel(val value: NumProp) extends NumberEnumDescribedWithString {
  override def validate(): ValidationResult = SNMPV3SecurityLevel.validate(this)
}

object SNMPV3SecurityLevel extends NumberEnumDescribedWithStringCompanion[SNMPV3SecurityLevel] {
  override val all: Set[SNMPV3SecurityLevel] = Set(
    noAuthNoPriv,authNoPriv,authPriv,unknown
  )
  case object noAuthNoPriv extends SNMPV3SecurityLevel(0)
  case object authNoPriv extends SNMPV3SecurityLevel(1)
  case object authPriv extends SNMPV3SecurityLevel(2)
  case object unknown extends SNMPV3SecurityLevel(-1)
}