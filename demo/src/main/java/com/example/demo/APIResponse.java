package com.example.demo;

public enum APIResponse {
	
	USER_CREATED_SUCCESSFULLY(201,"User created successfully."),
	USER_UPDATED_SUCCESSFULLY(201,"User details updated successfully."),
	
	//User Module Response Codes
	FIRST_NAME_EMPTY(100,"First Name cannot be empty"),
	EMAIL_ID_EMPTY(101,"Email id cannot be empty"),
	EMAIL_NOT_VALID(102,"Email id is not valid"),
	PHONE_NUMBER_IS_EMPTY(103,"Phone number cannot be empty"),
	PHONE_NUMBER_NOT_VALID(104,"Phone number is not valid"),
	PASSWORD_EMPTY(105,"Password cannot be empty"),
	PASSWORD_NOT_COMPLIANT(106, "Password does not comply with the rules. Please ensure to have upper case, lowercase letters and numbers in your password"), 
	EMAIL_ID_ALREADY_REGISTERED(107,"This email Id is already registered. Please use the 'Forget Password' option if you forgot your password or register with a different Id"), 
	USER_CREATION_FAILED(108, "Something went wrong while creating user. Please contact support"), 
	PHONE_ALREADY_REGISTERED(109,"This phone number is already registered. Please use the 'Forget Password' option if you forgot your password or register with a different number"), 
	USER_OTP_GENERATED_SUCCESSFULLY(200, "OTP Generated successfully"), 
	USER_NOT_FOUND(110,"User Not Found. Please check emailId"), 
	OTP_EXPIRED(111, "Your OTP has expired. Please regenerate it to continue"), 
	OTP_NOT_CREATED(112,"OTP not created for the user yet"), 
	USER_VERIFIED_SUCCESSFULLY(200, "User verified successfully"),
	USER_VERIFICATION_FAILED(114, "Failed to verify user. Please enter the correct OTP"),
	USER_LOGIN_SUCCESSFUL(200, "User Login successful"), 
	PASSWORD_MISMATCH(116, "Password is incorrect. Please try again"),
	PASSWORD_UPDATE_FAILED(117,"Password update failed. Please contact support"), 
	PASSWORD_RESET_SUCCESSFULLY(200, "Password reset successfully"),
	USER_ALLERGY_UPDATED_FAILED(118, "Issue occurred while updating user allergy details. Please contact support"),
	USER_ALLERGY_UPDATED_SUCCESSFULLY(200, "User allergy details updated successfully"),
	USER_FETCHED_SUCCESSFULLY(205, "User details fetched successfully"),
	USER_UPDATION_FAILED(119, "Failed to update user."),
	
	//PRODUCT MODULE
	USER_PRODUCTS_FETCHED_SUCCESSFULLY(200, "User Products fetched successfully"),
	USER_PRODUCTS_UPDATED_SUCCESSFULLY(200, "User Updated fetched successfully"), 
	EXPIRED_PRODUCTS_FETCHED_SUCCESSFULLY(200, "User's expired products fetched successfully"),
	EXPIRED_PRODUCTS_FAILED(200, "Failed to fetch User's expired products"), 
	USER_PRODUCTS_DELETED_SUCCESSFULLY(208, "User Product deleted successfully");

	
	int code;
	String message;
	
	public int getCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
	
	APIResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}
		
}
