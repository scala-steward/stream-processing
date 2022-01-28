package org.mkuthan.examples.streaming.usersessions

import com.spotify.scio.testing.PipelineSpec
import com.spotify.scio.testing.TestStreamScioContext
import com.spotify.scio.testing.testStreamOf
import org.joda.time.Duration
import org.mkuthan.examples.streaming.beam.TimestampedMatchers
import org.mkuthan.examples.streaming.beam._

class BeamUserSessionsTest extends PipelineSpec with TimestampedMatchers {

  import BeamUserSessions._

  private val DefaultGapDuration = Duration.standardMinutes(10L)

  "Session" should "be empty for empty stream" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)].advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(sc.testStream(userActions), DefaultGapDuration)

    results should beEmpty
  }

  "Short visit" should "be aggregated into single session" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)]
      .addElementsAtTime("00:00:00", ("jack", "open app"))
      .addElementsAtTime("00:01:00", ("jack", "close app"))
      .advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(sc.testStream(userActions), DefaultGapDuration)

    results.withTimestamp should inOnTimePane("00:00:00", "00:11:00") {
      containSingleValueAtTime("00:10:59.999", ("jack", Iterable("open app", "close app")))
    }
  }

  "Short visit from two clients" should "be aggregated into two simultaneous sessions" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)]
      .addElementsAtTime("00:00:00", ("jack", "open app"))
      .addElementsAtTime("00:00:00", ("ben", "open app"))
      .addElementsAtTime("00:01:00", ("jack", "close app"))
      .addElementsAtTime("00:01:30", ("ben", "close app"))
      .advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(sc.testStream(userActions), DefaultGapDuration)

    results.withTimestamp should inOnTimePane("00:00:00", "00:11:00") {
      containSingleValueAtTime("00:10:59.999", ("jack", Iterable("open app", "close app")))
    }

    results.withTimestamp should inOnTimePane("00:00:00", "00:11:30") {
      containSingleValueAtTime("00:11:29.999", ("ben", Iterable("open app", "close app")))
    }
  }

  "Long but continuous visit" should "be aggregated into single sessions" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)]
      .addElementsAtTime("00:00:00", ("jack", "open app"))
      .addElementsAtTime("00:01:00", ("jack", "search product"))
      .addElementsAtTime("00:01:30", ("jack", "open product"))
      .addElementsAtTime("00:03:00", ("jack", "add to cart"))
      .addElementsAtTime("00:09:30", ("jack", "checkout"))
      .addElementsAtTime("00:13:10", ("jack", "close app"))
      .advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(sc.testStream(userActions), DefaultGapDuration)

    results.withTimestamp should inOnTimePane("00:00:00", "00:23:10") {
      containSingleValueAtTime(
        "00:23:09.999",
        ("jack", Iterable("open app", "search product", "open product", "add to cart", "checkout", "close app")))
    }
  }

  "Long interrupted visit" should "be aggregated into two sessions" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)]
      .addElementsAtTime("00:00:00", ("jack", "open app"))
      .addElementsAtTime("00:01:00", ("jack", "search product"))
      .addElementsAtTime("00:01:30", ("jack", "open product"))
      .addElementsAtTime("00:03:00", ("jack", "add to cart"))
      .addElementsAtTime("00:13:00", ("jack", "checkout"))
      .addElementsAtTime("00:13:10", ("jack", "close app"))
      .advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(sc.testStream(userActions), DefaultGapDuration)

    results.withTimestamp should inOnTimePane("00:00:00", "00:13:00") {
      containSingleValueAtTime(
        "00:12:59.999",
        ("jack", Iterable("open app", "search product", "open product", "add to cart")))
    }

    results.withTimestamp should inOnTimePane("00:13:00", "00:23:10") {
      containSingleValueAtTime("00:23:09.999", ("jack", Iterable("checkout", "close app")))
    }
  }

  "Late event" should "not close the gap and two sessions are materialized" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)]
      .addElementsAtTime("00:00:00", ("jack", "open app"))
      .addElementsAtTime("00:01:00", ("jack", "search product"))
      .addElementsAtTime("00:01:30", ("jack", "open product"))
      .advanceWatermarkTo("00:13:00")
      .addElementsAtTime("00:03:00", ("jack", "add to cart")) // dropped due to lateness
      .addElementsAtTime("00:09:30", ("jack", "checkout"))
      .addElementsAtTime("00:13:10", ("jack", "close app"))
      .advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(sc.testStream(userActions), DefaultGapDuration)

    results.withTimestamp should inOnTimePane("00:00:00", "00:11:30") {
      containSingleValueAtTime("00:11:29.999", ("jack", Iterable("open app", "search product", "open product")))
    }

    results.withTimestamp should inOnTimePane("00:09:30", "00:23:10") {
      containSingleValueAtTime("00:23:09.999", ("jack", Iterable("checkout", "close app")))
    }
  }

  "Late event" should "TODO" in runWithContext { sc =>
    val userActions = testStreamOf[(User, Action)]
      .addElementsAtTime("00:00:00", ("jack", "open app"))
      .addElementsAtTime("00:01:00", ("jack", "search product"))
      .addElementsAtTime("00:01:30", ("jack", "open product"))
      .addElementsAtTime("00:03:00", ("jack", "add to cart"))
      .advanceWatermarkTo("00:13:00")
      .addElementsAtTime("00:09:30", ("jack", "checkout"))
      .addElementsAtTime("00:13:10", ("jack", "close app"))
      .advanceWatermarkToInfinity()

    val results = activitiesInSessionWindow(
      sc.testStream(userActions),
      DefaultGapDuration,
      allowedLateness = Duration.standardMinutes(5))

    results.withTimestamp should inOnTimePane("00:00:00", "00:13:00") {
      containSingleValueAtTime("00:12:59.999", ("jack", Iterable("open app", "search product", "open product", "add to cart")))
    }

    //    results.withTimestamp should inWindow("00:09:30", "00:23:10") {
    //      containInAnyOrderAtTime(Seq(
    //        ("00:23:09.999", ("jack", Iterable("checkout", "close app")))
    //      ))
    //    }
  }

  // TODO: late event that fill the gap (discarded vs. accumulated)

  // TODO: speculative early results
}
