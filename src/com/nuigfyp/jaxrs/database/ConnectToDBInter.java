package com.nuigfyp.jaxrs.database;

import java.sql.SQLException;
import java.util.List;

import com.nuigfyp.jaxrs.model.Bug;

public interface ConnectToDBInter {
	
	public String changeBugStatus (int id, String todaysDate) throws SQLException;
	public String deleteBugAndFiles(int id) throws SQLException;
	public String updateDB(Bug bug, int id) throws SQLException;
	// check if searchforbug is required
	public boolean addEntry(Bug bug) throws SQLException;
	public List<Bug> getAllBugs();
}
