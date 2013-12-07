/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.references;

import java.util.Collections;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

@Component
@Named(ReferenceMacro.MACRO_NAME)
@Singleton
public class ReferenceMacro extends AbstractMacro<ReferenceMacroParameters>
{
    /** The name of this macro. */
    public static final String MACRO_NAME = "reference";

    /** The description of the macro. */
    private static final String DESCRIPTION = "Generates a reference to display at the end of the page.";

    /** The description of the macro content. */
    private static final String CONTENT_DESCRIPTION = "the text to place in the reference";

    /**
     * Matches MacroBlocks having a macro id of {@link ReferencesMacro#MACRO_NAME}.
     */
    private static final MacroBlockMatcher MACRO_BLOCK_MATCHER = new MacroBlockMatcher(ReferencesMacro.MACRO_NAME);

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ReferenceMacro()
    {
        super("Reference", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            ReferenceMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public int getPriority()
    {
        return 500;
    }

    @Override
    public List<Block> execute(ReferenceMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Block root = context.getXDOM();

        Block matchingBlock = root.getFirstBlock(MACRO_BLOCK_MATCHER, Block.Axes.DESCENDANT);
        if (matchingBlock != null) {
            return Collections.emptyList();
        }

        Block putFootnotesMacro =
            new MacroBlock(ReferencesMacro.MACRO_NAME, Collections.<String, String> emptyMap(), false);
        root.addChild(putFootnotesMacro);

        return Collections.emptyList();
    }
}
