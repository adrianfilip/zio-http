package zio.http

import io.netty.handler.codec.http.{QueryStringDecoder, QueryStringEncoder}
import scala.jdk.CollectionConverters._

final case class QueryParams private[http] (map: Map[String, List[String]])
    extends scala.collection.Map[String, List[String]] {
  self =>

  override def -(key: String): QueryParams = QueryParams(map - key)

  override def -(key1: String, key2: String, keys: String*): QueryParams =
    QueryParams(map.--(List(key1, key2) ++ keys))

  @deprecated("Use add instead", "2.12")
  override def +[V1 >: List[String]](kv: (String, V1)): QueryParams =
    throw new UnsupportedOperationException("deprecated")

  def ++(other: QueryParams): QueryParams =
    QueryParams((map.toList ++ other.map.toList).groupBy(_._1).map { case (key, values) =>
      (key, values.flatMap(_._2))
    })

  def add(key: String, value: String): QueryParams = if (map.contains(key)) {
    self.copy(
      map = map.map { case (key, oldValue) =>
        (key, oldValue :+ value)
      },
    )
  } else self.copy(map = map.updated(key, List(value)))

  def add(key: String, value: List[String]): QueryParams = if (map.contains(key)) {
    self.copy(
      map = map.map { case (key, oldValue) =>
        (key, oldValue ++ value)
      },
    )
  } else self.copy(map = map.updated(key, value))

  def encode: String = {
    val encoder = new QueryStringEncoder(s"")
    map.foreach { case (key, values) =>
      if (key != "") values.foreach { value => encoder.addParam(key, value) }
    }

    encoder.toString

  }

  def toMap: Map[String, List[String]] = map

  override def get(key: String): Option[List[String]] = map.get(key)

  override def iterator: Iterator[(String, List[String])] = map.iterator

}

object QueryParams {

  def apply(tuples: Tuple2[String, String]*): QueryParams =
    QueryParams(map = tuples.groupBy(_._1).map { case (key, values) =>
      key -> values.map(_._2).toList
    })

  def decode(queryStringFragment: String): QueryParams =
    if (queryStringFragment == null || queryStringFragment.isEmpty) {
      QueryParams.empty
    } else {
      val decoder = new QueryStringDecoder(queryStringFragment, false)
      val params  = decoder.parameters()
      QueryParams(params.asScala.view.map { case (k, v) =>
        (k, v.asScala.toList)
      }.toMap)
    }

  val empty: QueryParams = QueryParams(Map.empty[String, List[String]])

}
