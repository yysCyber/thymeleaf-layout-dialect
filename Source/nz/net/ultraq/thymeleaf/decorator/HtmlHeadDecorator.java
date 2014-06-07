/*
 * Copyright 2013, Emanuel Rabina (http://www.ultraq.net.nz/)
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

package nz.net.ultraq.thymeleaf.decorator;

import static nz.net.ultraq.thymeleaf.LayoutDialect.DIALECT_PREFIX_LAYOUT;
import static nz.net.ultraq.thymeleaf.LayoutUtilities.*;
import static nz.net.ultraq.thymeleaf.decorator.TitlePatternProcessor.CONTENT_TITLE;
import static nz.net.ultraq.thymeleaf.decorator.TitlePatternProcessor.DECORATOR_TITLE;
import static nz.net.ultraq.thymeleaf.decorator.TitlePatternProcessor.PROCESSOR_NAME_TITLEPATTERN;

import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;
import org.thymeleaf.dom.Text;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.attr.StandardTextAttrProcessor;
import org.thymeleaf.standard.processor.attr.StandardUtextAttrProcessor;

/**
 * A decorator specific to processing an HTML HEAD element.
 * 
 * @author Emanuel Rabina
 */
public class HtmlHeadDecorator extends XmlElementDecorator {

	/**
	 * Decorate the HEAD part.  This step replaces the decorator's TITLE element
	 * if the content has one, and appends all other content elements to the
	 * HEAD section, after all the decorator elements.
	 * 
	 * @param decoratorhtml Decorator's HTML element.
	 * @param contenthead	Content's HEAD element.
	 */
	@Override
	public void decorate(Element decoratorhtml, Element contenthead) {

		// If the decorator has no HEAD, then we can just use the content HEAD
		Element decoratorhead = findElement(decoratorhtml, HTML_ELEMENT_HEAD);
		if (decoratorhead == null) {
			if (contenthead != null) {
				decoratorhtml.insertChild(0, new Text(LINE_SEPARATOR));
				decoratorhtml.insertChild(1, contenthead);

				Element contenttitle = findElement(contenthead, HTML_ELEMENT_TITLE);
				if (contenttitle != null) {
					Element resultingtitle = new Element(HTML_ELEMENT_TITLE);
					extractTitle(contenthead, contenttitle, CONTENT_TITLE, resultingtitle);
					contenthead.insertChild(0, new Text(LINE_SEPARATOR));
					contenthead.insertChild(1, resultingtitle);
				}
			}
			return;
		}

		// Merge the content and decorator titles into a single title element
		Element decoratortitle = findElement(decoratorhead, HTML_ELEMENT_TITLE);
		Element contenttitle   = null;
		if (contenthead != null) {
			contenttitle = findElement(contenthead, HTML_ELEMENT_TITLE);
		}
		Element resultingtitle = null;
		if (decoratortitle != null || contenttitle != null) {
			resultingtitle = new Element(HTML_ELEMENT_TITLE);
			if (decoratortitle != null) {
				extractTitle(decoratorhead, decoratortitle, DECORATOR_TITLE, resultingtitle);
			}
			if (contenttitle != null) {
				extractTitle(contenthead, contenttitle, CONTENT_TITLE, resultingtitle);
			}

			// If there's a title pattern, get rid of all other text setters so
			// they don't interfere with it
			if (hasAttribute(resultingtitle, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN)) {
				removeAttribute(resultingtitle, StandardDialect.PREFIX, StandardTextAttrProcessor.ATTR_NAME);
				removeAttribute(resultingtitle, StandardDialect.PREFIX, StandardUtextAttrProcessor.ATTR_NAME);
			}
		}

		// Append the content's HEAD elements to the end of the decorator's HEAD
		// section, placing the resulting title at the beginning of it
		if (contenthead != null) {
			for (Node contentheadnode: contenthead.getChildren()) {
				decoratorhead.addChild(contentheadnode);
			}
		}
		if (resultingtitle != null) {
			decoratorhead.insertChild(0, new Text(LINE_SEPARATOR));
			decoratorhead.insertChild(1, resultingtitle);
		}

		super.decorate(decoratorhead, contenthead);
	}

	/**
	 * Extract the title from the given TITLE element, whether it be the text
	 * in the tag body or a th:text in the attributes.
	 * 
	 * @param head     HEAD tag containing <tt>title</tt>.
	 * @param title    TITLE tag from which to extract the title.
	 * @param titlekey Key to store the title to as a node property in
	 *                 <tt>result</tt>.   
	 * @param result   The new TITLE element being constructed.
	 */
	private static void extractTitle(Element head, Element title, String titlekey, Element result) {

		// Make the result look like the title
		Text titletext = (Text)title.getFirstChild();
		result.clearChildren();
		result.addChild(titletext);

		// Extract any text or processors from the title element's attributes
		if (hasAttribute(title, StandardDialect.PREFIX, StandardUtextAttrProcessor.ATTR_NAME)) {
			result.setNodeProperty(titlekey, getAttributeValue(title,
					StandardDialect.PREFIX, StandardUtextAttrProcessor.ATTR_NAME));
		}
		else if (hasAttribute(title, StandardDialect.PREFIX, StandardTextAttrProcessor.ATTR_NAME)) {
			result.setNodeProperty(titlekey, getAttributeValue(title,
					StandardDialect.PREFIX, StandardTextAttrProcessor.ATTR_NAME));
		}

		// Extract text from a previously set value (deep hierarchies)
		else if (title.hasNodeProperty(titlekey)) {
			result.setNodeProperty(titlekey, title.getNodeProperty(titlekey));
		}

		// Extract text from the title element's content
		else if (titletext != null) {
			result.setNodeProperty(titlekey, titletext.getContent());
		}

		pullAttributes(result, title);
		head.removeChild(title);
	}
}
