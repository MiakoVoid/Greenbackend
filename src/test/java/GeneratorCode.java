import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.po.TableFill;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author YangHaixiong
 * @date 2023/12/25 21:56
 */
public class GeneratorCode {

    // 允许main方法自动生产代码
    public static void main(String[] args) {
        //自动生成代码后将com.it下的config、utils移到模块下
        generatorCode();
    }


    private static void generatorCode() {
        Dict dict = YamlUtil.loadByPath("product-config.yml");
        String database = dict.getStr("DATABASE");
        String productName = dict.getStr("PRODUCT_NAME");
        Map<String, String> dataMap = Arrays.asList(database.replace("{", "").replace("}", "").split(","))
                .stream().map(pair -> pair.split("=")).collect(Collectors.toMap(pair -> pair[0].trim(), pair -> pair[1]));
        String tableStr = dict.getStr("TABLE");
        String[] tables = tableStr.split(" ");
        List<String> tableList = Arrays.stream(tables).map(tableName -> tableName.replace("\"", "").replace("-", ""))
                .collect(Collectors.toList());
        configurationParams(productName, dataMap.get("NAME"), dataMap.get("USERNAME"), dataMap.get("PASSWORD"),
                Integer.valueOf(dataMap.get("PORT")), tableList.toArray(new String[0]));
    }

    private static void configurationParams(String productName, String databaseName,
                                            String username, String password, Integer port, String[] tables) {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();
        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String sysPath = System.getProperty("user.dir");
        gc.setOutputDir(sysPath + "/src/main/java");//在当前路径上加上这个路径
        // gc.setAuthor("kate");//添加作者信息
        gc.setOpen(false);//是否打开资源管理器
        gc.setFileOverride(false);//是否覆盖文件
        gc.setServiceName("%sService");//去除服务接口service前缀
        gc.setIdType(IdType.ASSIGN_ID);//使用雪花主键
        gc.setDateType(DateType.ONLY_DATE);//设置默认的时间格式
        gc.setSwagger2(false); //实体属性 Swagger2 注解
        mpg.setGlobalConfig(gc);//把全局配置放入代码生成器中

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        //在这里需要改成你自己的数据库
        String url = "jdbc:mysql://localhost:" + port +
                "/" + databaseName + "?characterEncoding=utf-8&serverTimezone=UTC&useSSL=false";
        dsc.setUrl(url);
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setUsername(username);
        dsc.setPassword(password);
        dsc.setDbType(DbType.MYSQL);//设置默认类型
        mpg.setDataSource(dsc);//将数据源配置放入代码生成器中

        // 包配置
        PackageConfig pc = new PackageConfig();
        //这里需要设置自己的包路径名
        pc.setModuleName(productName);//设置模块名
        pc.setParent("com.it");//设置包名，这样com.atguigu.blog
        pc.setEntity("pojo");//设置实体类的包名
        pc.setMapper("mapper");//设置持久层的包名
        pc.setService("service");//设置业务层的包名
        pc.setController("controller");//设置表现层（控制层）的包名
        mpg.setPackageInfo(pc);//将包的配置放入到自动代码生成器中

        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setInclude(tables);//设置要包含生成的表
        strategy.setNaming(NamingStrategy.underline_to_camel);//设置驼峰命名的自动映射
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);//设置列名的驼峰命名自动映射
        strategy.setEntityLombokModel(true);//设置是否支持lombok
        strategy.setRestControllerStyle(true);

        strategy.setLogicDeleteFieldName("deleted");//自动配置逻辑删除字段
        //自动填充配置
        TableFill gmtCreate = new TableFill("gmt_create", FieldFill.INSERT);
        TableFill gmtModified = new TableFill("gmt_modified", FieldFill.UPDATE);
        //创建一个list集合,把两个自动填充策略加入到这个集合
        ArrayList<TableFill> tableFill = new ArrayList<>();
        tableFill.add(gmtCreate);
        tableFill.add(gmtModified);
        strategy.setTableFillList(tableFill);//把自动填充放入到配置策略
        strategy.setVersionFieldName("version");//开启乐观锁
        strategy.setRestControllerStyle(true);//配置restful的风格的驼峰命名
        strategy.setControllerMappingHyphenStyle(true);//url多字段的/变成_下划线
        mpg.setStrategy(strategy);//把所有策略放入自动代码生成器中
        mpg.execute();//执行
    }
}

