package com.hms.common.utils.hms;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;


/**
 * CommonUtil：通用工具类
 * @author	CYQ
 * @date	2021年11月30日 上午10:31:03
 * @version	1.0.0
*/ 
public class CommonUtil {
	private static CommonUtil util = new CommonUtil();
	
	/**
	 * delete by CYQ 2023年7月27日为了允许继承，不再控制单例
	 * 	private CommonUtil(){}
	 * */
	public static CommonUtil getCommonUtil() {
		return util;
	}
	private static Properties props = null ;//配置文件

	
	public static String initstr(Object str){
		return (str == null ? "" : str).toString();
	}
	public static Integer initInteger(Object data){
		if(data == null) {
			return null;
		}
		String dataStr = initstr(data);
		if("".equals(dataStr)) {
			return null;
		}
		return new Integer(dataStr);
	}
	/**
	 * checkJson：	传入需要必填校验的字符
	 * @param strs	void	TODO（参数说明）
	 * 创  建  人 ：CYQ
	 * 创建时间：2021年12月2日-上午10:13:47
	 * @throws Exception
	*/
	public static void checkNull(String[] strs,Map data) throws Exception {
		if(strs==null||strs.length==0) {
			return;
		}
		if(data==null||data.size()==0) {
			return;
		}
		StringBuffer errStr = new StringBuffer();
		String value;
		for(String key:strs) {
			value = initstr(data.get(key));
			if("".equals(value)) {
				errStr.append("["+key+"]");
			}
		}
		if(!"".equals(initstr(errStr))) {
			throw new Exception("以下字段不能为空"+errStr+"!");
		}
	}
	/** JSON格式的String转MAP
	 * @throws Exception */
	public static Map initMap(Object str) throws Exception{
		try {
			return JSON.parseObject(initstr(str), HashMap.class);
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("初始化JSON失败,"+e.getMessage());
		}
	}
//	/** JSON格式的String转MAP
//	 * @throws Exception */
//	public static Map initMap(Object str) throws Exception{
//		try {
//			return JSON.parseObject(initstr(str), HashMap.class);
//		} catch (Exception e) {
//			// TODO: handle exception
//			throw new Exception("初始化JSON失败,"+e.getMessage());
//		}
//	}
	/**
	 * 读取配置文件
	 * getWSAddress：	
	 * @return	Properties	TODO（参数说明）
	 * 创  建  人 ：lihy
	 * 创建时间：2018年8月27日-下午1:25:23
	 * @throws Exception
	 */
	public static Properties getWSAddress() throws Exception {
		props = new Properties();
		String filePath = "/resources/sip/sipconfig.properties";
		InputStream in = null ;
			try {
				in = new BufferedInputStream (new FileInputStream(filePath));
				props.load(in);
	       } catch (IOException e) {
				throw new Exception("配置文件加载异常,"+e.getMessage());
			}
			return props;
	}
	
	/**
	 * 获取配置文件的参数
	 * getParameter：	
	 * @param key
	 * @return	String	TODO（参数说明）
	 * 创  建  人 ：lihy
	 * 创建时间：2018年8月27日-下午1:26:20
	 * @throws Exception
	 */
	public synchronized static String getParameter(String key) throws Exception{
		if(props != null ){
			String value = props.getProperty(key);
			if(value == null || value.isEmpty()) {
				throw new Exception("根据配置文件未获取到有效的参数【"+key+"】");
			}
			return props.getProperty(key);
		} else {
			return getWSAddress().getProperty(key);
		}
	}
	/**
	 * 前台界面输出异常
	 * 创  建  人 ：CYQ
	 * 创建时间：2022年4月20日-下午12:25:02
	 */
	public static void writeMsg(HttpServletResponse response,String msg) throws IOException {
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json; charset=utf-8");
		PrintWriter writer = response.getWriter();
		writer.write(msg);
	}
	
	/**
	 * decodeURL：	URL解码
	 * 创  建  人 ：CYQ
	 * 创建时间：2023年3月4日
	*/
	public static String decodeURL(Object s) throws Exception {
		try {
			
			String decode = URLDecoder.decode(CommonUtil.initstr(s),"UTF-8");
			return decode;
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("decodeURL异常,"+e.getMessage());
		}
	}
	/**
	 * ecodeURL：	URL编码
	 * 创  建  人 ：CYQ
	 * 创建时间：2023年3月4日
	*/
	public static String encodeURL(Object s) throws Exception {
		try {
			String encode = URLEncoder.encode(CommonUtil.initstr(s),"UTF-8");
			return encode;
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("encodeURL异常,"+e.getMessage());
		}
	}
}
