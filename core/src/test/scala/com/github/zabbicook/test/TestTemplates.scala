package com.github.zabbicook.test

import com.github.zabbicook.entity.HostGroup.HostGroupId
import com.github.zabbicook.entity.Template
import com.github.zabbicook.entity.Template.TemplateId
import com.github.zabbicook.operation.{TemplateOp, TemplateSettings}

trait TestTemplates extends TestHostGroups { self: UnitSpec =>

  private[this] lazy val templateOp = new TemplateOp(cachedApi)

  /**
    * you can override to customize generated users.
    */
  protected[this] val testTemplates: Seq[TemplateSettings] = Seq(
    TemplateSettings(Template(host = specName("template1")), Seq(testHostGroups(0)), None),
    TemplateSettings(Template(host = specName("template2")), Seq(testHostGroups(1)),
      Some(Seq(Template(host = specName("template1")), Template(host = "Template OS Linux"))))
  )

  def presentTestTemplates(): (Seq[TemplateId], Seq[HostGroupId]) = {
    val hostGroupIds = presentTestHostGroups()
    val templates = await(templateOp.presentTemplates(testTemplates))
    (templates._1, hostGroupIds)
  }

  def cleanTestTemplates(): Unit = {
    await(templateOp.absentTemplates(testTemplates.map(_.template.host)))
    cleanTestHostGroups()
  }
}