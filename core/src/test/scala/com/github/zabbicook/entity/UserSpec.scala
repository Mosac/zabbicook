package com.github.zabbicook.entity

import com.github.zabbicook.hocon.{HoconReader, HoconSuccess}
import com.github.zabbicook.test.UnitSpec

class UserSpec extends UnitSpec {

  "User" should "be parsed in Hocon format" in {
    val s =
      s"""{
         |alias: "Alice"
         |autoLogin: true
         |autoLogout: 10
         |theme: "dark"
         |type: "user"
         |}""".stripMargin
    val HoconSuccess(actual) = HoconReader.read[User](s)
    assert(actual === User(
      alias = "Alice",
      autologin = Some(EnabledEnum.enabled),
      autologout = Some(NumProp(10)),
      theme = Some(Theme.dark),
      `type` = Some(UserType.user)
    ))
  }
}