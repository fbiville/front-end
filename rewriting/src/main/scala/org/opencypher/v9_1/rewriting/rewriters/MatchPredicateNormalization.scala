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
package org.opencypher.v9_1.rewriting.rewriters

import org.opencypher.v9_1.ast.Match
import org.opencypher.v9_1.expressions._
import org.opencypher.v9_1.util.{InputPosition, Rewriter, topDown}
import org.opencypher.v9_1.ast.Where
import org.opencypher.v9_1.expressions
import org.opencypher.v9_1.expressions.{And, GreaterThan, Not, Or}

abstract class MatchPredicateNormalization(normalizer: MatchPredicateNormalizer, getDegreeRewriting: Boolean) extends Rewriter {

  def apply(that: AnyRef): AnyRef = instance(that)

  private val rewriter = Rewriter.lift {
    case m@Match(_, pattern, _, where) =>
      val predicates = pattern.fold(Vector.empty[Expression]) {
        case pattern: AnyRef if normalizer.extract.isDefinedAt(pattern) => acc => acc ++ normalizer.extract(pattern)
        case _                                                          => identity
      }

      val rewrittenPredicates: List[Expression] = (predicates ++ where.map(_.expression)).toList

      val predOpt: Option[Expression] = rewrittenPredicates match {
        case Nil => None
        case exp :: Nil => Some(exp)
        case list => Some(list.reduce(And(_, _)(m.position)))
      }

      val newWhere: Option[Where] = predOpt.map {
        exp =>
          val pos: InputPosition = where.fold(m.position)(_.position)
          val e = if (getDegreeRewriting)
            exp.endoRewrite(whereRewriter)
          else
            exp
          Where(e)(pos)
      }

      m.copy(
        pattern = pattern.endoRewrite(topDown(Rewriter.lift(normalizer.replace))),
        where = newWhere
      )(m.position)
  }

  private def whereRewriter: Rewriter = Rewriter.lift {
    // WHERE (a)-[:R]->() to WHERE GetDegree( (a)-[:R]->()) > 0
    case p@PatternExpression(RelationshipsPattern(RelationshipChain(NodePattern(Some(node), List(), None),
                                                                    RelationshipPattern(None, types, None, None, dir, _),
                                                                    NodePattern(None, List(), None)))) =>
      GreaterThan(calculateUsingGetDegree(p, node, types, dir), SignedDecimalIntegerLiteral("0")(p.position))(p.position)
    // WHERE ()-[:R]->(a) to WHERE GetDegree( (a)<-[:R]-()) > 0
    case p@PatternExpression(RelationshipsPattern(RelationshipChain(NodePattern(None, List(), None),
                                                                    RelationshipPattern(None, types, None, None, dir, _),
                                                                    NodePattern(Some(node), List(), None)))) =>
      expressions.GreaterThan(calculateUsingGetDegree(p, node, types, dir.reversed), SignedDecimalIntegerLiteral("0")(p.position))(p.position)

    case a@And(lhs, rhs) =>
      And(lhs.endoRewrite(whereRewriter), rhs.endoRewrite(whereRewriter))(a.position)

    case o@Or(lhs, rhs) => Or(lhs.endoRewrite(whereRewriter), rhs.endoRewrite(whereRewriter))(o.position)

    case n@Not(e) => Not(e.endoRewrite(whereRewriter))(n.position)
  }

  private val instance = topDown(rewriter, _.isInstanceOf[Expression])
}