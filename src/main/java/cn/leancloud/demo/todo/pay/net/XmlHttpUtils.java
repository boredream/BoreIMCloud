package cn.leancloud.demo.todo.pay.net;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class XmlHttpUtils {

    private static final String CHARSET = "UTF-8";

    public static String postString(String url, Map<String, String> postParams) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        map.put("Content-Type", "text/xml");

        URL parsedUrl = new URL(url);
        HttpURLConnection connection = openConnection(parsedUrl);

        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }

        setConnectionParametersForRequest(connection, postParams);
        // Initialize HttpResponse with data from the HttpURLConnection.
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could
            // not be retrieved.
            // Signal to the caller that something was wrong with the
            // connection.
            throw new IOException(
                    "Could not retrieve response code from HttpUrlConnection.");
        }
        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                connection.getResponseCode(), connection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));
        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue()
                        .get(0));
                response.addHeader(h);

            }
        }

        Header contentTypeHeader = response.getHeaders(HTTP.CONTENT_TYPE)[0];
        String responseCharset = parseCharset(contentTypeHeader);

        byte[] bytes = entityToBytes(response.getEntity());
        String responseContent = new String(bytes, responseCharset);
        return responseContent;
    }

    /**
     * Returns the charset specified in the Content-Type of this header, or the
     * HTTP default (utf-8) if none can be found.
     */
    private static String parseCharset(Header contentTypeHeader) {
        String contentType = contentTypeHeader.getValue();
        if (contentType != null) {
            String[] params = contentType.split(";");
            for (int i = 1; i < params.length; i++) {
                String[] pair = params[i].trim().split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("charset")) {
                        return pair[1];
                    }
                }
            }
        }

        return "utf-8";
    }

    private static byte[] entityToBytes(HttpEntity entity) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            InputStream in = entity.getContent();
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
        } finally {
            bytes.close();
        }
        return bytes.toByteArray();
    }

    /**
     * Initializes an {@link HttpEntity} from the given
     * {@link HttpURLConnection}.
     *
     * @param connection
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     *
     * @param url
     * @return an open connection
     * @throws IOException
     */
    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(20 * 1000);
        connection.setReadTimeout(20 * 1000);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // // use caller-provided custom SslSocketFactory, if any, for HTTPS
        // if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
        // ((HttpsURLConnection)connection).setSSLSocketFactory(mSslSocketFactory);
        // }

        return connection;
    }

    private static byte[] encodeParameters(Map<String, String> params,
                                           String paramsEncoding) {
        //<xml>
        //    <appid>wxd930ea5d5a258f4f</appid>
        //    <mch_id>10000100</mch_id>
        //    <device_info>1000<device_info>
        //    <body>test</body>
        //    <nonce_str>ibuaiVcKdpRxkhJA</nonce_str>
        //    <sign>9A0A8659F005D6984697E2CA0A9CF3B7</sign>
        //<xml>
        try {
            StringBuilder encodedParams = new StringBuilder();
            encodedParams.append("<xml>");
            for (Entry<String, String> entry : params.entrySet()) {
                String param = String.format("<%s>%s</%s>",
                        entry.getKey(), entry.getValue(), entry.getKey());
                encodedParams.append(param);
            }
            encodedParams.append("</xml>");
            String xml = encodedParams.toString();
            System.out.println("wx prepare request xml = " + xml);
            return xml.getBytes(paramsEncoding);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: "
                    + paramsEncoding, uee);
        }
    }

    /**
     * 将xml字符串转换成map
     */
    public static Map<String, String> readStringXmlOut(String xml) throws ParserConfigurationException, IOException, SAXException {
        StringReader sr = new StringReader(xml);
        InputSource is = new InputSource(sr);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder=factory.newDocumentBuilder();
        Document doc = builder.parse(is);

        Map<String, String> map = new HashMap<>();
        Element rootElt = doc.getDocumentElement(); // 获取根节点
        NodeList nodeList = rootElt.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String nodeName = node.getNodeName();
            if(nodeName != null && nodeName.equals("#text")) {
                continue;
            }
            String content = node.getTextContent();
            map.put(nodeName, content);
        }
        return map;
    }

    /**
     * Returns the raw POST body to be sent.
     */
    private static byte[] getPostBody(Map<String, String> postParams) {
        if (postParams != null && postParams.size() > 0) {
            return encodeParameters(postParams, CHARSET);
        }
        return null;
    }

    private static void setConnectionParametersForRequest(
            HttpURLConnection connection,
            Map<String, String> postParams) throws IOException {
        byte[] postBody = getPostBody(postParams);
        if (postBody != null) {
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            DataOutputStream out = new DataOutputStream(
                    connection.getOutputStream());
            out.write(postBody);
            out.close();
        }
    }
}
