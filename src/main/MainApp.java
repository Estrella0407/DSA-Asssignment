/*
 * Module: Main Application Driver
 * Author:  WEI XIN
 * 
 * Description:
 * Main entry point class for launching the TARUMT Resorts Management System.
 */
package main;

import boundary.MainMenuUI;

public class MainApp {

    public static void main(String[] args) {
        MainMenuUI menu = new MainMenuUI();
        menu.displayMainMenu();
    }
}
