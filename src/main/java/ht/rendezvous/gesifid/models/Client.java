package ht.rendezvous.gesifid.models;

import java.time.LocalDateTime;

/**
 * Représente un client de la quincaillerie éligible au programme de fidélité.
 */
public class Client {
    private int id;
    private String nom;
    private String prenom;
    private String telephone;
    private double totalAchatsCumules;
    private LocalDateTime dateInscription;

    public Client() {}

    public Client(int id, String nom, String prenom, String telephone, double totalAchatsCumules, LocalDateTime dateInscription) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.totalAchatsCumules = totalAchatsCumules;
        this.dateInscription = dateInscription;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public double getTotalAchatsCumules() {
        return totalAchatsCumules;
    }

    public void setTotalAchatsCumules(double totalAchatsCumules) {
        this.totalAchatsCumules = totalAchatsCumules;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    /**
     * Helper pour afficher le nom complet.
     */
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    @Override
    public String toString() {
        return getNomComplet() + " (" + telephone + ")";
    }
}
