/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.similarity

import org.silkframework.entity.Index
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}

trait Aggregator extends AnyPlugin {
  def evaluate(weightedValues: Traversable[(Int, Double)]): Option[Double]

  /**
   * Combines two indexes into one.
   */
  def combineIndexes(index1: Index, index2: Index): Index

  def computeThreshold(threshold: Double, weight: Double): Double = {
    threshold
  }
}

object Aggregator extends PluginFactory[Aggregator]
