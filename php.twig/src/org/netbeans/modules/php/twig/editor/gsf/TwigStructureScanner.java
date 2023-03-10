/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s): Sebastian H??rl
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.twig.editor.gsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.twig.editor.parsing.TwigParserResult;

public class TwigStructureScanner implements StructureScanner {

    @Override
    public List<? extends StructureItem> scan(ParserResult info) {
        TwigParserResult result = (TwigParserResult) info;
        List<TwigParserResult.Block> blocks = new ArrayList<>();
        List<TwigStructureItem> items = new ArrayList<>();
        for (TwigParserResult.Block item : result.getBlocks()) {
            if (CharSequenceUtilities.equals(item.getDescription(), "block") || CharSequenceUtilities.equals(item.getDescription(), "*inline-block")) { //NOI18N
                blocks.add(item);
            }
        }
        boolean isTopLevel;
        for (TwigParserResult.Block item : blocks) {
            isTopLevel = true;
            for (TwigParserResult.Block check : blocks) {
                if (item.getOffset() > check.getOffset()
                        && item.getOffset() + item.getLength() < check.getOffset() + check.getLength()) {
                    isTopLevel = false;
                    break;
                }
            }
            if (isTopLevel) {
                items.add(new TwigStructureItem(result.getSnapshot(), item, blocks));
            }
        }
        return items;

    }

    @Override
    public Map<String, List<OffsetRange>> folds(ParserResult info) {
        TwigParserResult result = (TwigParserResult) info;
        List<OffsetRange> ranges = new ArrayList<>();
        for (TwigParserResult.Block block : result.getBlocks()) {
            ranges.add(new OffsetRange(
                    block.getOffset(), block.getOffset() + block.getLength()));
        }
        return Collections.singletonMap("tags", ranges); //NOI18N

    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }
}