package com.cdd.bao.util;

import javafx.scene.control.*;

public class UtilGUI
{

	/**
	 * Displays a helpful informational message.
	 */	
	public static void informMessage(String title, String msg)
	{
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

	/**
	 * Displays a message with warning theme.
	 */
	public static void informWarning(String title, String msg)
	{
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

}
