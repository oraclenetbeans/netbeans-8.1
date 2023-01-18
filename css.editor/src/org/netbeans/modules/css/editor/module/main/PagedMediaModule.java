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
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.css.editor.module.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.lexer.Token;
import org.netbeans.modules.csl.api.ColoringAttributes;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.css.editor.Css3Utils;
import org.netbeans.modules.css.editor.module.spi.CompletionContext;
import org.netbeans.modules.css.editor.module.spi.CssEditorModule;
import org.netbeans.modules.css.editor.module.spi.FeatureContext;
import org.netbeans.modules.css.editor.module.spi.Utilities;
import org.netbeans.modules.css.lib.api.CssModule;
import org.netbeans.modules.css.lib.api.CssTokenId;
import org.netbeans.modules.css.lib.api.Node;
import org.netbeans.modules.css.lib.api.NodeType;
import org.netbeans.modules.css.lib.api.NodeVisitor;
import org.netbeans.modules.css.lib.api.properties.Properties;
import org.netbeans.modules.web.common.api.LexerUtils;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author mfukala@netbeans.org
 */
@ServiceProvider(service = CssEditorModule.class)
public class PagedMediaModule extends ExtCssEditorModule implements CssModule {

    private static final String PROPERTIES_DEFINITION_PATH = "org/netbeans/modules/css/editor/module/main/properties/paged_media"; //NOI18N
    private static final Collection<String> PAGE_PSEUDO_CLASSES = Arrays.asList(new String[]{"first", "left", "right"}); //NOI18N
    private static final Collection<String> PAGE_MARGIN_SYMBOLS =
            Arrays.asList(new String[]{
                "top-left-corner",
                "top-left",
                "top-center",
                "top-right",
                "top-right-corner",
                "bottom-left-corner",
                "bottom-left",
                "bottom-center",
                "bottom-right",
                "bottom-right-corner",
                "left-top",
                "left-middle",
                "left-bottom",
                "right-top",
                "right-middle",
                "right-bottom" //NOI18N
            });

  
    @Override
    public List<CompletionProposal> getCompletionProposals(CompletionContext context) {
        List<CompletionProposal> proposals = new ArrayList<>();
        
        Node activeNode = context.getActiveNode();
        //switch to first non error node
        loop:
        for (;;) {
            switch (activeNode.type()) {
                case error:
                case recovery:
                    activeNode = activeNode.parent();
                    break;
                default:
                    break loop;
            }
        }
        String prefix = context.getPrefix(); //default
        Token<CssTokenId> token = context.getTokenSequence().token();
        if (token == null) {
            return Collections.emptyList(); //empty file - no tokens
        }

        CssTokenId tokenId = token.id();
        switch (activeNode.type()) {
            case page:
                switch (tokenId) {
                    case IDENT:
                        if (context.getActiveTokenDiff() == 0 && LexerUtils.followsToken(context.getTokenSequence(),
                                CssTokenId.PAGE_SYM, true, true, CssTokenId.WS) != null) {
                            //just after page name: @page mypag|
                            prefix = "";
                            proposals.addAll(getPagePseudoClassCompletionProposals(context, true));
                        }
                        break;
                    case WS:
                        if (LexerUtils.followsToken(context.getTokenSequence(),
                                CssTokenId.LBRACE, true, true, CssTokenId.WS) != null) {
                            //inside the page rule body, no prefix: @page { | }
                            proposals.addAll(getPageMarginSymbolsCompletionProposals(context, true));
                            proposals.addAll(getPropertiesCompletionProposals(context));
                        }
                        break;
                    case PAGE_SYM: //just after @page keyword: @page|
                        proposals.addAll(getPagePseudoClassCompletionProposals(context, true));
                        break;
                    case AT_SIGN:
                        //@page { @|  }
                        if (token.text().charAt(0) == '@') {
                            proposals.addAll(getPageMarginSymbolsCompletionProposals(context, true));
                        }
                        break;
                }
                break;
            case pseudoPage:
            case bodyItem:
                switch (tokenId) {
                    case COLON: //just after colon: @page :|
                    case IDENT: //in the page pseudo class: @page:fir|
                        proposals.addAll(getPagePseudoClassCompletionProposals(context, false));
                        break;
                }
                break;
            case margin:
                switch (tokenId) {
                    case WS:
                        //no prefix in margin
                        proposals.addAll(getPropertiesCompletionProposals(context));
                        break;
                }
                break;
        }

        return Utilities.filterCompletionProposals(proposals, prefix, true);
    }

    private static List<CompletionProposal> getPageMarginSymbolsCompletionProposals(CompletionContext context, boolean addAtPrefix) {
        String prefix = addAtPrefix ? "@" : null;
        return Utilities.createRAWCompletionProposals(PAGE_MARGIN_SYMBOLS, ElementKind.FIELD, context.getAnchorOffset(), prefix);
    }

    private static List<CompletionProposal> getPagePseudoClassCompletionProposals(CompletionContext context, boolean addColonPrefix) {
        String prefix = addColonPrefix ? ":" : null;
        return Utilities.createRAWCompletionProposals(PAGE_PSEUDO_CLASSES, ElementKind.FIELD, context.getAnchorOffset(), prefix);
    }

    private static List<CompletionProposal> getPropertiesCompletionProposals(CompletionContext context) {
        return Utilities.wrapProperties(Properties.getPropertyDefinitions(context.getFileObject()), context.getAnchorOffset());
    }

    @Override
    public <T extends Map<OffsetRange, Set<ColoringAttributes>>> NodeVisitor<T> getSemanticHighlightingNodeVisitor(FeatureContext context, T result) {
        return new NodeVisitor<T>(result) {

            @Override
            public boolean visit(Node node) {
                switch (node.type()) {
                    case pseudoPage:
                        getResult().put(Css3Utils.getOffsetRange(node), ColoringAttributes.CLASS_SET);
                        break;
                }
                return false;
            }
        };
    }

    @Override
    public String getName() {
        return "paged_media"; //NOI18N
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(this.getClass(), Constants.CSS_MODULE_DISPLAYNAME_BUNDLE_KEY_PREFIX + getName());
    }

    @Override
    public String getSpecificationURL() {
        return "http://www.w3.org/TR/css3-page";
    }

    @Override
    protected String getPropertyDefinitionsResourcePath() {
        return PROPERTIES_DEFINITION_PATH;
    }

    @Override
    protected CssModule getCssModule() {
        return this;
    }
}
