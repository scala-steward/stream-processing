package org.mkuthan.streamprocessing.shared.test.scio

import scala.reflect.ClassTag

import com.spotify.scio.coders.Coder
import com.spotify.scio.testing.SCollectionMatchers
import com.spotify.scio.values.SCollection

import cats.kernel.Eq
import org.apache.beam.sdk.transforms.windowing.IntervalWindow
import org.joda.time.Instant
import org.scalatest.matchers.Matcher

trait TimestampedMatchers {
  this: SCollectionMatchers =>

  import InstantConverters._

  def inOnTimePane[T: ClassTag](begin: String, end: String)(matcher: MatcherBuilder[T]): Matcher[T] =
    inOnTimePane(new IntervalWindow(stringToInstant(begin), stringToInstant(end)))(matcher)

  def inLatePane[T: ClassTag](begin: String, end: String)(matcher: MatcherBuilder[T]): Matcher[T] =
    inLatePane(new IntervalWindow(stringToInstant(begin), stringToInstant(end)))(matcher)

  def inEarlyPane[T: ClassTag](begin: String, end: String)(matcher: MatcherBuilder[T]): Matcher[T] =
    inEarlyPane(new IntervalWindow(stringToInstant(begin), stringToInstant(end)))(matcher)

  def inFinalPane[T: ClassTag](begin: String, end: String)(matcher: MatcherBuilder[T]): Matcher[T] =
    inFinalPane(new IntervalWindow(stringToInstant(begin), stringToInstant(end)))(matcher)

  def inWindow[T: ClassTag, B: ClassTag](begin: String, end: String)(matcher: IterableMatcher[T, B]): Matcher[T] =
    inWindow(new IntervalWindow(stringToInstant(begin), stringToInstant(end)))(matcher)

  /**
   * Assert that the SCollection contains the provided element at given time without making assumptions about other
   * elements in the collection.
   */
  def containValueAtTime[T: Coder: Eq](
      time: String,
      value: T
  ): IterableMatcher[SCollection[(T, Instant)], (T, Instant)] =
    containValue((value, stringToInstant(time)))

  /**
   * Assert that the SCollection contains a single provided element at given time.
   */
  def containSingleValueAtTime[T: Coder: Eq](
      time: String,
      value: T
  ): SingleMatcher[SCollection[(T, Instant)], (T, Instant)] =
    containSingleValue((value, stringToInstant(time)))

  /**
   * Assert that the SCollection contains the provided elements at given time.
   */
  def containInAnyOrderAtTime[T: Coder: Eq](
      time: String,
      value: Iterable[T]
  ): IterableMatcher[SCollection[(T, Instant)], (T, Instant)] =
    containInAnyOrder(value.map { case v => (v, stringToInstant(time)) })

  /**
   * Assert that the SCollection contains the provided timestamped elements.
   */
  def containInAnyOrderAtTime[T: Coder: Eq](
      value: Iterable[(String, T)]
  ): IterableMatcher[SCollection[(T, Instant)], (T, Instant)] =
    containInAnyOrder(value.map { case (time, v) => (v, stringToInstant(time)) })

}
