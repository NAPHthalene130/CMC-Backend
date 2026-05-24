package com.cmc.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExcelUtils {

    public static <T> void export(HttpServletResponse response, String fileName, String sheetName,
                                   Class<T> clazz, List<T> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded + ".xlsx");

        EasyExcel.write(response.getOutputStream(), clazz)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet(sheetName)
                .doWrite(data);
    }
}
