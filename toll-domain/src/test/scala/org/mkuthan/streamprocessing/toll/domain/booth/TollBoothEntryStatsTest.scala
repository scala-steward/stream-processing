package org.mkuthan.streamprocessing.toll.domain.booth

import com.spotify.scio.testing.testStreamOf
import com.spotify.scio.testing.PipelineSpec
import com.spotify.scio.testing.TestStreamScioContext

import org.joda.time.Duration
import org.joda.time.Instant

import org.mkuthan.streamprocessing.shared.test.scio._
import org.mkuthan.streamprocessing.shared.test.scio.TimestampedMatchers

class TollBoothEntryStatsTest extends PipelineSpec
    with TimestampedMatchers
    with TollBoothEntryFixture
    with TollBoothEntryStatsFixture {

  import TollBoothEntryStats._

  private val FiveMinutes = Duration.standardMinutes(5)

  behavior of "TollBoothEntryStats"

  it should "calculate TollBoothEntryStats in fixed window" in runWithContext { sc =>
    val tollBoothId1 = TollBoothId("1")
    val tollBoothId2 = TollBoothId("2")

    val tollBoothEntry1Time = Instant.parse("2014-09-10T12:01:00.000Z")
    val tollBoothEntry1 = anyTollBoothEntry.copy(
      id = tollBoothId1,
      entryTime = tollBoothEntry1Time,
      toll = BigDecimal(2)
    )

    val tollBoothEntry2Time = Instant.parse("2014-09-10T12:01:30.000Z")
    val tollBoothEntry2 = anyTollBoothEntry.copy(
      id = tollBoothId1,
      entryTime = tollBoothEntry2Time,
      toll = BigDecimal(1)
    )

    val tollBoothEntry3Time = Instant.parse("2014-09-10T12:04:00.000Z")
    val tollBoothEntry3 = anyTollBoothEntry.copy(
      id = tollBoothId2,
      entryTime = tollBoothEntry3Time,
      toll = BigDecimal(3)
    )

    val inputs = testStreamOf[TollBoothEntry]
      .addElementsAtTime(tollBoothEntry1.entryTime, tollBoothEntry1)
      .addElementsAtTime(tollBoothEntry2.entryTime, tollBoothEntry2)
      .addElementsAtTime(tollBoothEntry3.entryTime, tollBoothEntry3)
      .advanceWatermarkToInfinity()

    val results = calculateInFixedWindow(sc.testStream(inputs), FiveMinutes)

    results.withTimestamp should inOnTimePane("2014-09-10T12:00:00Z", "2014-09-10T12:05:00Z") {
      containInAnyOrderAtTime(
        "2014-09-10T12:04:59.999Z",
        Seq(
          anyTollBoothEntryStats.copy(
            id = tollBoothId1,
            count = 2,
            totalToll = BigDecimal(2 + 1),
            firstEntryTime = tollBoothEntry1Time,
            lastEntryTime = tollBoothEntry2Time
          ),
          anyTollBoothEntryStats.copy(
            id = tollBoothId2,
            count = 1,
            totalToll = BigDecimal(3),
            firstEntryTime = tollBoothEntry3Time,
            lastEntryTime = tollBoothEntry3Time
          )
        )
      )
    }
  }

  it should "encode TollBoothEntryStats to raw" in runWithContext { sc =>
    val recordTimestamp = Instant.parse("2014-09-10T12:04:59.999Z")
    val inputs = testStreamOf[TollBoothEntryStats]
      .addElementsAtTime(recordTimestamp, anyTollBoothEntryStats)
      .advanceWatermarkToInfinity()

    val results = encode(sc.testStream(inputs))
    results should containSingleValue(anyTollBoothEntryStatsRaw.copy(record_timestamp = recordTimestamp))
  }
}
