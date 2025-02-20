package org.mkuthan.streamprocessing.toll.domain.booth

import scala.util.control.NonFatal

import com.spotify.scio.coders.Coder
import com.spotify.scio.values.SCollection
import com.spotify.scio.values.SideOutput
import com.spotify.scio.ScioMetrics

import org.apache.beam.sdk.metrics.Counter
import org.joda.time.Instant

import org.mkuthan.streamprocessing.toll.domain.common.LicensePlate

final case class TollBoothExit(
    id: TollBoothId,
    exitTime: Instant,
    licensePlate: LicensePlate
)

object TollBoothExit {

  implicit val CoderCache: Coder[TollBoothExit] = Coder.gen
  implicit val CoderCacheRaw: Coder[TollBoothExit.Raw] = Coder.gen

  val DlqCounter: Counter = ScioMetrics.counter[TollBoothExit]("dlq")

  final case class Raw(
      id: String,
      exit_time: String,
      license_plate: String
  )

  def decode(inputs: SCollection[Raw]): (SCollection[TollBoothExit], SCollection[Raw]) = {
    val dlq = SideOutput[Raw]()
    val (results, sideOutputs) = inputs
      .withSideOutputs(dlq)
      .flatMap { case (input, ctx) =>
        try
          Some(fromRaw(input))
        catch {
          case NonFatal(_) =>
            ctx.output(dlq, input)
            DlqCounter.inc()
            None
        }
      }

    (results, sideOutputs(dlq))
  }

  private def fromRaw(raw: Raw): TollBoothExit =
    TollBoothExit(
      id = TollBoothId(raw.id),
      exitTime = Instant.parse(raw.exit_time),
      licensePlate = LicensePlate(raw.license_plate)
    )
}
