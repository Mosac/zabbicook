package com.github.zabbicook.test

import com.github.zabbicook.entity.{Permission, UserGroup}
import com.github.zabbicook.operation.{PermissionsOfHosts, UserGroupConfig, UserGroupOp}

trait TestUserGroups extends TestConfig with TestHostGroups { self: UnitSpec =>
  private[this] lazy val testUserGroupOp = new UserGroupOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testUserGroups = Seq(
    UserGroupConfig(UserGroup(name = specName("group1")), Seq(PermissionsOfHosts(testHostGroups(0).name, Permission.readOnly))),
    UserGroupConfig(UserGroup(name = specName("group2")), Seq(PermissionsOfHosts(testHostGroups(1).name, Permission.readWrite)))
  )

  def presentTestUserGroups(): Unit = {
    presentTestHostGroups()
    await(testUserGroupOp.present(testUserGroups))
  }

  def cleanTestUserGroups(): Unit = {
    await(testUserGroupOp.absent(testUserGroups.map(_.userGroup.name)))
    cleanTestHostGroups()
  }
}
