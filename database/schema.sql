-- ============================================================================
-- SCRIPT D'INITIALISATION DE LA BASE DE DONNÉES - SYSTEME GESIFID
-- ============================================================================
-- Client : Entreprise Rendez-vous (Quincaillerie)
-- Base de données : PostgreSQL
-- Auteur : Antigravity IDE Agent
-- Date : 27 Mai 2026
-- ============================================================================

-- Nettoyage des tables existantes (pour éviter les conflits lors des tests)
DROP TABLE IF EXISTS ligne_vente CASCADE;
DROP TABLE IF EXISTS vente CASCADE;
DROP TABLE IF EXISTS client CASCADE;
DROP TABLE IF EXISTS produit CASCADE;

-- 1. Table des produits (matériaux de construction et outillage)
CREATE TABLE produit (
    id SERIAL PRIMARY KEY,
    code_barre VARCHAR(50) UNIQUE,
    nom VARCHAR(100) NOT NULL,
    description TEXT,
    prix_unitaire NUMERIC(12, 2) NOT NULL CHECK (prix_unitaire >= 0),
    quantite_stock INTEGER NOT NULL DEFAULT 0 CHECK (quantite_stock >= 0),
    seuil_alerte INTEGER NOT NULL DEFAULT 5 CHECK (seuil_alerte >= 0)
);

-- 2. Table des clients (suivi du programme de fidélité)
CREATE TABLE client (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    telephone VARCHAR(20) UNIQUE NOT NULL,
    total_achats_cumules NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (total_achats_cumules >= 0),
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Table des ventes (entêtes de facturation)
CREATE TABLE vente (
    id SERIAL PRIMARY KEY,
    date_vente TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_id INTEGER REFERENCES client(id) ON DELETE SET NULL,
    total_brut NUMERIC(12, 2) NOT NULL CHECK (total_brut >= 0),
    montant_remise NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (montant_remise >= 0),
    total_net NUMERIC(12, 2) NOT NULL CHECK (total_net >= 0),
    mode_paiement VARCHAR(30) NOT NULL CHECK (mode_paiement IN ('CASH', 'MONCASH', 'VIREMENT', 'CHEQUE'))
);

-- 4. Table des lignes de vente (détails de la facture)
CREATE TABLE ligne_vente (
    id SERIAL PRIMARY KEY,
    vente_id INTEGER NOT NULL REFERENCES vente(id) ON DELETE CASCADE,
    produit_id INTEGER NOT NULL REFERENCES produit(id),
    quantite INTEGER NOT NULL CHECK (quantite > 0),
    prix_unitaire_facture NUMERIC(12, 2) NOT NULL CHECK (prix_unitaire_facture >= 0)
);

-- 5. Index pour accélérer les opérations de recherche en caisse et rapports
CREATE INDEX idx_produit_code_barre ON produit(code_barre);
CREATE INDEX idx_vente_date ON vente(date_vente);
CREATE INDEX idx_client_telephone ON client(telephone);

-- ============================================================================
-- DONNÉES DE TEST ET INITIALISATION DE RÉFÉRENCE
-- ============================================================================

-- Insertion de produits de quincaillerie phares
INSERT INTO produit (code_barre, nom, description, prix_unitaire, quantite_stock, seuil_alerte) VALUES
('7421102', 'Ciment Portland d''Haiti 42.5R', 'Sac de ciment de haute résistance 42.5R (50kg)', 950.00, 250, 20),
('8511204', 'Peinture Satinée Blanche Lanco 1G', 'Pot de peinture blanche satinée de qualité premium', 1200.00, 45, 5),
('6312209', 'Barre de fer 1/2 pouce (12mm)', 'Barre d''acier de structure pour béton armé de 6m', 650.00, 150, 15),
('4210988', 'Pelle de chantier en acier à manche', 'Pelle ronde robuste à long manche en bois', 850.00, 12, 3),
('9510022', 'Brouette en acier robuste 80L', 'Brouette métallique avec pneu increvable', 3200.00, 8, 2);

-- Insertion de clients types pour tester le programme de fidélisation
-- Client 1 : Nouveau client (0% remise)
INSERT INTO client (nom, prenom, telephone, total_achats_cumules) VALUES
('Jean-Baptiste', 'Pierre', '509-3777-6655', 0.00);

-- Client 2 : Client fidèle (Palier 2 : 2% remise, achats cumulés > 150 000 HTG)
INSERT INTO client (nom, prenom, telephone, total_achats_cumules) VALUES
('Augustin', 'Jean-Marie', '509-3899-2211', 185000.00);

-- Client 3 : Entrepreneur majeur (Palier 3 : 5% remise, achats cumulés > 500 000 HTG)
INSERT INTO client (nom, prenom, telephone, total_achats_cumules) VALUES
('Destin', 'Marie-Claire', '509-3122-4488', 525000.00);
