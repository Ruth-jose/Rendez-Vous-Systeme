package ht.rendezvous.gesifid.models;

/**
 * Représente un article individuel et sa quantité au sein d'une facture.
 */
public class LigneVente {
    private int id;
    private int venteId;
    private int produitId;
    private int quantite;
    private double prixUnitaireFacture;
    private String nomProduit; // Attribut temporaire utile pour l'affichage dans le tableau FXML
    private String codeProduitHelper; // Code barre temporaire pour l'affichage

    public LigneVente() {}

    public LigneVente(int id, int venteId, int produitId, int quantite, double prixUnitaireFacture) {
        this.id = id;
        this.venteId = venteId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaireFacture = prixUnitaireFacture;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVenteId() {
        return venteId;
    }

    public void setVenteId(int venteId) {
        this.venteId = venteId;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public double getPrixUnitaireFacture() {
        return prixUnitaireFacture;
    }

    public void setPrixUnitaireFacture(double prixUnitaireFacture) {
        this.prixUnitaireFacture = prixUnitaireFacture;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    /**
     * Calcule le total de la ligne de facture.
     */
    public double getTotalLigne() {
        return quantite * prixUnitaireFacture;
    }

    public String getCodeProduitHelper() {
        return codeProduitHelper;
    }

    public void setCodeProduitHelper(String codeProduitHelper) {
        this.codeProduitHelper = codeProduitHelper;
    }
}
