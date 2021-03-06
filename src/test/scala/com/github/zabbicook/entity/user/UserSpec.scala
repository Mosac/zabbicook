package com.github.zabbicook.entity.user

import com.github.zabbicook.entity.Entity.NotStored
import com.github.zabbicook.entity.prop.EnabledEnum
import com.github.zabbicook.hocon._
import com.github.zabbicook.test.UnitSpec
import HoconReadsCompanion._
import HoconReads.option

class UserSpec extends UnitSpec {

  "User" should "be parsed in Hocon format" in {
    val s =
      s"""{
         |alias: "Alice"
         |autoLogin: true
         |theme: "dark"
         |type: "user"
         |}""".stripMargin
    val HoconSuccess(actual) = HoconReader.read[User[NotStored]](s, User.optional("root"))
    assert(User[NotStored](
      alias = "Alice",
      autologin = Some(EnabledEnum.enabled),
      theme = Some(Theme.dark),
      `type` = Some(UserType.user)
    ) === actual)
  }
}
