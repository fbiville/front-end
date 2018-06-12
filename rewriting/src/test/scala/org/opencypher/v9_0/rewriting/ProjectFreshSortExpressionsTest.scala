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
package org.opencypher.v9_0.rewriting

import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.ast.semantics.{SemanticState, SyntaxExceptionCreator}
import org.opencypher.v9_0.rewriting.rewriters.{expandStar, normalizeReturnClauses, normalizeWithClauses, projectFreshSortExpressions}
import org.opencypher.v9_0.util.attribution.Attributes
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite
import org.opencypher.v9_0.util.{Rewriter, inSequence}

class ProjectFreshSortExpressionsTest extends CypherFunSuite with RewriteTest with AstConstructionTestSupport {
  val rewriterUnderTest: Rewriter = projectFreshSortExpressions(Attributes(idGen))

  test("don't adjust WITH without ORDER BY or WHERE") {
    assertRewrite(
      """MATCH n
        |WITH n AS n
        |RETURN n
      """.stripMargin,
      """MATCH n
        |WITH n AS n
        |RETURN n
      """.stripMargin)
  }

  test("duplicate WITH containing ORDER BY") {
    assertRewrite(
      """MATCH n
        |WITH n.prop AS prop ORDER BY prop
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH n.prop AS prop
        |WITH prop AS prop ORDER BY prop
        |RETURN prop
      """.stripMargin)
  }

  test("duplicate WITH containing ORDER BY that refers to previous variable") {
    assertRewrite(
      """MATCH n
        |WITH n.prop AS prop ORDER BY prop + n.x
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH n AS n, n.prop AS prop
        |WITH prop AS prop, prop + n.x AS `  FRESHID42`
        |WITH prop AS prop, `  FRESHID42` AS `  FRESHID42` ORDER BY `  FRESHID42`
        |WITH prop AS prop
        |RETURN prop AS prop
      """.stripMargin)
  }

  test("duplicate RETURN containing ORDER BY after WITH") {
    assertRewrite(
      """WITH 1 AS p, count(*) AS rng
        |RETURN p ORDER BY rng
      """.stripMargin,
      """WITH 1 AS p, count(*) AS rng
        |WITH p AS `  FRESHID36`, rng AS rng
        |WITH `  FRESHID36` AS `  FRESHID36` ORDER BY rng
        |RETURN `  FRESHID36` AS p
      """.stripMargin
    )
  }

  test("duplicate WITH containing WHERE") {
    assertRewrite(
      """MATCH n
        |WITH n.prop AS prop WHERE prop
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH n.prop AS prop
        |WITH prop AS prop WHERE prop
        |RETURN prop AS prop
      """.stripMargin)
  }

  test("preserve DISTINCT on first WITH") {
    assertRewrite(
      """MATCH n
        |WITH DISTINCT n.prop AS prop ORDER BY prop
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH DISTINCT n.prop AS prop
        |WITH prop AS prop ORDER BY prop
        |RETURN prop
      """.stripMargin)

    assertRewrite(
      """MATCH n
        |WITH DISTINCT n.prop AS prop WHERE prop
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH DISTINCT n.prop AS prop
        |WITH prop AS prop WHERE prop
        |RETURN prop
      """.stripMargin)
  }

  test("carry SKIP and LIMIT with ORDER BY") {
    assertRewrite(
      """MATCH n
        |WITH n.prop AS prop ORDER BY prop SKIP 2 LIMIT 5
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH n.prop AS prop
        |WITH prop AS prop ORDER BY prop SKIP 2 LIMIT 5
        |RETURN prop
      """.stripMargin)
  }

  test("carry SKIP and LIMIT with WHERE") {
    assertRewrite(
      """MATCH n
        |WITH n.prop AS prop SKIP 2 LIMIT 5 WHERE prop
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH n.prop AS prop
        |WITH prop AS prop SKIP 2 LIMIT 5 WHERE prop
        |RETURN prop
      """.stripMargin)
  }

  test("keep WHERE with ORDER BY") {
    assertRewrite(
      """MATCH n
        |WITH n.prop AS prop ORDER BY prop WHERE prop
        |RETURN prop
      """.stripMargin,
      """MATCH n
        |WITH n.prop AS prop
        |WITH prop AS prop ORDER BY prop WHERE prop
        |RETURN prop
      """.stripMargin)
  }

  test("handle RETURN * ORDERBY property") {
    assertRewrite(
      """MATCH n
        |RETURN * ORDER BY n.prop
      """.stripMargin,
      """MATCH n
        |WITH n AS n
        |WITH n AS n, n.prop AS `  FRESHID28`
        |WITH n AS n, `  FRESHID28` AS `  FRESHID28` ORDER BY `  FRESHID28`
        |WITH n AS n
        |RETURN n AS n
      """.stripMargin)
  }

  test("Does not introduce WITH for ORDER BY over preserved variable") {
    assertIsNotRewritten(
    """MATCH n
      |WITH n AS n, n.prop AS prop
      |WITH n AS n, prop AS prop ORDER BY prop
      |RETURN n AS n
    """.stripMargin
    )
  }

  test("Does not introduce WITH for WHERE over preserved variable") {
    assertIsNotRewritten(
      """MATCH n
        |WITH n AS n, n.prop AS prop
        |WITH n AS n, prop AS prop WHERE prop
        |RETURN n AS n
      """.stripMargin
    )
  }

  protected override def assertRewrite(originalQuery: String, expectedQuery: String) {
    val original = ast(originalQuery)
    val expected = ast(expectedQuery)
    val result = endoRewrite(original)
    assert(result === expected, "\n" + originalQuery)
  }

  private def ast(queryText: String) = {
    val parsed = parseForRewriting(queryText)
    val mkException = new SyntaxExceptionCreator(queryText, Some(pos))
    val attributes = Attributes(idGen)
    val normalized = parsed.endoRewrite(inSequence(normalizeReturnClauses(mkException, attributes), normalizeWithClauses(mkException, attributes)))
    val checkResult = normalized.semanticCheck(SemanticState.clean)
    normalized.endoRewrite(inSequence(expandStar(checkResult.state, attributes)))
  }
}
