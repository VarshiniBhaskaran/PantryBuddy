package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBUtil {
	public static Statement stmt;
	public static Connection con;
	
	//USER MODULE QUERIES
	public static final String CREATE_USER = "insert into user(NAME,EMAIL_ID,PHONE_NUMBER,PASSWORD,CREATED_TIME) values('%s','%s','%s','%s','%s')";
	public static final String GET_USER_OTP = "select user.ID, user.PHONE_NUMBER, user_to_otp.OTP, user_to_otp.CREATED_TIME from user left join user_to_otp on user.ID = user_to_otp.USER_ID where EMAIL_ID='%s'";
	public static final String GET_USER_OTP_FOR_PHONE = "select user.ID, user.PHONE_NUMBER, user_to_otp.OTP, user_to_otp.CREATED_TIME from user left join user_to_otp on user.ID = user_to_otp.USER_ID where PHONE_NUMBER='%s'";
	public static final String UPDATE_USER_OTP = "update user_to_otp set OTP=%s,CREATED_TIME='%s' where USER_ID=%s";
	public static final String CREATE_USER_OTP = "insert into user_to_otp(USER_ID,OTP, CREATED_TIME) values(%s,%s,'%s')";
	public static final String FETCH_USER_PASSWORD = "select PASSWORD,SALT from user inner join user_to_salt on user.ID = user_to_salt.USER_ID where user.EMAIL_ID = '%s'";
	public static final String UPDATE_USER_PASSWORD = "Update user set PASSWORD='%s' where EMAIL_ID='%s'";
	public static final String INSERT_USER_SALT = "insert into user_to_salt(USER_ID,SALT,CREATED_TIME) values(?,?,?)";
	public static final String UPDATE_USER_SALT = "update user_to_salt inner join user on user_to_salt.USER_ID = user.ID set SALT=?, user_to_salt.CREATED_TIME=? where user.EMAIL_ID=?";
	public static final String GET_USER_ID_FOR_EMAIL = "select ID from user where EMAIL_ID ='%s'";
	public static final String UPDATE_USER_ALLERGY= "update user set ALLERGY='%s' where EMAIL_ID='%s'";
			
	//PRODUCT MODULE QUERIES
	public static final String FETCH_USER_PRODUCTS = "select PRODUCT_NAME, MANUFACTURER, EXPIRY_DATE, COUNT, product.CATEGORY, INGREDIENTS, SERVING_SIZE,IMAGE from user_products inner join product on user_products.PRODUCT_ID = product.ID inner join user on user_products.USER_ID = user.id left join product_category_to_image on product.CATEGORY = product_category_to_image.CATEGORY where user.EMAIL_ID ='%s' order by EXPIRY_DATE";
	public static final String SELECT_PRODUCT = "select ID from product where PRODUCT_NAME='%s' and MANUFACTURER='%s'";
	public static final String SELECT_USER_PRODUCT = "select COUNT from user_products where PRODUCT_ID=%s and USER_ID=%s and EXPIRY_DATE='%s'";
	public static final String INSERT_PRODUCT ="insert into product(PRODUCT_NAME, MANUFACTURER, CATEGORY, INGREDIENTS,SERVING_SIZE) values('%s','%s','%s','%s','%s')";
	public static final String INSERT_USER_PRODUCTS = "insert into user_products(PRODUCT_ID, USER_ID,COUNT,EXPIRY_DATE) values(%s,%s,%s,'%s')";
	public static final String UPDATE_USER_PRODUCTS = "update user_products set COUNT=%s where USER_ID = %s and PRODUCT_ID=%s";
	
	public static void createDBConnection() throws Exception {
		Class.forName("com.mysql.cj.jdbc.Driver");
		con = DriverManager.getConnection(
				"jdbc:mysql://127.0.0.1:3306/pantry_buddy", "root",
				"rootpass");
		stmt = con.createStatement();
		boolean result = stmt.execute("select * from user");
		System.out.println("result is " + result);
	}

	public static Long insertOrUpdate(String query) throws Exception {
		stmt.execute(query, Statement.RETURN_GENERATED_KEYS);
		ResultSet rs = stmt.getGeneratedKeys();
		Long id = null;
        if (rs.next()){
            id=rs.getLong(1);
        }
        return id;
	}

	public static ResultSet executeQuery(String query) throws Exception {
		return stmt.executeQuery(query);
	}
	
	public static int update(String query) throws Exception{
		return stmt.executeUpdate(query);
	}
}
