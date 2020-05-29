package fpmin

import java.io.IOException
import zio._
import zio.blocking._
import zio.clock._
import zio.console._

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    startInterpreter.exitCode

  val startInterpreter: ZIO[Any, IOException, Unit] = {
    val interpreter =
      Blocking.live >+>
      Github.live >+>
      Covid19.live >+>
      Clock.live >+>
      StatsAggregator.live >+>
      Console.live >+>
      StatsInterpreter.live

    ZIO.service[StatsInterpreter].flatMap(_.start).provideLayer(interpreter)
  }
}
