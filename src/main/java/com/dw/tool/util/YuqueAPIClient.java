package com.dw.tool.util;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2025/1/8 14:12
 */
public class YuqueAPIClient {


    static String authToken = "aliyungf_tc=08708f82fe8166f36a85051a9ba32547b54c9da920a06ea54383123df2a0180a; yuque_ctoken=XBuh7UFdCgK0QmqXSGCW5H9z; receive-cookie-deprecation=1; _uab_collina=172706291215488212417582; lang=zh-cn; current_theme=default; _yuque_session=cQPbTtJC4-wwB5FweH5l9Rhp3bhVApgX62JCdzmEXuDSL8_j6rs_Qr0spJ4E-Bj4L9MLWc3fF30gbj7MbmnUvg==; tfstk=fZKKG7cf4fcn322TWzgGZza5u5His3pEtBJbq_f3PCd9hKPhPyXhVuOcsHfHKMMJwIOdrQprLT1WNQCk-VmDLpSPVjA-mmvFi18I7BCQR5M5etZSLMaxNpSPVf0WBwRWLItd-gDRVAI1eTPCVTZIBl6F1k67ATNsB1W5V9_CRPi1EtB5NuOWCABP1_GzTJCud_EJr9PhjYlK5uEJXOGFMpE3qOKApaCXdHE7VZ6dJs9C6jYmsvQ6UaKzUuQkdERPhBNIw_-9CHTXGf4hOEBJnUdjEyWGyHSONHi4H1tpehQeSWaCHMCdlH_j2xpfPFKOxHGzudvOOZIMSVlhrMdpus7I7fRJB6AWvNNKs_YDIH_9Gfq9aaLWxGtI1mIy8nxAj1PcMT4IBAUzzw6w89jvwjeG1tBOiA4gzz7GQOCmByUzzwhNBsDNuzzPJk5..; acw_tc=ac11000117363157693462269e098dfbaf69a43c753d10d7e0c465c02d75cf";

    static String csrfToken = "XBuh7UFdCgK0QmqXSGCW5H9z";

