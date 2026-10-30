"""
Partie H — Détection de fraude bancaire (classification binaire)
Jeu de données : bank_transactions.csv
"""
from pathlib import Path
import warnings

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_recall_curve,
    precision_score,
    recall_score,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

warnings.filterwarnings("ignore")

ROOT = Path(__file__).resolve().parent
DATA_PATH = ROOT.parent / "bank_transactions.csv"
if not DATA_PATH.exists():
    DATA_PATH = ROOT / "bank_transactions.csv"

OUTPUT_DIR = ROOT / "outputs"
OUTPUT_DIR.mkdir(exist_ok=True)

# ---------------------------------------------------------------------------
# 1. Charger les données
# ---------------------------------------------------------------------------
df = pd.read_csv(DATA_PATH)
print("=" * 70)
print("1. Données chargées")
print(f"   Shape: {df.shape}")
print(df.head())

# ---------------------------------------------------------------------------
# 2. Proportion de fraudes et valeurs manquantes
# ---------------------------------------------------------------------------
print("\n" + "=" * 70)
print("2. Proportion de fraudes et valeurs manquantes")
fraud_counts = df["is_fraud"].value_counts()
fraud_ratio = df["is_fraud"].mean()
print(f"   Classes:\n{fraud_counts}")
print(f"   Proportion de fraudes (is_fraud=1): {fraud_ratio:.4%} ({fraud_counts.get(1, 0)} / {len(df)})")
print(f"\n   Valeurs manquantes:\n{df.isna().sum()}")
print(f"\n   % manquants:\n{(df.isna().mean() * 100).round(2)}")

# ---------------------------------------------------------------------------
# 3. Séparation entraînement / test stratifiée
# ---------------------------------------------------------------------------
ID_COLS = ["transaction_id", "account_id"]
TARGET = "is_fraud"

# Feature engineering (calculee sans fuite: uniquement colonnes pre-transaction)
df["amount_vs_avg"] = df["amount"] / df["average_amount_last_7d"].replace(0, np.nan)
df["is_night"] = ((df["hour"] >= 0) & (df["hour"] <= 5)).astype(int)
df["high_velocity"] = (df["transactions_last_24h"] >= 10).astype(int)

FEATURE_COLS = [
    "amount", "hour", "country", "merchant_category",
    "transactions_last_24h", "average_amount_last_7d",
    "amount_vs_avg", "is_night", "high_velocity",
]

X = df[FEATURE_COLS]
y = df[TARGET]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
print("\n" + "=" * 70)
print("3. Split stratifié 80/20")
print(f"   Train: {len(X_train)} (fraude={y_train.mean():.4%})")
print(f"   Test : {len(X_test)} (fraude={y_test.mean():.4%})")

# ---------------------------------------------------------------------------
# 4. Identifiants exclus (déjà fait via FEATURE_COLS)
# ---------------------------------------------------------------------------
print("\n" + "=" * 70)
print("4. Variables explicatives (identifiants exclus)")
print(f"   Features: {FEATURE_COLS}")

# ---------------------------------------------------------------------------
# 5. Pipeline : imputation, encodage, normalisation
#    (fit UNIQUEMENT sur train — pas de fuite depuis le test)
# ---------------------------------------------------------------------------
numeric_features = [
    "amount", "hour", "transactions_last_24h", "average_amount_last_7d",
    "amount_vs_avg", "is_night", "high_velocity",
]
categorical_features = ["country", "merchant_category"]

numeric_pipeline = Pipeline(
    steps=[
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler()),
    ]
)
categorical_pipeline = Pipeline(
    steps=[
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("encoder", OneHotEncoder(handle_unknown="ignore")),
    ]
)

preprocessor = ColumnTransformer(
    transformers=[
        ("num", numeric_pipeline, numeric_features),
        ("cat", categorical_pipeline, categorical_features),
    ]
)

# ---------------------------------------------------------------------------
# 6. Modèles candidats — sélection du meilleur AP sur un holdout du TRAIN
#    (le jeu de test reste intact pour l'évaluation finale)
# ---------------------------------------------------------------------------
X_fit, X_val, y_fit, y_val = train_test_split(
    X_train, y_train, test_size=0.2, random_state=42, stratify=y_train
)

candidates = {
    "logistic_regression": LogisticRegression(
        max_iter=1000, class_weight="balanced", random_state=42
    ),
    "random_forest": RandomForestClassifier(
        n_estimators=300,
        max_depth=8,
        min_samples_leaf=5,
        class_weight="balanced_subsample",
        random_state=42,
        n_jobs=-1,
    ),
    "gradient_boosting": GradientBoostingClassifier(
        n_estimators=200,
        learning_rate=0.05,
        max_depth=3,
        random_state=42,
    ),
}

