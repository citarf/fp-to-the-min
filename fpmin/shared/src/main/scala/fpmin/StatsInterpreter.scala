package fpmin

import fpmin.csv._

import java.time.{Clock=>_,_}
import java.io.IOException
import zio._
import zio.clock._
import zio.console._
import zio.duration._

/**
 * Main loop for interacting with a user
 */
trait StatsInterpreter {
   def start: IO[IOException, Unit]
}

/**
 * Fetch aggregated statistics for APRIL and display them to the console
 */
case class LiveStatsInterpreter(clock: Clock.Service, console: Console.Service, aggregator: StatsAggregator) extends StatsInterpreter {
  import console._

  def start: IO[IOException, Unit] = {
    val forApril = aggregateAndSummarize(Month.APRIL)
    forApril.repeat(Schedule.spaced(1.minute).provide(Has(clock))).fork *>
    getStrLn.unit
  }

  private def aggregateAndSummarize(month: Month): UIO[Unit] =
    aggregator.aggregate(month).flatMap(printSummary) *>
    putStrLn("Press [Enter] to stop downloading...")

  private def printSummary(csv: Csv) =
    putStrLn("-- SUMMARY --") *>
    putStrLn(csv.truncate(10).toString())

}

object StatsInterpreter {
  val live =
    ZLayer.fromServices[Clock.Service, Console.Service, StatsAggregator, StatsInterpreter](LiveStatsInterpreter.apply)
}
