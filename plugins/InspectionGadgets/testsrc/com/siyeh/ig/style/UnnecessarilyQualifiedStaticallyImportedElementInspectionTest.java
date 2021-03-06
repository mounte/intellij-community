/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.style;

import com.siyeh.ig.IGInspectionTestCase;

public class UnnecessarilyQualifiedStaticallyImportedElementInspectionTest
  extends IGInspectionTestCase {

  public void test() throws Exception {
    doTest("com/siyeh/igtest/style/unnecessarily_qualified_statically_imported_element",
           new UnnecessarilyQualifiedStaticallyImportedElementInspection());
  }

  public void testSameMemberNames() throws Exception {
    doTest(getTestName(true));
  }

  public void testMethodRef() throws Exception {
    doTest(getTestName(true));
  }

  private void doTest(String testName) throws Exception {
    doTest("com/siyeh/igtest/style/unnecessarily_qualified_statically_imported_element/" + testName,
           new UnnecessarilyQualifiedStaticallyImportedElementInspection());
  }
}