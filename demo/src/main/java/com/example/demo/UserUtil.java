package com.example.demo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UserUtil {
	@PostMapping("/user/create")
	public String createUser(String firstName, String lastName, String emailId, String phoneNumber, String password) throws Exception {
		String precheckResponse = performPrechecks(firstName, lastName, emailId, phoneNumber, password);
		if (precheckResponse != null) {
			return precheckResponse;
		}
		String currentTime = CommonUtils.getCurrentTime();
		String name = firstName + "###" + lastName;

		byte[] salt = CommonUtils.generateSalt();

		String hashedPassword = CommonUtils.generateHashForString(password, salt);

		String query = String.format(DBUtil.CREATE_USER, name, emailId, phoneNumber, hashedPassword, currentTime);
		Long userId = DBUtil.insertOrUpdate(query);
		//EmailServiceImpl.sendWelcomeEmail(emailId);
		if (userId != null || userId != 0) {
			updatePassword(userId, salt, currentTime);
			return CommonUtils.generateResponse(APIResponse.USER_CREATED_SUCCESSFULLY).toString();
		} else {
			return CommonUtils.generateResponse(APIResponse.USER_CREATION_FAILED).toString();
		}
	}

	private void updatePassword(Long userId, byte[] salt, String currentTime) throws Exception {
		PreparedStatement pstmt = DBUtil.con.prepareStatement(DBUtil.INSERT_USER_SALT);
		pstmt.setLong(1, userId);
		pstmt.setBytes(2, salt);
		pstmt.setString(3, currentTime);
		pstmt.execute();
	}

	private String performPrechecks(String firstName, String lastName, String emailId, String phoneNumber, String password) throws Exception {
		if (firstName == null || firstName.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.FIRST_NAME_EMPTY).toString();
		}
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		if (!CommonUtils.checkIfEmailIsValid(emailId)) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_NOT_VALID).toString();
		}
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.PHONE_NUMBER_IS_EMPTY).toString();
		}
		if (password == null || password.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.PASSWORD_EMPTY).toString();
		}
		if (!CommonUtils.checkIfPasswordIsValid(password)) {
			return CommonUtils.generateResponse(APIResponse.PASSWORD_NOT_COMPLIANT).toString();
		}
		if (CommonUtils.checkIfEmailExists(emailId)) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_ALREADY_REGISTERED).toString();
		}
		return null;
	}

	@GetMapping("/user/otp")
	public String generateOTP(String emailId) throws Exception {
		Random rnd = new Random();
		int otp = 100000 + rnd.nextInt(900000);
		String query = String.format(DBUtil.GET_USER_OTP, emailId);
		ResultSet rs = DBUtil.executeQuery(query);
		java.util.Date dt = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = sdf.format(dt);

		if (rs.next()) {
			Long userId = rs.getLong("ID");
			Long otpId = rs.getLong("OTP");
			String phoneNumber = rs.getString("PHONE_NUMBER");
			query = null;
			if (otpId == null || otpId != 0) {
				query = String.format(DBUtil.UPDATE_USER_OTP, otp, currentTime, userId);
			} else {
				query = String.format(DBUtil.CREATE_USER_OTP, userId, otp, currentTime);
			}
			DBUtil.insertOrUpdate(query);
			JSONObject response = CommonUtils.generateResponse(APIResponse.USER_OTP_GENERATED_SUCCESSFULLY);
			response.put("otp", otp);
			response.put("phone_number", phoneNumber);
			return response.toString();
		}

		return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
	}

	@PostMapping("/user/otp")
	public String verifyUser(String emailId, int otp) throws Exception {
		String selectQuery = String.format(DBUtil.GET_USER_OTP, emailId);
		ResultSet rs = DBUtil.executeQuery(selectQuery);
		if (rs.next()) {
			Long otp_local = rs.getLong("OTP");
			String createdTime = rs.getString("CREATED_TIME");
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date otpCreatedTime = format.parse(createdTime);
			long millis = otpCreatedTime.getTime();
			long elapsedMillis = System.currentTimeMillis() - millis;

			if (elapsedMillis > TimeUnit.MINUTES.toMillis(10)) {
				return CommonUtils.generateResponse(APIResponse.OTP_EXPIRED).toString();
			}
			if (otp_local == 0) {
				return CommonUtils.generateResponse(APIResponse.OTP_NOT_CREATED).toString();
			} else if (otp_local == otp) {
				return CommonUtils.generateResponse(APIResponse.USER_VERIFIED_SUCCESSFULLY).toString();
			} else {
				return CommonUtils.generateResponse(APIResponse.USER_VERIFICATION_FAILED).toString();
			}
		}
		return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
	}

	@PostMapping("/user/login")
	public String loginUser(String emailId, String password) throws Exception {
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		if (password == null || password.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.PASSWORD_EMPTY).toString();
		}
		String query = String.format(DBUtil.FETCH_USER_PASSWORD, emailId);
		ResultSet rs = DBUtil.executeQuery(query);
		if (rs.next()) {
			String user_password = rs.getString("PASSWORD");
			byte[] salt = rs.getBytes("SALT");
			String hashedPassword = CommonUtils.generateHashForString(password, salt);
			if (user_password.trim().equals(hashedPassword)) {
				return CommonUtils.generateResponse(APIResponse.USER_LOGIN_SUCCESSFUL).toString();
			} else {
				return CommonUtils.generateResponse(APIResponse.PASSWORD_MISMATCH).toString();
			}
		}
		return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
	}

	@PostMapping("/user/password")
	public String resetPassword(String emailId, String password) throws Exception {
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		if (password == null || password.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.PASSWORD_EMPTY).toString();
		}
		ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_OTP, emailId));
		if (rs.next() == false) {
			return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
		}
		byte[] salt = CommonUtils.generateSalt();
		String hashedPassword = CommonUtils.generateHashForString(password, salt);
		String query = String.format(DBUtil.UPDATE_USER_PASSWORD, hashedPassword, emailId);
		int rowsAffected = DBUtil.update(query);
		if (rowsAffected == 0) {
			return CommonUtils.generateResponse(APIResponse.PASSWORD_UPDATE_FAILED).toString();
		} else {
			String currentTime = CommonUtils.getCurrentTime();
			updatePassword(salt, currentTime, emailId);
			return CommonUtils.generateResponse(APIResponse.PASSWORD_RESET_SUCCESSFULLY).toString();
		}
	}

	private void updatePassword(byte[] salt, String currentTime, String emailId) throws Exception {
		PreparedStatement pstmt = DBUtil.con.prepareStatement(DBUtil.UPDATE_USER_SALT);
		pstmt.setBytes(1, salt);
		pstmt.setString(2, currentTime);
		pstmt.setString(3, emailId);
		pstmt.execute();
	}

	@PostMapping("/user/delete")
	public String deleteUser(@RequestParam(required = true) String emailId) {
		return "delete User";
	}
	
	@PostMapping("/user/allergy")
	public String addUserAllergy(@RequestParam(required = true)String emailId, String commaSeparatedAllergy) throws Exception {
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_OTP, emailId));
		if (rs.next() == false) {
			return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
		}
		String query = String.format(DBUtil.UPDATE_USER_ALLERGY, commaSeparatedAllergy, emailId);
		int rowsAffected = DBUtil.update(query);
		if(rowsAffected == 0) {
			return CommonUtils.generateResponse(APIResponse.USER_ALLERGY_UPDATED_FAILED).toString();
		}
		return CommonUtils.generateResponse(APIResponse.USER_ALLERGY_UPDATED_SUCCESSFULLY).toString();
	
	}


}
