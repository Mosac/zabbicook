package com.github.zabbicook.operation

import com.github.zabbicook.Logging
import com.github.zabbicook.api.ZabbixApi
import com.github.zabbicook.entity.User._
import com.github.zabbicook.entity.UserGroup.UserGroupId
import com.github.zabbicook.entity.{Permission, UserGroup, UserGroupPermission}
import com.github.zabbicook.hocon.HoconReads
import com.github.zabbicook.hocon.HoconReads._
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * @see https://www.zabbix.com/documentation/3.2/manual/api/reference/usergroup
  */
class UserGroupOp(api: ZabbixApi) extends OperationHelper with Logging {
  private[this] val hostGroupOp = new HostGroupOp(api)
  private[this] val logger = defaultLogger

  def findByName(name: String): Future[Option[(UserGroup, Seq[UserGroupPermission])]] = {
    val param = Json.obj()
      .filter("name" -> name)
      .prop("selectRights" -> "extend")
      .outExtend()
    api.requestSingle("usergroup.get", param).map {
      _.map {
        case root: JsObject =>
          try {
            val userGroup = Json.fromJson[UserGroup](root).get
            val rights = Json.fromJson[Seq[UserGroupPermission]]((root \ "rights").get).get
            (userGroup, rights)
          } catch {
            case NonFatal(e) =>
              logger.error(s"usergroup.get returns: ${Json.prettyPrint(root)}", e)
              sys.error(s"usergroup.get($name) returns an invalid object.")
          }
        case els =>
          logger.error(s"usergroup.get returns: ${Json.prettyPrint(els)}")
          sys.error(s"usergroup.get($name) returns a non object.")
      }
    }
  }

  def findByNames(names: Seq[String]): Future[Seq[(UserGroup, Seq[UserGroupPermission])]] = {
    Future.traverse(names)(name => findByName(name)).map(_.flatten)
  }

  /**
    * If any one of names does not exist, throw an Exception
    */
  def findByNamesAbsolutely(names: Seq[String]): Future[Seq[(UserGroup, Seq[UserGroupPermission])]] = {
    findByNames(names).map { results =>
      if (results.length < names.length) {
        val notFounds = (names.toSet -- results.map(_._1.name).toSet).mkString(",")
        throw NoSuchEntityException(s"No such user groups are not found: ${notFounds}")
      }
      results
    }
  }

  /**
    * @param group user group
    * @param rights permissions to assign to the group
    * @return user group ID of generated group
    *         throw an Exception if group already exists
    */
  def create(
    group: UserGroup,
    rights: Seq[UserGroupPermission]
  ): Future[(UserGroupId, Report)] = {
    val param = Json.toJson(group.removeReadOnly).as[JsObject]
        .prop("rights" -> rights)
    api.requestSingleId[UserGroupId]("usergroup.create", param, "usrgrpids")
      .map((_, Report.created(group)))
  }

  def update(
    group: UserGroup,
    rights: Seq[UserGroupPermission]
  ): Future[(UserGroupId, Report)] = {
    val param = Json.toJson(group).as[JsObject]
      .prop("rights" -> rights)
    api.requestSingleId[UserGroupId]("usergroup.update", param, "usrgrpids")
      .map((_, Report.updated(group)))
  }

  def delete(groups: Seq[UserGroup]): Future[(Seq[UserId], Report)] = {
    if (groups.isEmpty) {
      Future.successful((Seq(), Report.empty()))
    } else {
      val param = Json.toJson(groups.map(_.usrgrpid.getOrElse(sys.error(s"usergroup.delete requires ids"))))
      api.requestIds[UserGroupId]("usergroup.delete", param, "usrgrpids")
        .map((_, Report.deleted(groups)))
    }
  }

  /**
    * keep the status of the user group to be constant.
    * If the user group with the specified name does not exist, create it.
    * If already exists , it fills the gap.
    * @return User group id and operation state
    */
  def present(userGroup: UserGroupConfig): Future[(UserGroupId, Report)] = {
    // convert to UserGroupPermission object
    val permissionsFut = hostGroupOp.findByNamesAbsolutely(userGroup.permissionsOfHosts.keys.toSeq).map {
      _.map { group =>
        val id = group.groupid.getOrElse(sys.error(s"HostGroupOp.findByName(${group.name}) returns no host group id"))
        val permission = userGroup.permissionsOfHosts(group.name)
        UserGroupPermission(id, permission)
      }
    }

    for {
      permissions <- permissionsFut
      storedOpt <- findByName(userGroup.userGroup.name)
      result <- storedOpt match {
        case Some((storedUserGroup, storedPermissions)) =>
          val id = storedUserGroup.usrgrpid.getOrElse(sys.error("UserGroup.findByName returns no id"))
          if (
            storedUserGroup.shouldBeUpdated(userGroup.userGroup) ||
            storedPermissions.toSet != permissions.toSet
          ) {
            update(userGroup.userGroup.copy(usrgrpid = Some(id)), permissions)
          } else {
            Future.successful((id, Report.empty()))
          }
        case None =>
          create(userGroup.userGroup, permissions)
      }
    } yield result
  }

  def present(groups: Seq[UserGroupConfig]): Future[(Seq[UserGroupId], Report)] = {
    traverseOperations(groups)(present)
  }

  /**
    * @param groupNames names of gropus to be deleted
    */
  def absent(groupNames: Seq[String]): Future[(Seq[UserGroupId], Report)] = {
    for {
      r <- findByNames(groupNames)
      ids <- delete(r.map(_._1))
    } yield ids
  }
}

/**
  * @param userGroup User group
  * @param permissionsOfHosts Pairs of name of a host group and a permission which describes the access level to the host.
  *                           The specified host groups must be presented before calling present() function.
  */
case class UserGroupConfig(userGroup: UserGroup, permissionsOfHosts: Map[String, Permission])

object UserGroupConfig {
  implicit val hoconReads: HoconReads[UserGroupConfig] = {
    for {
      userGroup <- of[UserGroup]
      permission <- required[Map[String, Permission]]("permission")
    } yield {
      UserGroupConfig(userGroup, permission)
    }
  }
}