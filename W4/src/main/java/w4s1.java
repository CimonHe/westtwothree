import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.*;
import java.net.http.HttpClient.Version;
import java.time.Duration;

import java.sql.*;

public class w4s1 {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/learnjdbc?useSSL=false&characterEncoding=utf8";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "*******";//代指密码
    private static int idCountry=0;
    private static int idProvince=0;
    static HttpClient httpClient = HttpClient.newBuilder().build();
    public static void main(String[] args) {
        // 获取数据库连接:
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        writeToMysql();//爬取信息进行解码，提取数据并写入数据库
        searchProvince("Fujian");//测试查询某省的疫情信息
        upadateToCoutryMysql("Fujian",520);//测试更新某省痊愈人数信息
        searchProvince("Fujian");//测试获取某省的疫情信息

        try {
            assert conn != null;
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }// 关闭连接:

    }

    public static String getCovidInfo(String country)//--------爬取疫情某个国家（参数）API的数据并以String格式返回
    {
        String url = "https://covid-api.mmediagroup.fr/v1/cases"+"/?country="+country;
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder(new URI(url))
                    // 设置Header:
                    .header("User-Agent", "Java HttpClient").header("Accept", "*/*")
                    // 设置超时:
                    .timeout(Duration.ofSeconds(5))
                    // 设置版本:
                    .version(Version.HTTP_2).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        assert response != null;
        System.out.println(response.body());
        return response.body();
    }



   public static void addToCountryMysql(String s)//将某个国家疫情信息添加到countrymsql数据库
   {
       JSONObject jsonObject = JSONObject.parseObject(s); //因为JSONObject继承了JSON，所以这样也是可以的
       try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
           try (PreparedStatement ps = conn.prepareStatement(
                   "INSERT INTO countrymysql (id, confirmed, recovered, deaths,country,population,sq_km_area,life_expectancy,elevation_in_meters,continent,abbreviation,location,iso,capital_city) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
               ps.setObject(1, ++idCountry); // 注意：索引从1开始
               ps.setObject(2, jsonObject.getInteger("confirmed"));
               ps.setObject(3, jsonObject.getInteger("recovered"));
               ps.setObject(4, jsonObject.getInteger("deaths"));
               ps.setObject(5, jsonObject.getString("country"));
               ps.setObject(6, jsonObject.getInteger("population"));
               ps.setObject(7, jsonObject.getInteger("sq_km_area"));
               ps.setObject(8, jsonObject.getString("life_expectancy"));
               ps.setObject(9, jsonObject.getString("elevation_in_meters"));
               ps.setObject(10, jsonObject.getString("continent"));
               ps.setObject(11, jsonObject.getString("abbreviation"));
               ps.setObject(12, jsonObject.getString("location"));
               ps.setObject(13, jsonObject.getInteger("iso"));
               ps.setObject(14, jsonObject.getString("capital_city"));
               int n = ps.executeUpdate();
           }
       } catch (SQLException throwables) {
           throwables.printStackTrace();
       }

   }


   public static void upadateToCoutryMysql(String provinceName,int num)//更新某省痊愈人数信息
    {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE provincemysql SET recovered=? WHERE province=?")) {
                ps.setObject(1,num ); // 注意：索引从1开始
                ps.setObject(2, provinceName);
                int n = ps.executeUpdate(); // 返回更新的行数
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    public static void addToProvinceMysql(String s,String province)//将某个省疫情信息添加到province数据库
    {
        JSONObject jsonObject = JSONObject.parseObject(s); //因为JSONObject继承了JSON，所以这样也是可以的
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO provincemysql (id,lat,c_long,confirmed,recovered,deaths,updated,country_id,province) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setObject(1, ++idProvince);
                ps.setObject(2, jsonObject.getString("lat"));
                ps.setObject(3, jsonObject.getString("long"));
                ps.setObject(4, jsonObject.getInteger("confirmed"));
                ps.setObject(5, jsonObject.getInteger("recovered"));
                ps.setObject(6, jsonObject.getInteger("deaths"));
                ps.setObject(7, jsonObject.getString("updated"));
                ps.setObject(8, idCountry);
                ps.setObject(9, province);
                int n = ps.executeUpdate();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void searchProvince(String provinceName)//查询某省的疫情信息
    {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT confirmed, recovered, deaths, updated FROM provincemysql WHERE province=?")) {
                ps.setObject(1, provinceName); // 注意：索引从1开始
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int confirmed = rs.getInt("confirmed");
                        int recovered = rs.getInt("recovered");
                        int deaths = rs.getInt("deaths");
                        String updated = rs.getString("updated");
                        System.out.println(provinceName+" province "+"截止 "+updated+"  "+"新冠确诊人数为："+confirmed+"    痊愈人数为："+recovered+" 死亡人数为："+deaths);
                    }
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    public static void jsonToData(String s)//将爬取下来的json格式数据利用fastjson和手动格式处理转换成数据
    {
        int i=1;
        for (;s.charAt(i)!='{';i++);
        String str1="";
        // System.out.println(i);
        for (;s.charAt(i-1)!='}';i++)
        {
            if (s.charAt(i)!=' ')
                str1=str1+s.charAt(i);
        }
        System.out.println(str1);
            addToCountryMysql(str1);
            while (true) {
                //JSONObject jsonObject = JSONObject.parseObject(str1);
                //    System.out.println(i);
                for (; !Character.isLetter(s.charAt(i)); i++) ;
                //  System.out.println(i);
                String province = "";
                String str2 = "";
                for (; Character.isLetter(s.charAt(i)); i++) {
                    if (s.charAt(i) != ' ')
                        province = province + s.charAt(i);
                }
                System.out.println(province);
                for (; s.charAt(i) != '{' && i < s.length(); i++) ;
                if (i >= s.length() - 1)
                    break;
                //    System.out.println(i);
                for (; s.charAt(i - 1) != '}' && i < s.length(); i++) {
                    //  System.out.println(i);
                    if (s.charAt(i) != ' ')
                        str2 = str2 + s.charAt(i);
                }
                System.out.println(str2);
                addToProvinceMysql(str2, province);
                if (i >= s.length() - 1)
                    break;
        }
    }

   public static void writeToMysql()//爬取信息进行解码，提取数据并写入数据库
   {
        String s=getCovidInfo("China");
        jsonToData(s);
        s=getCovidInfo("US");
        jsonToData(s);
       s=getCovidInfo("Japan");
       jsonToData(s);
       try {
           s=getCovidInfo(URLEncoder. encode("United Kingdom", "utf-8" ));
       } catch (UnsupportedEncodingException e) {
           e.printStackTrace();
       }
       jsonToData(s);
   }
}


