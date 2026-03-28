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
    public static void main(String[] args) {
        String msg = "\n" +
                "**问候：**\n" +
                "下午好，多肉小姐。\n" +
                "\n" +
                "**天气情况：**\n" +
                "明日在厦门市湖里区，白天天气多云，夜晚也是多云。白天最高温度18℃，夜间最低温度11℃。白天风向东北风，风力等级1 - 3级；夜间同样为东北风，风力等级1 - 3级。\n" +
                "\n" +
                "**注意事项：**\n" +
                "温差较大，请注意适时增减衣物以防感冒。外出时可携带外套应对早晚的低温。祝您出勤顺利。\n" +
                "";
        String newmsg = removeMarkdownTags(msg);
        System.out.println(newmsg);
        System.out.println("输出：");
        System.out.println(MDUtil.mdtohtml(newmsg));
    }
    @Log(title = "转换md文件", businessType = BusinessType.OTHER)
    public static String removeMarkdownTags(String s) {
        String startTag = "```markdown";
        String endTag = "```";

        if (s != null && s.startsWith(startTag) && s.endsWith(endTag)) {
            int startIndex = startTag.length();
            int endIndex = s.length() - endTag.length();
            return s.substring(startIndex, endIndex);
        }
        return s;
    }
}
