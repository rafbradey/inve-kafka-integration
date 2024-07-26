package com.gabriel.integration.inve.model;

import lombok.Data;

import java.util.Date;

@Data
public class Inventory {
	private int id;
	private String name;
	private int productId;
	private String productName;
	private int shelfId;
	private String shelfName;
	private int warehouseId;
	private String warehouseName;
	private double cost;
	private double quantity;
	private int uomId;
	private String uomName;
	private Date lastUpdated;
	private Date created;
	@Override
	public String toString(){
		return name;
	}
}
