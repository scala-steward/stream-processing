package org.mkuthan.streamprocessing.shared.test.scio

import scala.reflect.runtime.universe.TypeTag

import com.spotify.scio.bigquery.types.BigQueryType
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation

import org.scalatest.Suite

import org.mkuthan.streamprocessing.shared.test.gcp.BigQueryClient
import org.mkuthan.streamprocessing.toll.shared.configuration.BigQueryTable

trait BigQueryScioContext extends GcpScioContext with BigQueryClient {
  this: Suite =>

  def withDataset(fn: String => Any): Any = {
    val datasetName = generateDatasetName()
    try {
      createDataset(datasetName)
      fn(datasetName)
    } finally
      deleteDataset(datasetName)
  }

  def withTable[T <: HasAnnotation: TypeTag](datasetName: String)(fn: BigQueryTable[T] => Any): Any = {
    val tableName = generateTableName()
    createTable(datasetName, tableName, BigQueryType[T].schema)
    try
      fn(BigQueryTable[T](s"$datasetName.$tableName"))
    finally
      deleteTable(datasetName, tableName)
  }
}
