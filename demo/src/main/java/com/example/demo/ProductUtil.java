package com.example.demo;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductUtil {
	public static final String UPC_API_KEY = "5A7E28020FB2A4F78A8DE783FF2B3444";
	public static final String UPC_URL = "https://api.upcdatabase.org/product/%s?apikey=5A7E28020FB2A4F78A8DE783FF2B3444";

	@GetMapping("/product")
	public String fetchUserProducts(String emailId) throws Exception {
		if (emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.FETCH_USER_PRODUCTS, emailId));
		JSONObject response = CommonUtils.generateResponse(APIResponse.USER_PRODUCTS_FETCHED_SUCCESSFULLY);
		JSONArray arr = new JSONArray();
		while (rs.next()) {
			JSONObject object = new JSONObject();
			object.put("product_name", rs.getString("PRODUCT_NAME"));
			object.put("manufacturer", rs.getString("MANUFACTURER"));
			object.put("expiry_date", rs.getDate("EXPIRY_DATE"));
			object.put("count", rs.getInt("COUNT"));
			// TODO: check in which color the product should be displayed.
			arr.put(object);
		}
		response.put("product_details", arr);
		return response.toString();
	}

	@PostMapping("/product")
	public String updateUserProducts(String emailId, long productId, String expiryDate) throws Exception {
		if (emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_ID_FOR_EMAIL, emailId));
		if(!rs.next()) {
			return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
		}
		Long userId = rs.getLong("ID");
		JSONObject response = fetchProductDetails(productId);

		rs = DBUtil.executeQuery(String.format(DBUtil.SELECT_PRODUCT, response.getString("description"), response.getString("manufacturer")));
		Long product = 0L;
		if(rs.next()) {
			product = rs.getLong("ID");
		}
		int count = 0;
		if (product != 0) {
			rs = DBUtil.executeQuery(String.format(DBUtil.SELECT_USER_PRODUCT, product, userId));
			while (rs.next()) {
				count = rs.getInt("COUNT");
			}
		} else {
			product = DBUtil.insertOrUpdate(String.format(DBUtil.INSERT_PRODUCT, response.getString("description"), response.getString("manufacturer")));
		}
		count++;
		if(count == 1) {
			DBUtil.insertOrUpdate(String.format(DBUtil.INSERT_USER_PRODUCTS, product, userId, count, java.sql.Date.valueOf(expiryDate)));
		}else {
			String query = String.format(DBUtil.UPDATE_USER_PRODUCTS, count, userId, product);
			DBUtil.insertOrUpdate(query);
		}
		JSONObject obj =  CommonUtils.generateResponse(APIResponse.USER_PRODUCTS_FETCHED_SUCCESSFULLY);
		return obj.toString();
	}

	private JSONObject fetchProductDetails(long productId) throws Exception {
		String url = String.format(UPC_URL, productId);
		String response = CommonUtils.httpGetCall(url);
		return new JSONObject(response);
	}
}