    public static void main(String[] args) throws Exception {
        String baseURL = "https://www.yuque.com/api";
        String personalBooksURL = baseURL + "/mine/personal_books";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Step 1: 获取所有知识库的Id
            HttpGet personalBooksRequest = new HttpGet(personalBooksURL);
            personalBooksRequest.setHeader("Cookie", authToken);
            personalBooksRequest.setHeader("X-Csrf-Token", csrfToken);

            HttpResponse personalBooksResponse = httpClient.execute(personalBooksRequest);
            HttpEntity personalBooksEntity = personalBooksResponse.getEntity();
            String personalBooksResponseString = EntityUtils.toString(personalBooksEntity);
            JSONObject personalBooksData = new JSONObject(personalBooksResponseString);
            JSONArray personalBooks = personalBooksData.getJSONArray("data");

            // 1.1、遍历知识库
            for (int i = 0; i < personalBooks.length(); i++) {
                JSONObject book = personalBooks.getJSONObject(i);
                String bookId = String.valueOf(book.get("id"));
                String bookName = (String) book.get("name");

                // Step 2: 获取知识库下的所有文章
                String bookURL = baseURL + "/docs/?book_id=" + bookId;
                HttpGet docRequest = new HttpGet(bookURL);
                docRequest.setHeader("Cookie", authToken);
                docRequest.setHeader("X-Csrf-Token", csrfToken);

                HttpResponse docResponse = httpClient.execute(docRequest);
                HttpEntity docEntity = docResponse.getEntity();
                String docResponseString = EntityUtils.toString(docEntity);
                JSONObject docData = new JSONObject(docResponseString);
                JSONArray docs = docData.getJSONArray("data");

                // Step 3: 遍历知识库下的所有文章
                for (int j = 0; j < docs.length(); j++) {
                    JSONObject docEntry = docs.getJSONObject(j);
                    String docId = String.valueOf(docEntry.get("id"));
                    String docName = docEntry.getString("title");

                    // Step 3.1：根据文档层级创建目录结构
                    String docPath = createDocumentPath(bookName, docEntry);

                    String exportURL = baseURL + "/docs/" + docId + "/export";
                    HttpPost exportRequest = new HttpPost(exportURL);
                    exportRequest.setHeader("Cookie", authToken);
                    exportRequest.setHeader("X-Csrf-Token", csrfToken);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "markdown");
                    jsonObject.put("force", 0);
                    jsonObject.put("options", "{\"latexType\": 2,\"enableAnchor\": 0,\"enableBreak\": 0}");

                    StringEntity entity = new StringEntity(jsonObject.toString());
                    entity.setContentType("application/json");
                    exportRequest.setEntity(entity);

                    HttpResponse exportResponse = httpClient.execute(exportRequest);
                    HttpEntity exportEntity = exportResponse.getEntity();
                    String exportResponseString = EntityUtils.toString(exportEntity);
                    JSONObject jsonObject1 = new JSONObject(exportResponseString);
                    if (!jsonObject1.has("data")) {
                        continue;
                    }
                    JSONObject exportData = jsonObject1.getJSONObject("data");
                    String downloadURL = exportData.getString("url");

                    // Step 3.2：保存文章到本地
                    saveFileFromURL(downloadURL, docPath);

//                    // Step 3.3: 处理文章中的附件（表格、PDF、图片等）
//                    extractAndDownloadAttachments(docEntry, docPath);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // 根据文档层级创建文件夹路径
    private static String createDocumentPath(String bookName, JSONObject docEntry) {
        // 解析文档层级信息，递归获取父文档的结构
        StringBuilder pathBuilder = new StringBuilder("D:/tmp/yuque/" + bookName);

        // 假设文档层级信息存在于字段 `parent` 或 `parent_id` 中
        // 您可能需要根据语雀的 API 结构进一步解析文档层级关系
//        String docTitle = docEntry.getString("title");
//        pathBuilder.append("/").append(docTitle);

        // 如果存在子文档或父文档，您可以在此添加递归逻辑
        // 比如：递归遍历父文档链、子文档等
//         pathBuilder.append("/" + parentTitle);

        return pathBuilder.toString();
    }

    public static void saveFileFromURL(String fileURL, String docPath) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(fileURL);
            httpGet.setHeader("Cookie", authToken);
            httpGet.setHeader("X-Csrf-Token", csrfToken);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String fileName;

            // 获取文件名
            if (response.getHeaders("Content-Disposition")[0].getElements()[0].getParameters().length == 2) {
                String file = response.getHeaders("Content-Disposition")[0].getElements()[0].getParameters()[1].getValue();
                fileName = URLDecoder.decode(file);
                fileName = fileName.substring(fileName.lastIndexOf("'"));
            } else {
                fileName = response.getHeaders("Content-Disposition")[0].getElements()[0].getParameters()[0].getValue();
            }
            fileName = fileName.replaceAll("[|<>?'‘’【】？—\\s]", "");

            // 创建文件路径
            File dir = new File(docPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs(); // 创建文件夹（包括父目录）
                if (!created) {
                    throw new IOException("无法创建目录：" + dir.getPath());
                }
            }

            // 创建文件路径
            File file = new File(dir, fileName);
            if (file.exists()) {
                System.out.println("文件已存在，将会覆盖：" + file.getPath());
            }

            if (entity != null) {
                try (InputStream inputStream = entity.getContent();
                     FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int n;
                    while ((n = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, n);
                    }
//                    System.out.println("文件已保存：" + file.getPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 解析文档中的附件并下载（包括表格、图片、PDF、Word等）
    private static void extractAndDownloadAttachments(JSONObject docEntry, String docPath) {
        // 解析文档中的附件链接（图片、PDF、Word等）
        String docContent = docEntry.getString("body");  // 文档的内容部分（HTML 或 Markdown 格式）

        // 匹配文档中的所有附件URL（可以根据实际情况修改正则）
        Pattern pattern = Pattern.compile("(http[s]?://[\\S]+(?:\\.pdf|\\.docx|\\.xlsx|\\.jpg|\\.png))");
        Matcher matcher = pattern.matcher(docContent);

        while (matcher.find()) {
            String attachmentURL = matcher.group(1);
            String fileName = attachmentURL.substring(attachmentURL.lastIndexOf("/") + 1);

            // 下载附件
            try {
                saveFileFromURL(attachmentURL, docPath + "/attachments");
            } catch (Exception e) {
                System.out.println("下载附件失败：" + attachmentURL);
            }
        }
    }
}
