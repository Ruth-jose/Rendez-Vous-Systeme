package ht.rendezvous.gesifid.services;

import ht.rendezvous.gesifid.models.LigneVente;
import ht.rendezvous.gesifid.models.Vente;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ImpressionService {

    /**
     * Formate un ticket de caisse en texte brut de 32 caractères de largeur pour imprimante 58mm.
     */
    public String genererTexteTicket(Vente vente, String nomClient, double tauxRemise) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Properties companyProps = loadCompanyProperties();
        String companyName = companyProps.getProperty("company.name");
        String companySlogan = companyProps.getProperty("company.slogan");
        String companyAddress = companyProps.getProperty("company.address");
        String companyPhone = companyProps.getProperty("company.phone");

        sb.append("================================\n");
        sb.append(centrerTexte(companyName, 32)).append("\n");
        if (companySlogan != null && !companySlogan.trim().isEmpty()) {
            sb.append(centrerTexte(companySlogan, 32)).append("\n");
        }
        sb.append(centrerTexte(companyAddress, 32)).append("\n");
        if (companyPhone != null && !companyPhone.trim().isEmpty()) {
            sb.append(centrerTexte("Tel: " + companyPhone, 32)).append("\n");
        }
        sb.append("================================\n");
        sb.append("Ticket No : #V-").append(String.format("%06d", vente.getId())).append("\n");
        sb.append("Date : ").append(vente.getDateVente().format(dtf)).append("\n");
        sb.append("Caissier : Caisse Principale\n");
        sb.append("--------------------------------\n");
        if (nomClient != null && !nomClient.trim().isEmpty()) {
            sb.append("Client : ").append(tronquer(nomClient, 23)).append("\n");
            sb.append("Remise Fidélité : ").append(String.format("%.1f", tauxRemise * 100)).append("%\n");
        } else {
            sb.append("Client : COMPTOIR / ANONYME\n");
        }
        sb.append("--------------------------------\n");
        sb.append("Articles:\n");
        sb.append("--------------------------------\n");

        for (LigneVente lv : vente.getLignesVente()) {
            String nom = lv.getNomProduit() != null ? lv.getNomProduit() : "Article #" + lv.getProduitId();
            sb.append(lv.getQuantite()).append(" x ").append(tronquer(nom, 23)).append("\n");
            
            String totalLigneStr = String.format("%,.2f HTG", lv.getTotalLigne());
            sb.append(remplirEspaces(32 - totalLigneStr.length())).append(totalLigneStr).append("\n");
        }

        sb.append("--------------------------------\n");
        
        String brutStr = String.format("%,.2f HTG", vente.getTotalBrut());
        sb.append("TOTAL BRUT :").append(remplirEspaces(20 - brutStr.length())).append(brutStr).append("\n");
        
        String remiseStr = String.format("%,.2f HTG", vente.getMontantRemise());
        sb.append("REMISE :").append(remplirEspaces(24 - remiseStr.length())).append(remiseStr).append("\n");
        
        String netStr = String.format("%,.2f HTG", vente.getTotalNet());
        sb.append("TOTAL NET :").append(remplirEspaces(21 - netStr.length())).append(netStr).append("\n");
        
        sb.append("================================\n");
        sb.append("Mode de Paiement : ").append(vente.getModePaiement()).append("\n");
        sb.append("================================\n");
        sb.append("Merci pour votre confiance !\n");
        sb.append("   A bientot chez Rendez-vous\n");
        sb.append("================================\n\n\n\n"); // Marges d'alimentation physique

        return sb.toString();
    }

    /**
     * Envoie directement le texte formaté à l'imprimante thermique par défaut du système Windows.
     */
    public void imprimerTicket(String ticketTexte) {
        try {
            // Recherche de l'imprimante par défaut
            PrintService serviceDefault = PrintServiceLookup.lookupDefaultPrintService();
            
            if (serviceDefault == null) {
                System.err.println("ERREUR IMPRESSION : Aucune imprimante configurée par défaut dans Windows.");
                return;
            }

            // Flux binaire du ticket en UTF-8
            InputStream is = new ByteArrayInputStream(ticketTexte.getBytes("UTF-8"));
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(is, flavor, null);

            DocPrintJob job = serviceDefault.createPrintJob();
            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            
            job.print(doc, aset);
            is.close();
            System.out.println("Impression envoyée avec succès à l'imprimante : " + serviceDefault.getName());
        } catch (Exception e) {
            System.err.println("ERREUR IMPRESSION : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String tronquer(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    private String remplirEspaces(int count) {
        if (count <= 0) return "";
        return " ".repeat(count);
    }

    private Properties loadCompanyProperties() {
        Properties props = new Properties();
        // Valeurs par défaut
        props.setProperty("company.name", "ENTREPRISE RENDEZ-VOUS");
        props.setProperty("company.slogan", "\"Bâtir l'avenir en confiance\"");
        props.setProperty("company.address", "Cayes-Jacmel, Sud-Est, Haïti");
        props.setProperty("company.phone", "509-3777-6655");

        java.io.File propFile = new java.io.File("database.properties");
        if (propFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(propFile);
                 java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8)) {
                Properties loaded = new Properties();
                loaded.load(isr);
                for (String key : loaded.stringPropertyNames()) {
                    if (key.startsWith("company.")) {
                        props.setProperty(key, loaded.getProperty(key));
                    }
                }
            } catch (java.io.IOException e) {
                System.err.println("Erreur lors de la lecture des propriétés de l'entreprise : " + e.getMessage());
            }
        }
        return props;
    }

    private String centrerTexte(String str, int width) {
        if (str == null) return "";
        if (str.length() >= width) return str.substring(0, width);
        int padding = (width - str.length()) / 2;
        return " ".repeat(padding) + str;
    }
}
