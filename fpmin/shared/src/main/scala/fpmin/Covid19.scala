package fpmin

import fpmin.csv._

import java.io.IOException
import zio._
import Covid19._

/**
 * The Covid19 service provides access to standardized data sets.
 */
trait Covid19 {
  def load(day: Int, month: Int, region: Region = Region.Global, year: Int = 2020): ZIO[Any, IOException, Csv]
}

/**
 * A production implementation of the Covid19 service that depends on a Github service.
 */
case class LiveCovid19(github: Github) extends Covid19 {

  def load(day: Int, month: Int, region: Region = Region.Global, year: Int = 2020): ZIO[Any, IOException, Csv] =
    github.download(Slug, formFullPath(day, month, region, year)).map(Csv.fromString)

  private def formFullPath(day: Int, month: Int, region: Region, year: Int): String = {
    def pad(int: Int): String = (if (int < 10) "0" else "") + int.toString
    s"csse_covid_19_data/csse_covid_19_daily_reports${region.suffix}/${pad(day)}-${pad(month)}-${year}.csv"
  }

}


object Covid19 {

  val Slug: String =
    "CSSEGISandData/COVID-19"

  val live =
    ZLayer.fromService[Github, Covid19](LiveCovid19.apply)

}

sealed trait Region {
  def suffix: String
}

object Region {
  case object US extends Region {
    def suffix: String = "_us"
  }

  case object Global extends Region {
    def suffix: String = ""
  }

}
