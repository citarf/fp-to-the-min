package fpmin

import fpmin.csv._

import java.time.{Clock=>_,_}
import zio._
import zio.clock._
import zio.duration._

/**
 * Aggregate all the death statistics for a given period of time.
 */
trait StatsAggregator {
  def aggregate(month: Month): UIO[Csv]
}

object StatsAggregator {

  /**
   * This aggregator computes statistics coming from the Covid19 service
   * if they are available
   */
  val live = ZLayer.fromServices[Clock.Service, Covid19, StatsAggregator] {
    (clock: Clock.Service, covid19: Covid19) =>
    
    new StatsAggregator {
      def aggregate(month: Month): UIO[Csv] = {
        val daysInMonth = YearMonth.of(2020, month.getValue()).lengthOfMonth()
        ZIO.foreachPar(1 to daysInMonth) { dayInMonth =>
          getCsv(month, dayInMonth).map(groupByDeaths)
        }.map(_.reduce(_ + _))
      }

      private def getCsv(month: Month, dayInMonth: Int) =
        covid19.load(month.getValue(), dayInMonth).
          retry(Schedule.exponential(1.millis) && Schedule.recurs(100)).
          timeoutFail(new Error)(30.seconds).provide(Has(clock)) orElse ZIO.succeed(Csv.empty)

      private def groupByDeaths(csv: Csv): Csv = {
        val retained = csv.retain("country_region", "confirmed", "deaths", "recovered", "active", "province_state")
        val grouped = retained.groupBy("province_state", "country_region")
        grouped.sortByReverse(grouped("deaths").int)
      }
    }
  }
}
