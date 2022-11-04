package org.mkuthan.streamprocessing.shared.test.scio

import org.scalatest.Suite

import org.mkuthan.streamprocessing.shared.test.gcp.PubSubClient
import org.mkuthan.streamprocessing.toll.shared.configuration.PubSubSubscription
import org.mkuthan.streamprocessing.toll.shared.configuration.PubSubTopic

trait PubSubScioContext extends GcpScioContext with PubSubClient {
  this: Suite =>

  def withTopic[T](fn: PubSubTopic[T] => Any): Unit = {
    val topicName = generateTopicName()
    try {
      createTopic(topicName)
      fn(PubSubTopic[T](topicName))
    } finally
      deleteTopic(topicName)
  }

  def withSubscription[T](topicName: String)(fn: PubSubSubscription[T] => Any): Unit = {
    val subscriptionName = generateSubscriptionName()
    try {
      createSubscription(topicName, subscriptionName)
      fn(PubSubSubscription[T](subscriptionName))
    } finally
      deleteSubscription(subscriptionName)
  }
}