print("\n" + "=" * 70)
print("6. Selection du modele (AP sur validation interne du train)")
best_name, best_ap_val, best_estimator = None, -1.0, None
for name, clf in candidates.items():
    pipe = Pipeline([("preprocess", preprocessor), ("clf", clf)])
    pipe.fit(X_fit, y_fit)
    proba_val = pipe.predict_proba(X_val)[:, 1]
    ap_val = average_precision_score(y_val, proba_val)
    print(f"   {name:22s} AP_val={ap_val:.4f}")
    if ap_val > best_ap_val:
        best_name, best_ap_val, best_estimator = name, ap_val, clf

model = Pipeline([("preprocess", preprocessor), ("clf", best_estimator)])
model.fit(X_train, y_train)
print(f"   >>> Modele retenu: {best_name}")

# Seuil fixe UNIQUEMENT sur le train (jamais sur le test)
y_train_proba = model.predict_proba(X_train)[:, 1]
precisions, recalls, thresholds = precision_recall_curve(y_train, y_train_proba)
f1s = 2 * precisions[:-1] * recalls[:-1] / (precisions[:-1] + recalls[:-1] + 1e-12)
best_idx = int(np.nanargmax(f1s))
decision_threshold = float(thresholds[best_idx])
print(f"   Seuil choisi sur TRAIN (max F1): {decision_threshold:.4f}")

# ---------------------------------------------------------------------------
# 7. Évaluation sur le test
# ---------------------------------------------------------------------------
y_proba = model.predict_proba(X_test)[:, 1]
y_pred = (y_proba >= decision_threshold).astype(int)

precision_fraud = precision_score(y_test, y_pred, pos_label=1)
recall_fraud = recall_score(y_test, y_pred, pos_label=1)
f1_fraud = f1_score(y_test, y_pred, pos_label=1)
ap = average_precision_score(y_test, y_proba)
cm = confusion_matrix(y_test, y_pred)

print("\n" + "=" * 70)
print("7. Évaluation (classe fraude = 1)")
print(f"   Precision (fraude): {precision_fraud:.4f}")
print(f"   Recall (fraude)   : {recall_fraud:.4f}")
print(f"   F1-score (fraude) : {f1_fraud:.4f}")
print(f"   Average Precision : {ap:.4f}")
print(f"\n   Matrice de confusion:\n{cm}")
print("\n" + classification_report(y_test, y_pred, target_names=["légitime", "fraude"]))

# Visualisations
fig, axes = plt.subplots(1, 2, figsize=(12, 4))
sns.heatmap(cm, annot=True, fmt="d", cmap="Blues", ax=axes[0],
            xticklabels=["légitime", "fraude"], yticklabels=["légitime", "fraude"])
axes[0].set_title("Matrice de confusion")
axes[0].set_ylabel("Réel")
axes[0].set_xlabel("Prédit")

axes[1].plot(recalls, precisions, label=f"AP={ap:.3f}")
axes[1].set_xlabel("Recall")
axes[1].set_ylabel("Precision")
axes[1].set_title("Courbe Precision-Recall (test)")
axes[1].legend()
axes[1].grid(True, alpha=0.3)
plt.tight_layout()
fig.savefig(OUTPUT_DIR / "evaluation.png", dpi=120)
print(f"\n   Figure sauvegardée: {OUTPUT_DIR / 'evaluation.png'}")

# ---------------------------------------------------------------------------
# Questions d'analyse
# ---------------------------------------------------------------------------
print("\n" + "=" * 70)
print("QUESTIONS D'ANALYSE")
answers = """
1. Pourquoi l'accuracy seule est-elle insuffisante ?
   Les classes sont fortement desequilibrees (~quelques % de fraudes). Un modele
   qui predit toujours 'legitime' obtient une accuracy elevee tout en manquant
   100% des fraudes. Precision/Recall/F1/AP sur la classe fraude sont plus pertinents.

2. Compromis faux positifs vs fraudes non detectees ?
   Faux positifs (FP) = clients legitimes bloques => friction / cout operationnel.
   Faux negatifs (FN) = fraudes non detectees => pertes financieres directes.
   Le metier fixe le seuil selon le cout relatif FP vs FN.

3. Effet d'une diminution du seuil de decision ?
   Plus de transactions classees 'fraude' => recall augmente, precision diminue
   (moins de FN, plus de FP). Utile si le cout d'une fraude manquee est eleve.

4. Pourquoi privilegier une validation temporelle sur donnees reelles ?
   Les patterns de fraude evoluent dans le temps (concept drift). Un split aleatoire
   melange passe/futur et surestime les performances. Une validation temporelle
   (train sur passe, test sur futur) simule mieux le deploiement en production.
"""
print(answers)
(OUTPUT_DIR / "analysis_answers.txt").write_text(answers, encoding="utf-8")

# Sauvegarde métriques
metrics = pd.DataFrame(
    [{
        "precision_fraud": precision_fraud,
        "recall_fraud": recall_fraud,
        "f1_fraud": f1_fraud,
        "average_precision": ap,
        "threshold": decision_threshold,
        "fraud_ratio": fraud_ratio,
    }]
)
metrics.to_csv(OUTPUT_DIR / "metrics.csv", index=False)
print(f"Métriques sauvegardées: {OUTPUT_DIR / 'metrics.csv'}")
print("\nTerminé.")
