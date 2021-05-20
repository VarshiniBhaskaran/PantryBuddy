package com.example.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class CommonUtils {
	
	
	public static boolean checkIfEmailExists(String emailId) throws Exception {
		String query = String.format(DBUtil.GET_USER_OTP, emailId);
		ResultSet rs = DBUtil.executeQuery(query);
		while(rs.next()) {
			Long userId = rs.getLong("ID");
			if(userId!=null || userId!=0) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean checkIfPhoneNumberExists(String number) throws Exception {
		String query = String.format(DBUtil.GET_USER_OTP_FOR_PHONE, number);
		ResultSet rs = DBUtil.executeQuery(query);
		while(rs.next()) {
			Long userId = rs.getLong("ID");
			if(userId!=null || userId!=0) {
				return true;
			}
		}
		return false;
	}

	public static boolean checkIfPasswordIsValid(String password) {
		Pattern passwordRegex = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
		return passwordRegex.matcher(password).matches();	
	}

	public static boolean checkIfEmailIsValid(String emailId) {
		String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";
		Pattern pat = Pattern.compile(emailRegex);
		return pat.matcher(emailId).matches();	
	}

	public static String getCurrentTime() {
		Date dt = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(dt);		
	}

	public static JSONObject generateResponse(APIResponse apiResponse) throws JSONException {
		JSONObject response = new JSONObject();
		response.put("Code", apiResponse.getCode());
		response.put("Message", apiResponse.getMessage());
		return response;
	}
	
	public static byte[] generateSalt() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[16];
		random.nextBytes(salt);
		return salt;
	}
	public static String generateHashForString(String string, byte[] salt) throws Exception{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(salt);
		byte[] bytes = md.digest(string.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
		 sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		String generatedPassword = sb.toString();
		return generatedPassword;
	}
	
	public static String httpGetCall(String requestUrl) throws Exception {
		URL url = new URL(requestUrl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		con.setInstanceFollowRedirects(false);
		int status = con.getResponseCode();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		con.disconnect();
		System.out.println(content.toString());
		return content.toString();



	}

}
