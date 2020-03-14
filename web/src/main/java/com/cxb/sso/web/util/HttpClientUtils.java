package com.cxb.sso.web.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * HttpClient工具类
 */
public class HttpClientUtils {

	// utf-8字符编码
	private static final String CHARSET_UTF_8 = "utf-8";

	// HTTP内容类型。
	private static final String CONTENT_TYPE_TEXT_HTML = "text/xml";

	// HTTP内容类型。相当于form表单的形式，提交数据
	private static final String CONTENT_TYPE_FORM_URL = "application/x-www-form-urlencoded";

	// HTTP内容类型。相当于form表单的形式，提交数据
	private static final String CONTENT_TYPE_JSON_URL = "application/json;charset=utf-8";

	public static enum HTTP_TYPE {GET, POST}

	// 连接管理器
	private static PoolingHttpClientConnectionManager pool;

	// 请求配置
	private static RequestConfig requestConfig;

	static {

		try {
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					builder.build());
			// 配置同时支持 HTTP 和 HTPPS
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register(
					"http", PlainConnectionSocketFactory.getSocketFactory()).register(
					"https", sslsf).build();
			// 初始化连接管理器
			pool = new PoolingHttpClientConnectionManager(
					socketFactoryRegistry);
			// 将最大连接数增加到200，实际项目最好从配置文件中读取这个值
			pool.setMaxTotal(200);
			// 设置最大路由
			pool.setDefaultMaxPerRoute(2);
			// 根据默认超时限制初始化requestConfig
			int socketTimeout = 10000;
			int connectTimeout = 10000;
			int connectionRequestTimeout = 10000;
			requestConfig = RequestConfig.custom().setConnectionRequestTimeout(
					connectionRequestTimeout).setSocketTimeout(socketTimeout).setConnectTimeout(
					connectTimeout).build();

			//System.out.println("初始化HttpClientTest~~~结束");
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		} catch (KeyStoreException e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		} catch (KeyManagementException e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}

