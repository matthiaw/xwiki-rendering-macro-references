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

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

@Component
@Named(ReferencesMacro.MACRO_NAME)
@Singleton
public class ReferencesMacro extends AbstractMacro<ReferenceMacroParameters> {
	/** The name of this macro. */
	public static final String MACRO_NAME = "references";

	/** The description of the macro. */
	private static final String DESCRIPTION = "Displays the references defined so far." + " If missing, all references are displayed by default at the end of the page.";

	/** ID attribute name. */
	private static final String ID_ATTRIBUTE_NAME = "id";

	/** CSS Class attribute name. */
	private static final String CLASS_ATTRIBUTE_NAME = "class";

	/** Prefix for the ID of the reference link to the footnote. */
	private static final String FOOTNOTE_ID_PREFIX = "x_reference_";

	/** Prefix for the ID of the footnote. */
	private static final String FOOTNOTE_REFERENCE_ID_PREFIX = "x_reference_pre_";

	/**
	 * Used to parse the content of the macro.
	 */
	@Inject
	private MacroContentParser contentParser;

	/**
	 * Create and initialize the descriptor of the macro.
	 */
	public ReferencesMacro() {
		super("Put References", DESCRIPTION, ReferenceMacroParameters.class);
		setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
	}

	@Override
	public boolean supportsInlineMode() {
		return false;
	}

	private int referenceCounter;

	@Override
	public List<Block> execute(ReferenceMacroParameters parameters, String content, MacroTransformationContext context) throws MacroExecutionException {
		List<Block> result = Collections.emptyList();

		// Get the list of footnotes in the document
		Block root = context.getXDOM();
		List<MacroMarkerBlock> footnotes = root.getBlocks(new ClassBlockMatcher(MacroMarkerBlock.class), Block.Axes.DESCENDANT);
		for (ListIterator<MacroMarkerBlock> it = footnotes.listIterator(); it.hasNext();) {
			MacroMarkerBlock macro = it.next();
			if (ReferenceMacro.MACRO_NAME.equals(macro.getId())) {
				continue;
			} else if (ReferencesMacro.MACRO_NAME.equals(macro.getId())) {
				macro.getParent().replaceChild(Collections.<Block> emptyList(), macro);
			}
			it.remove();
		}
		if (footnotes.size() <= 0) {
			return result;
		}

		NumberedListBlock container = new NumberedListBlock(Collections.<Block> emptyList());
		container.setParameter(CLASS_ATTRIBUTE_NAME, "references");

		references = new ArrayList<Reference>();
		referenceCounter = 1;

		for (MacroMarkerBlock footnote : footnotes) {

			boolean add = true;
			Reference targetRef = new Reference();
			for (Reference reference : references) {
				if (reference.getContent().equals(footnote.getContent())) {
					targetRef = reference;
					add = false;
				}
			}

			if (targetRef != null) {
				targetRef.add(footnote);
				if (add) {
					targetRef.setId(referenceCounter);
					references.add(targetRef);
					referenceCounter++;
				}
			}
		}

		for (Reference reference : references) {
			reference.parseReferences();
			container.addChild(reference.createBlock(context));
		}

		return Collections.<Block> singletonList(container);
	}

	private class Reference {
		public Reference() {
			footerContent = new String();
			refs = new ArrayList<MacroMarkerBlock>();
		}

		private String footerContent;
		private int id;

		public void setId(int id) {
			this.id = id;
		}

		public void parseReferences() {
			char letter = '^';
			
			if (refs.size()>1) {
				letter = 'a';
			}
			
			for (MacroMarkerBlock ref : refs) {
				if (ref != null) {
					Block block = createFootnoteBlock(letter);
					addFootnoteRef(ref, block);
					letter++;
				}
			}
		}

		private void addFootnoteRef(MacroMarkerBlock footnoteMacro, Block footnoteRef) {
			for (ListIterator<Block> it = footnoteMacro.getChildren().listIterator(); it.hasNext();) {
				Block b = it.next();
				it.remove();
			}
			footnoteMacro.addChild(footnoteRef);
		}

		private Block createFootnoteBlock(char letter) {
			Block result = new WordBlock(id + "");
			DocumentResourceReference reference = new DocumentResourceReference(null);
			reference.setAnchor(FOOTNOTE_ID_PREFIX + id+letter);
			result = new LinkBlock(Collections.singletonList(result), reference, false);
			result = new FormatBlock(Collections.singletonList(result), Format.SUPERSCRIPT);
			result.setParameter(ID_ATTRIBUTE_NAME, FOOTNOTE_REFERENCE_ID_PREFIX + id+letter);
			result.setParameter(CLASS_ATTRIBUTE_NAME, "footnoteRef");
			return result;
		}

		public void add(MacroMarkerBlock footnoteMacro) {
			String content = footnoteMacro.getContent();
			if (StringUtils.isBlank(content)) {
				content = " ";
			}

			if (!footerContent.equals(content)) {
				footerContent = content;
			}

			refs.add(footnoteMacro);
		}

		public ListItemBlock createBlock(MacroTransformationContext context) throws MacroExecutionException {

			List<Block> parsedContent;
			try {
				parsedContent = contentParser.parse(footerContent, context, false, true).getChildren();
			} catch (MacroExecutionException e) {
				parsedContent = Collections.<Block> singletonList(new WordBlock(footerContent));
			}

			List<Block> links = new ArrayList<Block>();
			char letter = '^';
			
			if (refs.size()>1) {
				letter = 'a';
			}
			for (MacroMarkerBlock ref : refs) {
				if (ref != null) {
					Block start = new WordBlock("" + letter + " ");
					DocumentResourceReference reference = new DocumentResourceReference(null);
					reference.setAnchor(FOOTNOTE_REFERENCE_ID_PREFIX + id+letter);
					Block result = new LinkBlock(Collections.singletonList(start), reference, false);
					result = new FormatBlock(Collections.singletonList(result), Format.SUPERSCRIPT);
					result.setParameter(ID_ATTRIBUTE_NAME, FOOTNOTE_ID_PREFIX + id+letter);
					result.setParameter(CLASS_ATTRIBUTE_NAME, "footnoteBackRef");
					links.add(result);
					letter++;
				}
			}

			ListItemBlock listItem = new ListItemBlock(links);
			listItem.addChild(new SpaceBlock());
			listItem.addChildren(parsedContent);
			listItem.setParameter(CLASS_ATTRIBUTE_NAME, ReferenceMacro.MACRO_NAME);

			return listItem;
		}

		public String getContent() {
			return footerContent;
		}

		/**
		 * List of References in Text
		 */
		private List<MacroMarkerBlock> refs;
	}

	/**
	 * List of Footer-References
	 */
	private List<Reference> references;

}
