package org.xresloader.core.data.dst;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.engine.IdentifyEngine;
import org.xresloader.core.scheme.SchemeConf;
import org.json.JSONObject;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstUECsv extends DataDstJava {
    static private Pattern fileToClassMatcher = Pattern.compile("[\\\\.-\\\\(\\\\)]");

    static private String codeHeaderPrefix1 = String.join("\r\n",
            "// This file is generated by xresloader, please don't edit it.", "", "#pragma once", "",
            "#include \"CoreMinimal.h\"", "#include \"Engine/DataTable.h\"", "");
    static private String codeHeaderIncludeGenerated = "#include \"%s.generated.h\"";
    static private String codeHeaderPrefix2 = String.join("\r\n", "", "", "", "USTRUCT(BlueprintType)", "");
    static private String codeHeaderClassName = "struct F%s : public FTableRowBase";
    static private String codeHeaderPrefix3 = String.join("\r\n", "", "{", "    GENERATED_USTRUCT_BODY()", "",
            "    // Start of fields");
    static private String codeHeaderSuffix = "\r\n};";

    static private String codeSourceInclude = "#include \"%s.h\"";

    @Override
    public boolean init() {
        return true;
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "ue csv";
    }

    public class UECsvCodeInfo {
        public String header = null;
        public String source = null;
        public String outputDir = null;
        public String headerDir = null;
        public String sourceDir = null;
        public String clazzName = null;
        public String bashName = null;
    };

    static public String getIdentName(String in) {
        return fileToClassMatcher.matcher(in).replaceAll("_");
    }

    public UECsvCodeInfo getCodeInfo(String outputFile) throws IOException {
        UECsvCodeInfo ret = new UECsvCodeInfo();

        File ofd = new File(outputFile);
        ret.outputDir = ofd.getParentFile().getCanonicalFile().getAbsolutePath();

        // TODO redirect header directory and source directory
        ret.headerDir = ret.outputDir;
        ret.sourceDir = ret.outputDir;

        String fileName = ofd.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            ret.bashName = outputFile;
        } else {
            ret.bashName = fileName.substring(0, lastDot);
        }
        ret.clazzName = getIdentName(ret.bashName);

        ret.header = ret.headerDir + File.separator + ret.clazzName + ".h";
        ret.source = ret.sourceDir + File.separator + ret.clazzName + ".cpp";

        return ret;
    }

    private Charset encodingCache = null;

    private byte[] dumpString(String in) {
        if (null != encodingCache) {
            return in.getBytes(encodingCache);
        }

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return in.toString().getBytes();

        encodingCache = Charset.forName(encoding);
        return in.getBytes(encodingCache);
    }

    public FileOutputStream createCodeHeaderFileStream(UECsvCodeInfo code) throws IOException {
        File ofd = new File(code.header);
        File parentFile = ofd.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(code.header, false);
        fos.write(dumpString(codeHeaderPrefix1));
        fos.write(dumpString(String.format(codeHeaderIncludeGenerated, code.clazzName)));
        fos.write(dumpString(codeHeaderPrefix2));
        fos.write(dumpString(String.format(codeHeaderClassName, code.clazzName)));
        fos.write(dumpString(codeHeaderPrefix3));

        return fos;
    }

    public FileOutputStream createCodeSourceFileStream(UECsvCodeInfo code) throws IOException {
        File ofd = new File(code.source);
        File parentFile = ofd.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(code.source, false);
        fos.write(dumpString(String.format(codeSourceInclude, code.clazzName)));

        return fos;
    }

    public void appendCommonHeader(CSVPrinter sp) throws IOException {
        sp.printComment("This file is generated by xresloader, please don't edit it.");
    }

    @Override
    public final byte[] build(DataDstImpl compiler) throws ConvException {
        // DataDstJava.DataDstObject data_obj = build_data(compiler);
        StringBuffer sb = new StringBuffer();
        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    }

    @Override
    public final DataDstWriterNode compile() {
        ProgramOptions.getLoger().error("UE-CSV can not be protocol description.");
        return null;
    }

    private void writeConstData(CSVPrinter sp, Object data, String prefix) throws IOException {
        // null
        if (null == data) {
            sp.printRecord(prefix, "");
            return;
        }

        // 数字
        // 枚举值已被转为Java Long，会在这里执行
        if (data instanceof Number) {
            sp.printRecord(prefix, data);
            return;
        }

        // 布尔
        if (data instanceof Boolean) {
            sp.printRecord(prefix, ((Boolean) data) ? 1 : 0);
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            sp.printRecord(prefix, data);
            return;
        }

        // 列表
        if (data instanceof List) {
            List<Object> ls = (List<Object>) data;
            for (int i = 0; i < ls.size(); ++i) {
                if (prefix.isEmpty()) {
                    writeConstData(sp, ls.get(i), String.format("%d", i));
                } else {
                    writeConstData(sp, ls.get(i), String.format("%s.%d", prefix, i));
                }
            }
            return;
        }

        // Hashmap
        if (data instanceof Map) {
            Map<String, Object> mp = (Map<String, Object>) data;
            for (Map.Entry<String, Object> item : mp.entrySet()) {
                if (prefix.isEmpty()) {
                    writeConstData(sp, item.getValue(), String.format("%s", item.getKey()));
                } else {
                    writeConstData(sp, item.getValue(), String.format("%s.%s", prefix, item.getKey()));
                }
            }
            return;
        }

        ProgramOptions.getLoger().error("rewrite %s as nil, should not called here.", data.toString());
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     * @throws IOException
     */
    public final byte[] dumpConst(HashMap<String, Object> data) throws IOException {

        StringBuffer sb = new StringBuffer();
        CSVPrinter csv = new CSVPrinter(sb, CSVFormat.EXCEL.withHeader("key", "value"));

        appendCommonHeader(csv);
        writeConstData(csv, data, "");

        // 加载代码
        UECsvCodeInfo codeInfo = getCodeInfo(SchemeConf.getInstance().getOutputFileAbsPath());

        DataDstWriterNode valueField = DataDstWriterNode.create(null, DataDstWriterNode.JAVA_TYPE.INT);
        valueField.identify = IdentifyEngine.n2i("value", 0);

        FileOutputStream headerFs = createCodeHeaderFileStream(codeInfo);
        headerFs.write(dumpString("\r\n    /** Field Type: FString, Name: key, skipped tag field**/\r\n"));

        writeCodeHeaderField(headerFs, valueField);
        headerFs.write(dumpString(codeHeaderSuffix));
        headerFs.close();

        FileOutputStream sourceFs = createCodeSourceFileStream(codeInfo);
        sourceFs.close();

        // 带编码的输出
        return dumpString(sb.toString());
    }

    private String headerFieldUProperty = null;

    private final String getHeaderFieldUProperty() {
        if (null == headerFieldUProperty) {
            LinkedList<String> ls = new LinkedList<String>();
            if (!SchemeConf.getInstance().getUECSVOptions().editAccess.isEmpty()) {
                ls.add(SchemeConf.getInstance().getUECSVOptions().editAccess);
            }

            if (!SchemeConf.getInstance().getUECSVOptions().category.isEmpty()
                    && !SchemeConf.getInstance().getUECSVOptions().blueprintAccess.isEmpty()) {
                ls.add(SchemeConf.getInstance().getUECSVOptions().blueprintAccess);
                ls.add(String.format("Category = %s", SchemeConf.getInstance().getUECSVOptions().category));
            }
            headerFieldUProperty = String.format("    UPROPERTY(%s)\r\n", String.join(", ", ls));
        }

        return headerFieldUProperty;
    }

    private final void writeCodeHeaderField(FileOutputStream fout, DataDstWriterNode node) throws IOException {
        fout.write(dumpString(String.format("\r\n    /** Field Type: %s, Name: %s **/\r\n", node.getType().name(),
                node.identify.name)));

        fout.write(dumpString(getHeaderFieldUProperty()));

        switch (node.getType()) {
        case INT: {
            fout.write(dumpString(String.format("    %s %s;\r\n", "int32", getIdentName(node.identify.name))));
            break;
        }

        case LONG: {
            fout.write(dumpString(String.format("    %s %s;\r\n", "int64", getIdentName(node.identify.name))));
            break;
        }

        case FLOAT: {
            fout.write(dumpString(String.format("    %s %s;\r\n", "float", getIdentName(node.identify.name))));
            break;
        }

        case DOUBLE: {
            fout.write(dumpString(String.format("    %s %s;\r\n", "double", getIdentName(node.identify.name))));
            break;
        }

        case BOOLEAN: {
            fout.write(dumpString(String.format("    %s %s;\r\n", "bool", getIdentName(node.identify.name))));
            break;
        }

        case STRING: {
            fout.write(dumpString(String.format("    %s %s;\r\n", "FString", getIdentName(node.identify.name))));
            break;
        }

        case BYTES: {
            fout.write(dumpString(String.format("\r\n    /** Bytes data will be encoded by base64 */\r\n",
                    node.identify.name, node.getType().name())));
            fout.write(dumpString(String.format("    %s %s;\r\n", "FString", getIdentName(node.identify.name))));
            break;
        }

        case MESSAGE: {
            ProgramOptions.getLoger().error("invalid data type %s for UE-CSV, should not called here.",
                    node.getType().name());
            break;
        }

        default:
            ProgramOptions.getLoger().error("invalid data type %s for UE-CSV, should not called here.",
                    node.getType().name());
            break;
        }
    }
}
