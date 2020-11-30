/**
 * boilerpipe
 *
 * Copyright (c) 2009, 2014 Christian Kohlschütter
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.boilerpipe.filters.english;

import java.util.List;
import java.util.ListIterator;

import com.kohlschutter.boilerpipe.BoilerpipeFilter;
import com.kohlschutter.boilerpipe.BoilerpipeProcessingException;
import com.kohlschutter.boilerpipe.document.TextBlock;
import com.kohlschutter.boilerpipe.document.TextDocument;

/**
 * Classifies {@link TextBlock}s as content/not-content through rules that have been determined
 * using the C4.8 machine learning algorithm, as described in the paper
 * "Boilerplate Detection using Shallow Text Features", particularly using text densities and link
 * densities.
 */
public class DensityRulesClassifier implements BoilerpipeFilter {
  public static final DensityRulesClassifier INSTANCE = new DensityRulesClassifier();

  /**
   * Returns the singleton instance for RulebasedBoilerpipeClassifier.
   */
  public static DensityRulesClassifier getInstance() {
    return INSTANCE;
  }

  public boolean process(TextDocument doc) throws BoilerpipeProcessingException {
    List<TextBlock> textBlocks = doc.getTextBlocks();
    boolean hasChanges = false;

    ListIterator<TextBlock> it = textBlocks.listIterator();
    if (!it.hasNext()) {
      return false;
    }
    TextBlock prevBlock = TextBlock.EMPTY_START;
    TextBlock currentBlock = it.next();
    TextBlock nextBlock = it.hasNext() ? it.next() : TextBlock.EMPTY_START;

    hasChanges = classify(prevBlock, currentBlock, nextBlock) | hasChanges;

    if (nextBlock != TextBlock.EMPTY_START) {
      while (it.hasNext()) {
        prevBlock = currentBlock;
        currentBlock = nextBlock;
        nextBlock = it.next();
        hasChanges = classify(prevBlock, currentBlock, nextBlock) | hasChanges;
      }
      prevBlock = currentBlock;
      currentBlock = nextBlock;
      nextBlock = TextBlock.EMPTY_START;
      hasChanges = classify(prevBlock, currentBlock, nextBlock) | hasChanges;
    }

    return hasChanges;
  }

  /**
   * Classifies a given TextBlock curr using a heuristic algorithm.
   * Returns true if isContent of the TextBlock curr is changed, false otherwise.
   */
  protected boolean classify(final TextBlock prev, final TextBlock curr, final TextBlock next) {
    final boolean isContent;

    if (curr.getLinkDensity() <= 0.333333) {
      if (prev.getLinkDensity() <= 0.555556) {
        if (curr.getTextDensity() <= 9) {
          if (next.getTextDensity() <= 10) {
            isContent = prev.getTextDensity() > 4;
          } else {
            isContent = true;
          }
        } else {
          isContent = next.getTextDensity() != 0;
        }
      } else {
        isContent = next.getTextDensity() > 11;
      }
    } else {
      isContent = false;
    }

    // Update isContent in curr, and return true if the value is changed.
    return curr.setIsContent(isContent);
  }

}
