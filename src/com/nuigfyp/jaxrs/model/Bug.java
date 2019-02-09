package com.nuigfyp.jaxrs.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import com.nuigfyp.jaxrs.model.Bug;

@XmlRootElement(name = "Bug")
public class Bug implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id, severity, project, active, bugClassification;
	private String reporterName, testerName, description, screenshot, document, startDate, endDate;
	// boolean active;

	public Bug() {
	}

	public Bug(int id, String reporterName, String testerName, String description, int severity, int project,
			String screenshot, String document, String startDate, String endDate, int active,
			int bugClassification) {
		super();
		this.id = id;
		this.severity = severity;
		this.project = project;
		this.reporterName = reporterName;
		this.testerName = testerName;
		this.description = description;
		this.screenshot = screenshot;
		this.document = document;
		this.startDate = startDate;
		this.endDate = endDate;
		this.active = active;
		this.bugClassification = bugClassification;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSeverity() {
		return severity;
	}

	public void setSeverity(int severity) {
		this.severity = severity;
	}

	public int getProject() {
		return project;
	}

	public void setProject(int project) {
		this.project = project;
	}

	public String getReporterName() {
		return reporterName;
	}

	public void setReporterName(String reporterName) {
		this.reporterName = reporterName;
	}

	public String getTesterName() {
		return testerName;
	}

	public void setTesterName(String testerName) {
		this.testerName = testerName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public int getBugClassification() {
		return bugClassification;
	}

	public void setBugClassification(int bugClassification) {
		this.bugClassification = bugClassification;
	}

	public int getActive() {
		return active;
	}

	public void setActive(int active) {
		this.active = active;
	}
}