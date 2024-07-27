package com.gabriel.integration.inve.model;

import lombok.Data;

import java.util.Date;

@Data
public class Inventory {
	private int id;
	private String name;
	private String itemLabel;
	private String itemBrand;
	private String itemDescription;
	private double quantity;
	private double price;
	private int categoryId;
	private String categoryName;
	private int supplierId;
	private String supplierName;
	private int storageId;
	private String storageName;
	private Date purchaseDate;
	private Date expiryDate;
	private int statusId;
	private String statusName;
	private Date lastUpdated;
	private Date created;
	@Override
	public String toString(){
		return name;
	}
}
