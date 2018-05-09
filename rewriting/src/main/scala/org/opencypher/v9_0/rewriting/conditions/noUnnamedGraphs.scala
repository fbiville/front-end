/*
 * Copyright © 2002-2018 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.ast.conditions

import org.opencypher.v9_0.ast.SingleGraphAs
import org.opencypher.v9_0.rewriting.Condition

case object noUnnamedGraphs extends Condition {
  def apply(that: Any): Seq[String] = {
    val graphs = collectNodesOfType[SingleGraphAs].apply(that)
    val unnamed = graphs.filter(_.as.isEmpty)
    unnamed.map { graphDef => s"GraphDef at ${graphDef.position} is unnamed" }
  }

  override def name: String = productPrefix
}