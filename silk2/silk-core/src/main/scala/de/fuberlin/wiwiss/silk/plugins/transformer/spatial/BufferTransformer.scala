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

package de.fuberlin.wiwiss.silk.plugins.transformer.spatial

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.spatial.SpatialExtensionsUtils.getBufferedGeometry

/**
 * This plugin returns the buffered geometry of the input geometry (It assumes that geometries are expressed in WKT and WGS 84 (latitude-longitude)).
 * In case that the literal is not a geometry, it is returned as it is.
 *
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@Plugin(
  id = "BufferTransformer",
  categories = Array("Spatial"),
  label = "Buffer Transformer",
  description = "Returns the buffered geometry of the input geometry. Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class BufferTransformer(distance: Double = 0.0) extends SimpleTransformer {

  override def evaluate(value: String) = {
    getBufferedGeometry(value, distance)
  }

}