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
package org.opencypher.v9_0.rewriting.rewriters

import org.opencypher.v9_0.expressions.{NodePattern, RelationshipPattern, Variable}
import org.opencypher.v9_0.util.attribution.{Attributes, SameId}
import org.opencypher.v9_0.util.{Rewriter, UnNamedNameGenerator, bottomUp}

case class nameAllPatternElements(attributes: Attributes) extends Rewriter {

  private val namingRewriter: Rewriter = bottomUp(Rewriter.lift {
    case pattern: NodePattern if pattern.variable.isEmpty =>
      val syntheticName = UnNamedNameGenerator.name(pattern.position.bumped())
      pattern.copy(variable = Some(Variable(syntheticName)(pattern.position)(attributes.copy(pattern.id))))(pattern.position)(SameId(pattern.id))

    case pattern: RelationshipPattern if pattern.variable.isEmpty =>
      val syntheticName = UnNamedNameGenerator.name(pattern.position.bumped())
      pattern.copy(variable = Some(Variable(syntheticName)(pattern.position)(attributes.copy(pattern.id))))(pattern.position)(SameId(pattern.id))
  })

  override def apply(in: AnyRef): AnyRef = namingRewriter.apply(in)
}
