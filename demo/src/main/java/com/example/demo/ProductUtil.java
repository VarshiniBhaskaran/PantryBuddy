package com.example.demo;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
	// public static final String UPC_API_KEY = "5A7E28020FB2A4F78A8DE783FF2B3444";
	public static final String API_URL = "https://go.littlebunch.com/v1/food/%s";

	@GetMapping("/product")
	public String fetchUserProducts(String emailId) throws Exception {
		if (emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		JSONObject response = CommonUtils.generateResponse(APIResponse.USER_PRODUCTS_FETCHED_SUCCESSFULLY);
		try (ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.FETCH_USER_PRODUCTS, emailId))) {
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject object = new JSONObject();
				object.put("product_name", rs.getString("PRODUCT_NAME"));
				object.put("manufacturer", rs.getString("MANUFACTURER"));
				object.put("expiry_date", rs.getDate("EXPIRY_DATE"));
				object.put("category", rs.getString("CATEGORY"));
				object.put("serving_size", rs.getString("SERVING_SIZE"));
				object.put("ingredients", rs.getString("INGREDIENTS"));
				object.put("count", rs.getInt("COUNT"));
				String image = rs.getString("IMAGE");
				System.out.println("image is" + image);
				if (image == null) {
					object.put("image", "default_img");
				} else {
					object.put("image", rs.getString("IMAGE"));
				}
				String allergy = rs.getString("ALLERGY");
				System.out.println("user allergy allergy is " + allergy);
				System.out.println("ingredients" + rs.getString("INGREDIENTS"));

				if (allergy != null && !allergy.trim().isEmpty()) {
					String[] allergyArr = allergy.split(",");
					System.out.println(allergyArr + Arrays.toString(allergyArr));
					String content = rs.getString("INGREDIENTS").toLowerCase();
					System.out.println("ingredients" + content);
					for (String all : allergyArr) {
						System.out.println("all is " + all);
						if (content.contains(all.toLowerCase())) {
							object.put("is_allergic", true);
							System.out.println("allergy added");
							break;
						}
					}
				}
				arr.put(object);
			}

			response.put("product_details", arr);
		}
		return response.toString();
	}

	@PostMapping("/product")
	public String updateUserProducts(String emailId, String productId, String expiryDate) throws Exception {
		System.out.println("expiry date" + expiryDate);
		if (emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		try (ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_ID_FOR_EMAIL, emailId))) {
			if (!rs.next()) {
				return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
			}
			Long userId = rs.getLong("ID");
			JSONObject response = fetchProductDetails(productId);
			System.out.println("product id is " + productId);
			System.out.println("====================================");
			System.out.println("the json response is " + response);
			System.out.println("====================================");
			JSONObject food_details = response.getJSONArray("items").getJSONObject(0);
			String description = food_details.getString("foodDescription");
			String company = food_details.getString("company");
			String ingredients = food_details.getString("ingredients");
			String servingSize = food_details.getJSONArray("servingSizes").getJSONObject(0).getString("servingUnit");
			String category = food_details.getJSONObject("foodGroup").getString("description");
			try (ResultSet rs1 = DBUtil.executeQuery(String.format(DBUtil.SELECT_PRODUCT, description, company))) {
				Long product = 0L;
				if (rs1.next()) {
					product = rs1.getLong("ID");
				}
				int count = 0;
				if (product != 0) {
					try (ResultSet rs2 = DBUtil.executeQuery(String.format(DBUtil.SELECT_USER_PRODUCT, product, userId, java.sql.Date.valueOf(expiryDate)))) {
						while (rs2.next()) {
							count = rs.getInt("COUNT");
						}
					}
				} else {
					product = DBUtil.insertOrUpdate(String.format(DBUtil.INSERT_PRODUCT, description, company, category, ingredients, servingSize));
				}
				count++;
				if (count == 1) {
					DBUtil.insertOrUpdate(String.format(DBUtil.INSERT_USER_PRODUCTS, product, userId, count, java.sql.Date.valueOf(expiryDate)));
				} else {
					String query = String.format(DBUtil.UPDATE_USER_PRODUCTS, count, userId, product, java.sql.Date.valueOf(expiryDate));
					DBUtil.insertOrUpdate(query);
				}
			}
			JSONObject obj = CommonUtils.generateResponse(APIResponse.USER_PRODUCTS_UPDATED_SUCCESSFULLY);
			return obj.toString();
		}
	}

	private JSONObject fetchProductDetails(String productId) throws Exception {
		String url = String.format(API_URL, productId);
		System.out.println("==============================");
		System.out.println("the incoming url is " + url);
		System.out.println("==========================");
		String response = CommonUtils.httpGetCall(url);
		return new JSONObject(response);
	}

	@PostMapping("/productManual")
	public String updateUserProductsManually(String emailId, String itemName, String expiryDate) throws Exception {
		System.out.println("email : " + emailId + " itemname " + itemName + " expiry " + expiryDate);
		if (emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		try (ResultSet rs = DBUtil.executeQuery(String.format(DBUtil.GET_USER_ID_FOR_EMAIL, emailId))) {
			if (!rs.next()) {
				return CommonUtils.generateResponse(APIResponse.USER_NOT_FOUND).toString();
			}
			Long userId = rs.getLong("ID");
			try (ResultSet rs1 = DBUtil.executeQuery(String.format(DBUtil.SELECT_PRODUCT, itemName, null))) {
				Long product = 0L;
				if (rs1.next()) {
					product = rs.getLong("ID");
				}
				int count = 0;
				if (product != 0) {
					try (ResultSet rs2 = DBUtil.executeQuery(String.format(DBUtil.SELECT_USER_PRODUCT, product, userId, java.sql.Date.valueOf(expiryDate)))) {
						while (rs2.next()) {
							count = rs.getInt("COUNT");
						}
					}
				} else {
					product = DBUtil.insertOrUpdate(String.format(DBUtil.INSERT_PRODUCT, itemName, null, null, null, null));
				}
				count++;
				if (count == 1) {
					DBUtil.insertOrUpdate(String.format(DBUtil.INSERT_USER_PRODUCTS, product, userId, count, java.sql.Date.valueOf(expiryDate)));
				} else {
					String query = String.format(DBUtil.UPDATE_USER_PRODUCTS, count, userId, product, java.sql.Date.valueOf(expiryDate));
					DBUtil.insertOrUpdate(query);
				}
			}

		}
		JSONObject obj = CommonUtils.generateResponse(APIResponse.USER_PRODUCTS_UPDATED_SUCCESSFULLY);
		return obj.toString();
	}

	@PostMapping("/product/delete")
	public String deleteProduct(String emailId, String productName, String expiryDate) throws Exception {
		if (emailId.trim().isEmpty()) {
			return CommonUtils.generateResponse(APIResponse.EMAIL_ID_EMPTY).toString();
		}
		String query = String.format(DBUtil.FETCH_PRODUCT, emailId, productName, java.sql.Date.valueOf(expiryDate));
		try (ResultSet rs = DBUtil.executeQuery(query)) {
			if (rs.next()) {
				int count = rs.getInt("COUNT");
				--count;
				if (count == 0) {
					query = String.format(DBUtil.DELETE_USER_PRODUCT, emailId, productName, java.sql.Date.valueOf(expiryDate));
					DBUtil.update(query);
				} else {
					query = String.format(DBUtil.UPDATE_PRODUCT_COUNT, count, emailId, productName, java.sql.Date.valueOf(expiryDate));
					DBUtil.update(query);
				}
			}
		}

		return CommonUtils.generateResponse(APIResponse.USER_PRODUCTS_DELETED_SUCCESSFULLY).toString();
	}
}
