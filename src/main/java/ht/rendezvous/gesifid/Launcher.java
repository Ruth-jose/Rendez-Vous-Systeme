package ht.rendezvous.gesifid;

/**
 * Lanceur auxiliaire pour contourner la vérification de la JVM concernant JavaFX.
 * Cette classe ne doit pas hériter de 'javafx.application.Application'.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
