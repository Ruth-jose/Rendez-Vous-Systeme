package ht.rendezvous.gesifid.models;

/**
 * Représente un article/matériau de construction en quincaillerie.
 */
public class Produit {
    private int id;
    private String codeBarre;
    private String nom;
    private String description;
    private double prixUnitaire;
    private int quantiteStock;
    private int seuilAlerte;

    public Produit() {}

    public Produit(int id, String codeBarre, String nom, String description, double prixUnitaire, int quantiteStock, int seuilAlerte) {
        this.id = id;
        this.codeBarre = codeBarre;
        this.nom = nom;
        this.description = description;
        this.prixUnitaire = prixUnitaire;
        this.quantiteStock = quantiteStock;
        this.seuilAlerte = seuilAlerte;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public int getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(int quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    public int getSeuilAlerte() {
        return seuilAlerte;
    }

    public void setSeuilAlerte(int seuilAlerte) {
        this.seuilAlerte = seuilAlerte;
    }

    /**
     * Indique si l'article est sous le seuil critique d'approvisionnement.
     */
    public boolean estEnAlerteStock() {
        return this.quantiteStock <= this.seuilAlerte;
    }

    @Override
    public String toString() {
        return nom + " (" + prixUnitaire + " HTG)";
    }
}
