package com.github.zabbicook.cli

import com.github.zabbicook.cli.RunResult.{ArgumentError, RunSuccess}
import com.github.zabbicook.doc.MetaDocGen
import com.github.zabbicook.recipe.Recipe
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Main(printer: Printer) {
  def run(args: Array[String]): Future[Int] = {
    Arguments.parser.parse(args, Arguments()) match {
      case Some(conf) if conf.showVersion =>
        printer.out(BuildInfo.version)
        Future(0)

      case Some(conf) if conf.showDoc =>
        MetaDocGen.pathOf(conf.docRoot, Recipe.required(""), conf.docDepth) match {
          case Success(sb) =>
            printer.out(sb.toString())
            Future(0)
          case Failure(e) =>
            printer.error("Failed." + System.lineSeparator() + e.getMessage)
            Future(1)
        }

      case Some(conf) if conf.setPassword =>
        conf.newPassword match {
          case Some(newPass) =>
            val runner = new Runner(conf)
            runner.changePassword(conf.user, newPass, conf.pass).map(result(_, conf.isJson))
          case None =>
            resultFuture(ArgumentError("Failed in parsing arguments. Changing password requires --user, --pass, and --new-pass options."), conf.isJson)
        }

      case Some(conf) =>
        // zabbicook main sequence.
        val runner = new Runner(conf)
        runner.run().map(result(_, conf.isJson))

      case None =>
        resultFuture(ArgumentError("Failed in parsing arguments."), false)
    }
  }

  private[this] def result(r: RunResult, isJson: Boolean): Int = {
    outFormatted(r, isJson)
    if (r.isSuccess) 0 else 1
  }

  private[this] def resultFuture(r: RunResult, isJson: Boolean): Future[Int] = {
    val code = result(r, isJson)
    Future(code)
  }

  private[this] def outFormatted(res: RunResult, isJson: Boolean): Unit = {
    if (isJson) {
      printer.out(Json.prettyPrint(Json.toJson(res)))
    } else {
      val msg = res.asString
      res match {
        case _: RunSuccess => printer.out(msg)
        case _ => printer.error(msg)
      }
    }
  }

}

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      Arguments.parser.showUsage()
    } else {
      val f = new Main(Printer.default).run(args).map(rc => sys.exit(rc))
      Await.result(f, Duration.Inf)
    }
  }
}

