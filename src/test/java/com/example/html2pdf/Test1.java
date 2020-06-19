package com.example.html2pdf;

import java.io.FileOutputStream;

public class Test1 {
    public static void main(String[] args) {
        try {
            byte[] bytes = Html2pdfUtil.convert("https://erp112.tongtool.com/file/047081/logisticAddressLabel/2020-06-17/P011816.html");
            FileOutputStream fileOutputStream = new FileOutputStream("./hello-world.pdf");
            fileOutputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
