package com.example.html2pdf;

import cn.wanghaomiao.xpath.model.JXDocument;
import cn.wanghaomiao.xpath.model.JXNode;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.attach.impl.layout.HtmlPageBreak;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.font.FontProvider;

import java.io.*;
import java.net.URL;
import java.util.List;

public final class Html2pdfUtil {
    private Html2pdfUtil() {

    }

    /**
     * @param htmlUrl htmlUrl
     * @return
     * @throws Exception Exception
     */
    public static byte[] convert(String htmlUrl) throws Exception {
        WebClient wc = new WebClient(BrowserVersion.CHROME);

        wc.getOptions().setUseInsecureSSL(true);
        wc.getOptions().setJavaScriptEnabled(true); // 启用JS解释器，默认为true
        wc.getOptions().setCssEnabled(true); // 禁用css支持
        wc.getOptions().setThrowExceptionOnScriptError(false); // js运行错误时，是否抛出异常
        wc.getOptions().setTimeout(100000); // 设置连接超时时间 ，这里是10S。如果为0，则无限期等待
        wc.getOptions().setDoNotTrackEnabled(false);
//        wc.getOptions().setActiveXNative(true);
        HtmlPage page;

        byte[] responseContent = null;
        URL url = new URL(htmlUrl);
        WebRequest webRequest = new WebRequest(url, HttpMethod.POST);
        webRequest.setCharset("utf-8");
        page = wc.getPage(webRequest);
        WebResponse webResponse = page.getWebResponse();
        int status = webResponse.getStatusCode();
        // 读取数据内容
        if (status == 200) {
            if (page.isHtmlPage()) {
                // 等待JS执行完成，包括远程JS文件请求，Dom处理
                wc.waitForBackgroundJavaScript(10000);
                responseContent = ((HtmlPage) page).asXml().getBytes();
            } else {
                InputStream bodyStream = webResponse.getContentAsStream();
                responseContent = inputStream2byte(bodyStream);
                bodyStream.close();
            }
        }
        // 关闭响应流
        webResponse.cleanUp();
        String strToHtml = new String(responseContent);
        strToHtml = strToHtml.replace("width: 500px; height: 500px;", "width: 534px; height: 801px;table-layout:fixed;");
        strToHtml = subStringItemName(strToHtml);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        html2pdf(strToHtml, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 功能描述:
     *
     * @param inputStream 输入流
     * @return byte[] 数组
     */
    public static byte[] inputStream2byte(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {

            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = inputStream.read(buff, 0, 100)) > 0) {
                byteArrayOutputStream.write(buff, 0, rc);
            }
            return byteArrayOutputStream.toByteArray();
        } finally {
            byteArrayOutputStream.close();
        }
    }

    public static void html2pdf(String html, OutputStream outputStream) throws Exception {
        ConverterProperties props = new ConverterProperties();
        // props.setCharset("UFT-8"); 编码
        FontProvider fp = new FontProvider();
        fp.addStandardPdfFonts();
        // .ttf 字体所在目录
        InputStream inputStream = null;
        byte[] bytes = null;
        try {
            inputStream = Html2pdfUtil.class.getResourceAsStream("/font/arialuni.ttf");
            bytes = toByteArray(inputStream);
        } finally {
            inputStream.close();
        }
        fp.addFont(bytes);
        props.setFontProvider(fp);
        // html中使用的图片等资源目录（图片也可以直接用url或者base64格式而不放到资源里）
        // props.setBaseUri(resources);
        List<IElement> elements = HtmlConverter.convertToElements(html, props);
        PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream));
        Document document = new Document(pdf, new PageSize(408F, 612F), false);
        document.setMargins(3,3,3,3);
        for (IElement element : elements) {
            // 分页符
            if (element instanceof HtmlPageBreak) {
                document.add((HtmlPageBreak) element);
                //普通块级元素
            } else {
                document.add((IBlockElement) element);
            }
        }
        document.close();
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public static void main(String[] args)throws Exception {
        String strToHtml="面单html字符串";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        strToHtml = strToHtml.replace("width: 500px; height: 500px;", "width: 534px; height: 801px;table-layout:fixed;");
        strToHtml = subStringItemName(strToHtml);
        html2pdf(strToHtml, byteArrayOutputStream);
        FileOutputStream fileOutputStream = new FileOutputStream("e:\\hello-world.pdf");
        fileOutputStream.write(byteArrayOutputStream.toByteArray());
    }

    private static String subStringItemName(String html) {
        String xpath="//body/table/tbody/tr[4]/td/table/tbody/tr/td[1]/text()";
        JXDocument jxDocument = new JXDocument(html);
        List<JXNode> rs = jxDocument.selN(xpath);
        final int itemNameLengthLimit = 40;
        for (JXNode o:rs){
            try {
                String itemName = o.getTextVal();

                if (itemName.length() > itemNameLengthLimit){
                    html = html.replace(o.getTextVal(), itemName.substring(0, itemNameLengthLimit)+"&nbsp;&nbsp;");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return html;
    }
}
