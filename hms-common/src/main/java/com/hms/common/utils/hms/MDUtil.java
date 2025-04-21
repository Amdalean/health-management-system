package com.hms.common.utils.hms;

import com.hms.common.annotation.Log;
import com.hms.common.enums.BusinessType;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MDUtil {

    @Log(title = "md文件转html", businessType = BusinessType.OTHER)
	public static String mdtohtml(String md) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
	}
}
