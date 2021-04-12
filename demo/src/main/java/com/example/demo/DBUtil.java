package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBUtil {
	public static Statement stmt;
	public static Connection con;
	
	public static final String CREATE_USER = "insert into user(NAME,EMAIL_ID,PHONE_NUMBER,PASSWORD,CREATED_TIME) values('%s','%s','%s','%s','%s')";
	public static final String GET_USER_OTP = "select user.ID, user.PHONE_NUMBER, user_to_otp.OTP, user_to_otp.CREATED_TIME from user left join user_to_otp on user.ID = user_to_otp.USER_ID where EMAIL_ID='%s'";
	public static final String UPDATE_USER_OTP = "update user_to_otp set OTP=%s,CREATED_TIME='%s' where USER_ID=%s";
	public static final String CREATE_USER_OTP = "insert into user_to_otp(USER_ID,OTP, CREATED_TIME) values(%s,%s,'%s')";
	public static final String FETCH_USER_PASSWORD = "select PASSWORD,SALT from user inner join user_to_salt on user.ID = user_to_salt.USER_ID where user.EMAIL_ID = '%s'";
	public static final String UPDATE_USER_PASSWORD = "Update user set PASSWORD='%s' where EMAIL_ID='%s'";
	public static final String INSERT_USER_SALT = "insert into user_to_salt(USER_ID,SALT,CREATED_TIME) values(?,?,?)";
	public static final String UPDATE_USER_SALT = "update user_to_salt inner join user on user_to_salt.USER_ID = user.ID set SALT=?, user_to_salt.CREATED_TIME=? where user.EMAIL_ID=?";
	
	public static void createDBConnection() throws Exception {
		Class.forName("com.mysql.cj.jdbc.Driver");
		con = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/pantry_buddy", "root",
				"rootpass");
		stmt = con.createStatement();
		boolean result = stmt.execute("select * from user");
		System.out.println("result is " + result);
	}

	public static Integer insertOrUpdate(String query) throws Exception {
		stmt.execute(query, Statement.RETURN_GENERATED_KEYS);
		ResultSet rs = stmt.getGeneratedKeys();
		Integer id = null;
        if (rs.next()){
            id=rs.getInt(1);
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
