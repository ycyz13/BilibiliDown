package ycyz;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.junit.Test;

import java.util.Collections;

public class GenTable {
    @Test
    public void testGen(){
        String url = "jdbc:mysql://localhost:3306/movie?useUnicode=true&characterEncoding=utf8mb4&useSSL=true";
        String username = "root";
        String password = "123456";
        String author = "ycyz";
        String outputDir = "C:\\02-workspace\\codegen";
        String parentPackage = "ycyz";
        String moduleName = null;
        String outputFileXml = "C:\\02-workspace\\codegen\\xml";

        StrategyConfig config = new StrategyConfig.Builder().entityBuilder().enableLombok().build();
        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author(author) // 设置作者
                            .outputDir(outputDir); // 指定输出目录
                })
                .packageConfig(builder -> {
                    builder.parent(parentPackage) // 设置父包名
                            .moduleName(moduleName) // 设置父包模块名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, outputFileXml)); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.addInclude("up") // 设置需要生成的表名
                        .entityBuilder().enableLombok();  // lombok模式
//                            .addTablePrefix("t_", "c_"); // 设置过滤表前缀
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}
