package ht.rendezvous.gesifid.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente l'entête d'une transaction de vente (facturation).
 */
public class Vente {
    private int id;
    private LocalDateTime dateVente;
    private Integer clientId; // Peut être null pour un client anonyme au comptoir
    private double totalBrut;
    private double montantRemise;
    private double totalNet;
    private String modePaiement; // CASH, MONCASH, VIREMENT, CHEQUE
    private List<LigneVente> lignesVente = new ArrayList<>();

    public Vente() {}

    public Vente(int id, LocalDateTime dateVente, Integer clientId, double totalBrut, double montantRemise, double totalNet, String modePaiement) {
        this.id = id;
        this.dateVente = dateVente;
        this.clientId = clientId;
        this.totalBrut = totalBrut;
        this.montantRemise = montantRemise;
        this.totalNet = totalNet;
        this.modePaiement = modePaiement;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateVente() {
        return dateVente;
    }

    public void setDateVente(LocalDateTime dateVente) {
        this.dateVente = dateVente;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public double getTotalBrut() {
        return totalBrut;
    }

    public void setTotalBrut(double totalBrut) {
        this.totalBrut = totalBrut;
    }

    public double getMontantRemise() {
        return montantRemise;
    }

    public void setMontantRemise(double montantRemise) {
        this.montantRemise = montantRemise;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(double totalNet) {
        this.totalNet = totalNet;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public List<LigneVente> getLignesVente() {
        return lignesVente;
    }

    public void setLignesVente(List<LigneVente> lignesVente) {
        this.lignesVente = lignesVente;
    }

    public void ajouterLigne(LigneVente ligne) {
        this.lignesVente.add(ligne);
        recalculerTotaux();
    }

    /**
     * Recalcule les totaux brut et net basés sur les lignes et la remise déjà configurée.
     */
    public void recalculerTotaux() {
        this.totalBrut = 0.0;
        for (LigneVente ligne : lignesVente) {
            this.totalBrut += ligne.getQuantite() * ligne.getPrixUnitaireFacture();
        }
        this.totalNet = this.totalBrut - this.montantRemise;
    }
}
