package org.mkuthan.streamprocessing.toll.infrastructure.scio

import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.mkuthan.streamprocessing.shared.test.gcp.BigQueryClient
import org.mkuthan.streamprocessing.shared.test.scio.BigQueryScioContext
import org.mkuthan.streamprocessing.toll.infrastructure.json.JsonSerde
import org.mkuthan.streamprocessing.toll.shared.configuration.StorageBucket

class ScioContextBigQuerySyntaxTest extends AnyFlatSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with BigQueryScioContext
    with BigQueryClient
    with ScioContextBigQuerySyntax
    with SCollectionStorageSyntax {

  import IntegrationTestFixtures._

  behavior of "SCollectionBigQuerySyntax"

  // TODO: implement writeTable to prepare test data
  ignore should "load from table" in withScioContext { sc =>
    withDataset { datasetName =>
      withTable[SimpleClass](datasetName) { bigQueryTable =>
        writeTable(
          bigQueryTable.datasetName,
          bigQueryTable.tableName,
          simpleClassBigQueryType.toAvro(simpleObject1),
          simpleClassBigQueryType.toAvro(simpleObject2)
        )

        val tmpBucket = new StorageBucket[SimpleClass](bucket = sc.options.getTempLocation, numShards = 1)

        sc
          .loadFromBigQuery(bigQueryTable)
          .saveToStorageAsJson(tmpBucket)

        sc.run().waitUntilDone()

        eventually {
          val results = readObjectLines(tmpBucket.name, "GlobalWindow-pane-0-00000-of-00001.json")
            .map(JsonSerde.readJson[SimpleClass])

          results should contain.only(simpleObject1, simpleObject2)
        }
      }
    }
  }
}
