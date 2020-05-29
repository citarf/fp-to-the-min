package fpmin

import scala.io.Source

import java.io.IOException
import zio._
import zio.blocking._

trait Github {
  def download(slug: String, file: String): ZIO[Any, IOException, String]
}

case class LiveGithub(blocking: Blocking.Service) extends Github {
  import blocking.effectBlocking

  def download(slug: String, file: String): ZIO[Any, IOException, String] =
    open(slug, file).use { source =>
      effectBlocking(source.getLines().mkString("\n")).refineToOrDie[IOException]
    }

  def open(slug: String, file: String): Managed[IOException, Source] = {
    val acquire = effectBlocking(unsafeOpen(slug, file)).refineToOrDie[IOException]
    val release = (source: Source) => effectBlocking(source.close()).orDie
    Managed.make(acquire)(release)
  }

  /**
   * Opens a file from Github, so that it can be streamed or downloaded.
   */
  private def unsafeOpen(slug: String, file: String): Source =
    Source.fromURL(Github.downloadUrl(slug, file))
}

object Github {
  val live: ZLayer[Blocking, Nothing, Has[Github]] =
    ZLayer.fromService[Blocking.Service, Github](LiveGithub.apply)

  def downloadUrl(slug: String, file: String): String =
    s"https://github.com/${slug}/raw/master/${file}"
}
