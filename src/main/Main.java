package main;

public class Main{
	
	public static void main(String[] args){
		GUI window = new GUI();
		window.setBounds(300, 300, 200, 200);
		window.setExtendedState(GUI.MAXIMIZED_BOTH);
		//window.setUndecorated(true);
		window.setDefaultCloseOperation(GUI.DO_NOTHING_ON_CLOSE);
		window.setVisible(true);
	}	
}