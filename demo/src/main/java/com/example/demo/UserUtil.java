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
	private static final int ACTIVE = 1;

	@PostMapping("/user/create")
	public String createUser(String firstName, String lastName, String emailId, String phoneNumber, String password) throws Exception {
		String precheckResponse = performPrechecks(firstName, lastName, emailId, phoneNumber, password, true);
		if (precheckResponse != null) {
			return precheckResponse;
		}
		String currentTime = CommonUtils.getCurrentTime();
		String name = firstName + "###" + lastName;

		byte[] salt = CommonUtils.generateSalt();

		String hashedPassword = CommonUtils.generateHashForString(password, salt);

		String query = String.format(DBUtil.CREATE_USER, name, emailId, phoneNumber, hashedPassword, currentTime);
		Long userId = DBUtil.insertOrUpdate(query);

		EmailServiceImpl.sendWelcomeEmail(emailId);
		if (userId != null && userId != 0) {
			updatePassword(userId, salt, currentTime);
			return CommonUtils.generateResponse(APIResponse.USER_CREATED_SUCCESSFULLY).toString();
		} else {
			return CommonUtils.generateResponse(APIResponse.USER_CREATION_FAILED).toString();
		}
	}

	@PostMapping("/user/edit")
	public String editUser(String firstName, String lastName, String emailId, String phoneNumber, String password) throws Exception {
		String precheckResponse = performPrechecks(firstName, lastName, emailId, phoneNumber, password, false);
		if (precheckResponse != null) {
			return precheckResponse;
		}
		String currentTime = CommonUtils.getCurrentTime();
		String name = firstName + "###" + lastName;

		byte[] salt = CommonUtils.generateSalt();

		String hashedPassword = CommonUtils.generateHashForString(password, salt);

		String query = String.format(DBUtil.UPDATE_USER, name, phoneNumber, hashedPassword, currentTime, emailId);
		int updatedRows = DBUtil.update(query);
		if (updatedRows != 0) {
			updatePassword(salt, currentTime, emailId);
			return CommonUtils.generateResponse(APIResponse.USER_UPDATED_SUCCESSFULLY).toString();
		} else {
			return CommonUtils.generateResponse(APIResponse.USER_UPDATION_FAILED).toString();
		}
	}

	private void updatePassword(Long userId, byte[] salt, String currentTime) throws Exception {
		PreparedStatement pstmt = DBUtil.con.prepareStatement(DBUtil.INSERT_USER_SALT);
		pstmt.setLong(1, userId);
		pstmt.setBytes(2, salt);
		pstmt.setString(3, currentTime);
		pstmt.execute();
	}

	private String performPrechecks(String firstName, String lastName, String emailId, String phoneNumber, String password, boolean isSignup) throws Exception {
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
		if (isSignup) {
			if (CommonUtils.checkIfEmailExists(emailId)) {
				return CommonUtils.generateResponse(APIResponse.EMAIL_ID_ALREADY_REGISTERED).toString();
			}
			if (CommonUtils.checkIfPhoneNumberExists(phoneNumber)) {
				return CommonUtils.generateResponse(APIResponse.PHONE_ALREADY_REGISTERED).toString();
			}
		}
		return null;
	}

	@GetMapping("/user/otp")
	public String generateOTP(String emailId) throws Exception {
		Random rnd = new Random();
		int otp = 100000 + rnd.nextInt(900000);
		String query = String.format(DBUtil.GET_USER_OTP, emailId);
		try (ResultSet rs = DBUtil.executeQuery(query)) {
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
		}

		return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
	}

	@PostMapping("/user/otp")
	public String verifyUser(String emailId, int otp) throws Exception {
		String selectQuery = String.format(DBUtil.GET_USER_OTP, emailId);
		try (ResultSet rs = DBUtil.executeQuery(selectQuery)) {
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
					updateUserStatus(emailId, ACTIVE);
					return CommonUtils.generateResponse(APIResponse.USER_VERIFIED_SUCCESSFULLY).toString();
				} else {
					return CommonUtils.generateResponse(APIResponse.USER_VERIFICATION_FAILED).toString();
				}
			}
		}
		return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
	}

	private void updateUserStatus(String emailId, int status) throws Exception {
		String query = String.format(DBUtil.MARK_USER_STATUS, status, emailId);
		DBUtil.update(query);
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
		try (ResultSet rs = DBUtil.executeQuery(query)) {
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
		try (ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_OTP, emailId))) {
			if (rs.next() == false) {
				return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
			}
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
			updateUserStatus(emailId, ACTIVE);
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

	@GetMapping("/user/fetch")
	public String fetchUser(@RequestParam(required = true) String emailId) throws Exception {
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		String query = String.format(DBUtil.FETCH_USER, emailId);
		JSONObject response = null;
		try (ResultSet rs = DBUtil.executeQuery(query)) {
			if (rs.next()) {
				response = CommonUtils.generateResponse(APIResponse.USER_FETCHED_SUCCESSFULLY);
				String[] name = rs.getString("NAME").split("###");
				response.put("first_name", name[0]);
				response.put("last_name", name[1]);
				response.put("emailId", rs.getString("EMAIL_ID"));
				response.put("phoneNumber", rs.getString("PHONE_NUMBER"));
				response.put("allergy", rs.getString("ALLERGY"));
				response.put("isActive", rs.getString("ACTIVE"));
			} else {
				response = CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND);
			}
		}
		return response.toString();
	}

	@PostMapping("/user/allergy")
	public String addUserAllergy(@RequestParam(required = true) String emailId, String commaSeparatedAllergy) throws Exception {
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		try (ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_OTP, emailId))) {
			if (rs.next() == false) {
				return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
			}
		}
		String query = String.format(DBUtil.UPDATE_USER_ALLERGY, commaSeparatedAllergy, emailId);
		int rowsAffected = DBUtil.update(query);
		if (rowsAffected == 0) {
			return CommonUtils.generateResponse(APIResponse.USER_ALLERGY_UPDATED_FAILED).toString();
		}
		return CommonUtils.generateResponse(APIResponse.USER_ALLERGY_UPDATED_SUCCESSFULLY).toString();

	}
	
	@GetMapping("/expired/products/fetch")
	public String fetchExpiredProducts(@RequestParam(required = true) String emailId) throws Exception {
		if (emailId == null || emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		try (ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_OTP, emailId))) {
			if (rs.next() == false) {
				return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
			}
		}
		String query = String.format(DBUtil.FETCH_EXPIRED_FOOD, emailId);
		JSONObject resp = CommonUtils.generateResponse(APIResponse.EXPIRED_PRODUCTS_FETCHED_SUCCESSFULLY);
		try(ResultSet rs = DBUtil.executeQuery(query)){
			if(rs.next()) {
				resp.put("count", rs.getString("COUNT"));
			}
		}catch(Exception e) {
			return CommonUtils.generateResponse(APIResponse.EXPIRED_PRODUCTS_FAILED).toString();
		}
		return resp.toString();
	}

}
