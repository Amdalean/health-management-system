//package com.hms.task;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//import nc.bs.logging.Logger;
//import nc.bs.pub.pa.PreAlertObject;
//import nc.bs.pub.taskcenter.BgWorkingContext;
//import nc.bs.pub.taskcenter.IBackgroundWorkPlugin;
//import nc.util.sip.CommonUtil;
//import nc.util.sip.HttpConnection;
//import nc.util.sip.JdbcUtil;
//import nc.util.sip.MDUtil;
//import nc.util.sip.UtilConstants.MaillSender;
//import nc.vo.pub.BusinessException;
//import nc.vo.pub.lang.UFDate;
//import nc.vo.pub.lang.UFDateTime;
//import nccloud.message.util.MessageCenter;
//import nccloud.message.vo.NCCMessage;
//import nccloud.message.vo.NCCNoticeMessageVO;
//import nccloud.util.email.EmailUtil;
//
//
///**
// * WeatherReminderTaskPlugin：天气预报邮件通知插件
// * @author	CYQ
// * @date	2023年5月20日 下午5:05:14
// * @version	1.0.0
//*/
//public class WeatherReminderTaskPlugin {
//
//	private CommonUtil util = CommonUtil.getCommonUtil();
//	@Override
//	public PreAlertObject executeTask(BgWorkingContext context) throws BusinessException {
//		try {
//			String msg = sendEmail();
//
//			//通过系统内置能力，调用消息通知，请忽略此功能
//			sendMsg("CYQ",msg);
//
//			context.setLogStr("天气预报邮件通知任务调用完成！");
//			return null;
//		} catch (Exception e) {
//			throw new BusinessException("发生了未定义异常,"+e.getMessage());
//		}
//	}
//	/**
//	 * sendEmail：	发送邮件
//	 * @param data	void	TODO（参数说明）
//	 * 创  建  人 ：CYQ
//	 * 创建时间：2023年5月20日-下午5:53:50
//	 * @throws Exception
//	*/
//	private String sendEmail() throws Exception {
//		Logger.error("begin...sendEmail");
//		//获取明天天气
//		String weather = getGDAPI();
//
//		//通过gpt3.5获取信息
//		String gptbody = getGPTbody(weather);
//
//		String html = MDUtil.mdtohtml(gptbody);
//		//发送邮件
//		EmailUtil.sendEmail(MaillSender.we, "明日天气预报，请查收~", html);
//
//		return "天气预报发送完毕";
//	}
//	/**
//	 * 获取明天的天气
//	 * @throws BusinessException
//	 */
//	private String getGDAPI() throws BusinessException {
//		//url为高德天气预报服务的url，详情请参阅：https://console.amap.com/dev/key/app
//		String url = util.getParameter("gd_url");
//		String msg = HttpConnection.doGet(url, null, null);
//		Logger.error("高德返回消息："+msg);
//		Map map = util.initMap(msg);
//		String infocode = util.initstr(map.get("infocode"));
//		if(!"10000".equals(infocode)) {
//			throw new BusinessException("高德请求异常,"+msg);
//		}
//		JSONArray forecasts = (JSONArray)JSON.parse(util.initstr(map.get("forecasts")));
//		Map info = util.initMap(forecasts.get(0));
//		JSONArray casts = (JSONArray)JSON.parse(util.initstr(info.get("casts")));
//		String tomorrow = util.initstr(casts.get(1));
//		return tomorrow;
//	}
//	/**
//	 * 调用GPT3.5中转服务，获取报文
//	 * Edit by CYQ 2023年8月11日 更改服务商为阿里，通义千问
//	 * @param body
//	 * @return
//	 * @throws BusinessException
//	 */
//	private String getGPTbody(String data) throws BusinessException {
//		Logger.error("begin...getGPTbody");
//		//请将XX，YY替换为身份和昵称
//		String req = "我的昵称:崔崔;收信人昵称:肉崽崽;收信人身份:女朋友;请用中文讲述天气及注意事项，并问好。生成内容请遵循markdown语法。今天是"+new UFDate()+"，以下是明天的天气信息："+data;
//		JSONObject json = new JSONObject();
//		json.put("model", "qwen-v1");
//		JSONObject input = new JSONObject();
//
//		input.put("prompt", req);
//		json.put("input", input);
//
//
//
//		String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation?version-id=v1&task-group=aigc&task=text-generation&function-call=generation";
//		HashMap<String,String> head = new HashMap();
//		head.put("Authorization", "sk-d6e71773670f408c9b96e186c6e2dae4");
//		String msg = HttpConnection.dopost_OKHttp(url, json.toString(), head);
//		/**
//		 *
//		StringBuffer sb = new StringBuffer(msg);
//		sb.delete(sb.length()-1, sb.length());
//		sb.delete(0, 1);
//
//		//解码unicode字符
//		String text = unicodeDecode(sb.toString());
//		Logger.error("getGPTbody...msg="+text);
//		 * */
//		Map map = util.initMap(msg);
//		String code = util.initstr(map.get("code"));
//		if(!"".equals(code)) {
//			throw new BusinessException("调用通义千问异常，异常消息为，"+msg);
//		}
//		Map output = util.initMap(map.get("output"));
//		String text = util.initstr(output.get("text"));
//		return text;
//	}
//    /**
//     * @param string
//     * @return 转换之后的内容
//     * @Title: unicodeDecode
//     * @Description: unicode解码 将Unicode的编码转换为中文
//     */
//    private String unicodeDecode(String string) {
//        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
//        Matcher matcher = pattern.matcher(string);
//        char ch;
//        while (matcher.find()) {
//            ch = (char) Integer.parseInt(matcher.group(2), 16);
//            string = string.replace(matcher.group(1), ch + "");
//        }
//        return string;
//    }
//
//	/**
//	 * 失败信息发送指定业务员
//	 *
//	 * @MethodName: sendMsg
//	 * memo by CYQ 2023年4月26日 理论代码没问题，前台不显示通知，怀疑是标准bug
//	 * @author CYQ
//	 * @date 2023年5月20日
//	 */
//	private void sendMsg(String msg_users, String msg) throws BusinessException {
//		try {
//			if (msg_users == null || msg_users.isEmpty()) {
//				throw new BusinessException("未加载到有效的[msg_users]参数,请检查!");
//			}
//			NCCMessage message = new NCCMessage();
//			NCCNoticeMessageVO msgvo = new NCCNoticeMessageVO();
//			// 消息标题内容
//			msgvo.setSubject(msg);
//			msgvo.setSender("NC_USER0000000000000");
//			// 可以一次群发,发送人
//			String pk = JdbcUtil.queryColumn("sm_user", "cuserid", "user_code", msg_users);
//			msgvo.setReceiver(pk);
//			// 消息内容
//			msgvo.setContent(msg);
//			msgvo.setMsgsourcetype("reconcilemeg");
//			msgvo.setSendtime(new UFDateTime());
////			msgvo.setDetail(msg);
//			msgvo.setContenttype("BIZ");// 内容格式
//			msgvo.setMsgtype("nc");// 消息发送类型
//			msgvo.setMsgsourcetype("notice");// 消息来源类型
//			msgvo.setPriority(0);// 优先级
//			msgvo.setSendtime(new UFDateTime());// 发送时间
//			message.setMessage(msgvo);
//
//			message.setMessageType("notice");// 消息类型——通知
//			// 发送确认消息
//			MessageCenter.sendMessage(new NCCMessage[] { message });
//
//		} catch (Exception e) {
//			throw new BusinessException("业务执行完毕，NC消息发送异常..." + e.getMessage());
//		}
//	}
//
//
//}
