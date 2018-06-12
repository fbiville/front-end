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
package org.opencypher.v9_0.ast.semantics

import org.opencypher.v9_0.expressions.Pow
import org.opencypher.v9_0.util.DummyPosition
import org.opencypher.v9_0.util.symbols._

class PowTest extends InfixExpressionTestBase(Pow(_, _)(DummyPosition(0))(_)) {

  // Infix specializations:
  // 1 ^ 1 => 1
  // 1 ^ 1.1 => 1
  // 1.1 ^ 1 => 1.1
  // 1.1 ^ 1.1 => 1.1105

  test("shouldHandleAllSpecializations") {
    testValidTypes(CTInteger, CTInteger)(CTFloat)
    testValidTypes(CTInteger, CTFloat)(CTFloat)
    testValidTypes(CTFloat, CTInteger)(CTFloat)
    testValidTypes(CTFloat, CTFloat)(CTFloat)
  }

  test("shouldHandleCombinedSpecializations") {
    testValidTypes(CTFloat | CTInteger, CTFloat | CTInteger)(CTFloat)
  }

  test("shouldFailTypeCheckWhenAddingIncompatible") {
    testInvalidApplication(CTInteger, CTBoolean)(
      "Type mismatch: expected Float but was Boolean"
    )
    testInvalidApplication(CTBoolean, CTInteger)(
      "Type mismatch: expected Float but was Boolean"
    )
  }
}
