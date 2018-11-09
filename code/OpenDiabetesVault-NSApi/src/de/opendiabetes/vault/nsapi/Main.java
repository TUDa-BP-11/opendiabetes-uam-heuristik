package de.opendiabetes.vault.nsapi;

public class Main {
    public static void main(String[] args) {
        NSApi api = new NSApi("https://uam-bp11.ns.10be.de:22577/");
        System.out.println(api.getStatus());
    }
}
