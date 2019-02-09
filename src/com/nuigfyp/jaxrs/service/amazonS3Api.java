package com.nuigfyp.jaxrs.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;


public class amazonS3Api {

	private static String appId = "AKIAJ5VZIEJOX3CNWRYQ";
	private static String appSecret = "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";
	private final static String BUCKETNAME = "bug-reporter-bucket";
	// String Bucket__Name = "bug-reporter-bucket-" + UUID.randomUUID();


	public static boolean deleteFileFromS3(String key) {

		AmazonS3 s3 = setCredentials();		
		//System.out.println("@DELETE file named " + key + " from the Bucket: " + BUCKETNAME);		
		//boolean fileExist = s3.doesObjectExist(BUCKETNAME, key);
		
		// ========= Uncomment fileexist out and publish to Beanstalk. Works. Not working locally. But why do extra GET, Cost?
		// ========= Maybe generate a new URL and test locally by adding URL to the Client
		try {	
			
			//if(fileExist) {
				s3.deleteObject(BUCKETNAME, key);
				return true;
			//}	
			
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		
		return false; 
	}
	
	
	
	public static File getFileFromS3(String key) {

		AmazonS3 s3 = setCredentials();
		InputStream inputStream = null;
		File targetFile = null;

		//System.out.println("Amazon AWS S3 Running... getting file named " + key + " from the Bucket: " + BUCKETNAME);

		try {
			
			inputStream = s3.getObject(BUCKETNAME, key).getObjectContent();
			targetFile = File.createTempFile("s3test", "");
			targetFile.deleteOnExit();	
			
			//Without specifying the option to replace existing file, you'll get a FileAlreadyExistsException
			Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);			
			return targetFile;

		} catch (AmazonServiceException ase) {
			
			if (ase.getStatusCode() == HttpStatus.SC_NOT_FOUND || ase.getStatusCode() == 404) {
				System.out.println("The file was not found, and is a retunr code of 404. " + ase.getStatusCode());
		    }
			
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
			
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IO Exception Caught in getFileFromS3 file: " + e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		return targetFile;
	}

	public void saveFileToS3(File file, String finalFileDirectory) {

		AmazonS3 s3 = setCredentials();

		// https://stackoverflow.com/questions/45994046/upload-pdf-or-image-file-to-aws-bucket-java
		System.out.println("Amazon saveFileToS3() is running... adding file " + file.getName() + " to the Bucket: " + BUCKETNAME
				+ ", and will be called " + finalFileDirectory + " when in the database.");

		try {

			s3.putObject(new PutObjectRequest(BUCKETNAME, finalFileDirectory, file));

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	private static AmazonS3 setCredentials() {

		AWSCredentials credentials = new BasicAWSCredentials(appId, appSecret);
		AmazonS3 s3 = new AmazonS3Client(credentials);
		
		/*AWSCredentials credentials = null;

		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (C:\\Users\\kevin\\.aws\\credentials), and is in valid format.", e);
		}

		AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion("eu-west-1").build();*/

		/*
		 * AWSCredentials credentials = new
		 * ProfileCredentialsProvider("default").getCredentials(); AmazonS3 s3 =
		 * AmazonS3ClientBuilder.standard() .withCredentials(new
		 * AWSStaticCredentialsProvider(credentials)) .withRegion("eu-west-1") .build();
		 */
		return s3;
	}


	public static void timerDelay() {
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}
