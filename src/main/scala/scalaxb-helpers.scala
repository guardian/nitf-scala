package com.gu

import scala.collection.breakOut

import _root_.scalaxb.{CanWriteXML, DataRecord}
import _root_.scalaxb.XMLStandardTypes.__StringXMLFormat

/** Helper functions for creating instances of `DataRecord` and attributes */
package object scalaxb {
  type StringAttribute = (String, DataRecord[String])
  type StringAttributes = Map[String, DataRecord[String]]

  def attrs(keysAndValues: (String, String)*): StringAttributes = keysAndValues.map { case (k, v) => attr(k, v) }(breakOut)
  def attr(key: String, value: String): StringAttribute = attrWithValue(key, value)

  /** Prepends the key with ''@'' to match scalaxb's conventions and wraps the value in a `DataRecord` */
  def attrWithValue[T: CanWriteXML](key: String, value: T): (String, DataRecord[T]) = s"@$key" -> dataRecord(value)

  def dataRecord[T: CanWriteXML](x: T, xmlTagName: Option[String] = None): DataRecord[T] =
    DataRecord(namespace = None, key = xmlTagName, value = x)
}
