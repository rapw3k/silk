package org.silkframework.dataset

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}

/**
 * Manages the access to a specific dataset.
 */
trait Dataset extends TaskSpec with AnyPlugin with SinkTrait {

  /**
   * Returns a data source for reading entities from the data set.
   */
  def source: DataSource

  /**
   * Makes sure that the next write will start from an empty dataset.
   */
  def clear(): Unit

}

trait DatasetPluginAutoConfigurable[T <: Dataset] {
  /**
   * returns an auto-configured version of this plugin
   */
  def autoConfigured: T
}

trait SinkTrait {
  /**
   * Returns a link sink for writing entity links to the data set.
   */
  def linkSink: LinkSink

  /**
   * Returns a entity sink for writing entities to the data set.
   */
  def entitySink: EntitySink
}

/**
 * Creates new dataset olugin instances.
 */
object Dataset extends PluginFactory[Dataset]