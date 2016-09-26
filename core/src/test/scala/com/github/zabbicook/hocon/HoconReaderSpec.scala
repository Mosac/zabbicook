package com.github.zabbicook.hocon

import com.github.zabbicook.entity._
import com.github.zabbicook.hocon.HoconReads._
import com.github.zabbicook.test.UnitSpec
import com.typesafe.config.ConfigFactory

class HoconReaderSpec extends UnitSpec {

  sealed abstract class Address(val value: String) extends StringEnumProp {
    override def validate(): ValidationResult = Address.validate(this)
  }

  object Address extends StringEnumCompanion[Address] {
    override val all: Set[Address] = Set(earth,other)
    case object earth extends Address("earth")
    case object other extends Address("universe")
    case object unknown extends Address("unknown")
  }

  sealed abstract class Income(val value: NumProp) extends NumberEnumDescribedWithString {
    override def validate(): ValidationResult = Income.validate(this)
  }

  object Income extends NumberEnumDescribedWithStringCompanion[Income] {
    override val all: Set[Income] = Set(millionaire, poor)
    case object millionaire extends Income(0)
    case object poor extends Income(1)
    case object unknown extends Income(-1)
  }

  case class Person(name: String, age: Option[Int] = None, address: Option[Address] = None, income: Option[Income] = None)

  implicit val personReads: HoconReads[Person] = {
    for {
      name <- required[String]("name")
      age <- optional[Int]("age")
      address <- optional[Address]("address")
      income <- optional[Income]("income")
    } yield {
      Person(name, age, address, income)
    }
  }

  case class World(me: Person, others: Seq[Person], god: Option[Person])

  implicit val worldReads: HoconReads[World] = {
    for {
      me <- required[Person]("me")
      others <- required[Seq[Person]]("others")
      god <- optional[Person]("god")
    } yield {
      World(me, others, god)
    }
  }

  "reader" should "reads hocon to some objects" in {
    val alice = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{ name: "Alice", age: 12 }""")
    )
    val bob = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{name="Bob"}""")
    )
    assert(alice === HoconSuccess(Person("Alice",Some(12))))
    assert(bob === HoconSuccess(Person("Bob",None)))
  }

  "reader" should "reads hocon to some objects includes Array" in {
    val s =
      """
        |{
        |  me: { name: "Alice", age: 10, address: "earth" }
        |  others: [
        |    { name: "Bob", income: "millionaire" }
        |    { name: "Charlie", age: 99 }
        |  ]
        |  god: { name: "Yes", age: 2016, address: "other", income: "poor" }
        |}
      """.stripMargin
    val world = HoconReader.read[World](
      ConfigFactory.parseString(s)
    )
    assert(world === HoconSuccess(World(
      me = Person("Alice", Some(10), Some(Address.earth)),
      others = Seq(Person("Bob", income = Some(Income.millionaire)), Person("Charlie", Some(99))),
      god = Some(Person("Yes", Some(2016), Some(Address.other), Some(Income.poor)))
    )))
  }

  "read" should "fail when types are mismatched" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{ name: "Alice", age: "hoge"}""")
    )
    assert(r.isInstanceOf[HoconError.TypeMismatched])
  }

  "read" should "fail when required props do not exist" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{age: 1}""")
    )
    assert(r.isInstanceOf[HoconError.NotExist])
  }

  "read" should "fail when inputs are invalid format" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{age = 1""")
    )
    assert(r.isInstanceOf[HoconError.ParseFailed])
  }

  "read" should "fail when includes invalid enum string" in {
    val r = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{name: "Alice", address: "not applicative address"}""")
    )
    assert(r.isInstanceOf[HoconError.InvalidConditionProperty])

    val r2 = HoconReader.read[Person](
      ConfigFactory.parseString(s"""{name: "Alice", income: "not applicative address"}""")
    )
    assert(r2.isInstanceOf[HoconError.InvalidConditionProperty])
  }

  "read" should "parse Hocon map to seq[T]" in {
    val reads =
      for {
        users <- requiredMapToSet[Person]("users", "name")
      } yield {
        users
      }

    val actual = reads.read(ConfigFactory.parseString(
      s""" users: {
         |"Alice Alice": { address: "earth" }
         |"Bob Bob" : { address: "other" }
         |}
         |""".stripMargin
    ))

    assert(actual === HoconSuccess(Set(
      Person(name = "Alice Alice", address = Some(Address.earth)),
      Person(name = "Bob Bob", address = Some(Address.other))
    )))
  }
}