		// 设置请求超时时间
		requestConfig = RequestConfig.custom().setSocketTimeout(50000).setConnectTimeout(50000)
				.setConnectionRequestTimeout(50000).build();
	}

	private static CloseableHttpClient getHttpClient() {

		CloseableHttpClient httpClient = HttpClients.custom()
				// 设置连接池管理
				.setConnectionManager(pool)
				// 设置请求配置
				.setDefaultRequestConfig(requestConfig)
				// 设置重试次数
				.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
				.build();

		return httpClient;
	}

	/**
	 * 发送 post请求
	 *
	 * @param httpUrl 地址
	 */
	public static String sendHttpPost(String httpUrl) {
		// 创建httpPost
		HttpPost httpPost = new HttpPost(httpUrl);
		return sendHttp(httpPost, HTTP_TYPE.POST);
	}

	/**
	 * 发送 get请求
	 *
	 * @param httpUrl 地址
	 */
	public static String sendHttpGet(String httpUrl) {
		// 创建get请求
		HttpGet httpGet = new HttpGet(httpUrl);
		return sendHttp(httpGet, HTTP_TYPE.GET);
	}


	/**
	 * 发送 post请求（带文件）
	 *
	 * @param httpUrl   地址
	 * @param maps      参数
	 * @param fileLists 附件
	 */
	public static String sendHttpPost(String httpUrl, Map<String, String> maps, List<File> fileLists) {
		// 创建httpPost
		HttpPost httpPost = new HttpPost(httpUrl);
		MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
		if (maps != null) {
			for (String key : maps.keySet()) {
				meBuilder.addPart(key, new StringBody(maps.get(key), ContentType.TEXT_PLAIN));
			}
		}
		if (fileLists != null) {
			for (File file : fileLists) {
				FileBody fileBody = new FileBody(file);
				meBuilder.addPart("files", fileBody);
			}
		}
		HttpEntity reqEntity = meBuilder.build();
		httpPost.setEntity(reqEntity);
		return sendHttp(httpPost, HTTP_TYPE.POST);
	}

	/**
	 * 发送 post请求
	 *
	 * @param httpUrl 地址
	 * @param params  参数(格式:key1=value1&key2=value2)
	 */
	public static String sendHttpPostForm(String httpUrl, String params) {
		return postData(httpUrl, params, CONTENT_TYPE_FORM_URL);
	}

	/**
	 * 发送 post请求
	 *
	 * @param maps 参数
	 */
	public static String sendHttpPostMap(String httpUrl, Map<String, String> maps) {
		String param = convertStringParameter(maps);
		return sendHttpPostForm(httpUrl, param);
	}


	/**
	 * 发送 post请求 发送json数据
	 *
	 * @param httpUrl    地址
	 * @param paramsJson 参数(格式 json)
	 */
	public static String sendHttpPostJson(String httpUrl, String paramsJson) {
		return postData(httpUrl, paramsJson, CONTENT_TYPE_JSON_URL);
	}

	/**
	 * 发送 带Head的post JSON数据
	 *
	 * @param httpUrl    地址
	 * @param paramsJson 参数(格式 json)
	 */
	public static String sendHttpPostJson(String httpUrl, Map<String, String> headerMap, String paramsJson) {
		return postData(httpUrl, headerMap, paramsJson, CONTENT_TYPE_JSON_URL);
	}

	/**
	 * 发送 post请求 发送xml数据
	 *
	 * @param httpUrl   地址
	 * @param paramsXml 参数(格式 Xml)
	 */
	public static String sendHttpPostXml(String httpUrl, String paramsXml) {
		return postData(httpUrl, paramsXml, CONTENT_TYPE_TEXT_HTML);
	}

	/**
	 * 发送Post请求
	 *
	 * @param httpObject http对象
	 * @return 字符串
	 */
	private static String sendHttp(Object httpObject, HTTP_TYPE httpType) {

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		// 响应内容
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			httpClient = getHttpClient();

			if (httpType == HTTP_TYPE.GET) {
				HttpGet httpGet = (HttpGet) httpObject;
				// 配置请求信息
				httpGet.setConfig(requestConfig);
				// 执行请求
				response = httpClient.execute(httpGet);
			} else {
				HttpPost httpPost = (HttpPost) httpObject;
				// 配置请求信息
				httpPost.setConfig(requestConfig);
				// 执行请求
				response = httpClient.execute(httpPost);
			}
			// 得到响应实例
			HttpEntity entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, CHARSET_UTF_8);
			EntityUtils.consume(entity);
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			try {
				// 释放资源
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				//e.printStackTrace();
				System.err.println(e.getMessage());
			}
		}
		return responseContent;
	}

	private static String postData(String httpUrl, Map<String, String> headerMap, String params, String contentType) {
		HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
		try {
			// 设置参数
			for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				httpPost.setHeader(entry.getKey(), entry.getValue());
			}
			if (params != null && params.trim().length() > 0) {
				StringEntity stringEntity = new StringEntity(params, "UTF-8");
				stringEntity.setContentType(contentType);
				httpPost.setEntity(stringEntity);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return sendHttp(httpPost, HTTP_TYPE.POST);
	}

	/**
	 * 发送数据
	 *
	 * @param httpUrl     地址
	 * @param params      参数
	 * @param contentType 类型
	 * @return 字符串
	 */
	private static String postData(String httpUrl, String params, String contentType) {
		HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
		try {
			// 设置参数
			if (params != null && params.trim().length() > 0) {
				StringEntity stringEntity = new StringEntity(params, "UTF-8");
				stringEntity.setContentType(contentType);
				httpPost.setEntity(stringEntity);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return sendHttp(httpPost, HTTP_TYPE.POST);
	}

	/**
	 * 将map集合的键值对转化成：key1=value1&key2=value2 的形式
	 *
	 * @param parameterMap 需要转化的键值对集合
	 * @return 字符串
	 */
	private static String convertStringParameter(Map parameterMap) {
		StringBuffer parameterBuffer = new StringBuffer();
		if (parameterMap != null) {
			Iterator iterator = parameterMap.keySet().iterator();
			String key = null;
			String value = null;
			while (iterator.hasNext()) {
				key = (String) iterator.next();
				if (parameterMap.get(key) != null) {
					value = (String) parameterMap.get(key);
				} else {
					value = "";
				}
				parameterBuffer.append(key).append("=").append(value);
				if (iterator.hasNext()) {
					parameterBuffer.append("&");
				}
			}
		}
		return parameterBuffer.toString();
	}


}