# EMSense — Advertorial Listicle Project

## Produit
- **EMSense** : appareil EMS de stimulation plantaire (pad plat + wraps cheville + display digital)
- **Triple Therapy** : EMS + Chaleur + Compression
- Prix : ~~$99.99~~ → $37.45
- PDP : https://www.tryemsense.com/products/product-1

## Marché
- Cible : femmes 55-75 ans, douleurs pieds chroniques (neuropathie, brûlures, engourdissement)
- Sophistication Stage 4-5 (marché sceptique, aware)
- Approche DRM Agora-style : chaîne de 6 croyances émotionnelles
- Big Idea : "Vos pieds ont perdu la connexion" (Nerve Disconnect Effect)

## Fichiers research (NE PAS PUSH)
- `Research emsense.txt` — Intelligence marché, 4 segments démographiques
- `avatar emense.txt` — Avatar, parcours émotionnel, verbatims
- `EMSense Offer Brief.txt` — Big Idea, UMP, UMS, funnel architecture
- `EMSense necesary beliefs.txt` — 6 croyances d'achat Agora-style

## Page advertorial
- **`listicle.html`** = fichier source (copie de travail)
- **`index.html`** = copie identique pour GitHub Pages (servie à la racine)
- Structure : Advertorial banner → Hero → 5 sections émotionnelles → Mid-article CTA → Solution + produit → Triple Therapy cards → 3 Steps → CTA → Offre spéciale → Garantie → Testimonials → Reviews → Final CTA → Footer disclaimers → Sticky mobile CTA
- Fonts : Lora (headings) + Inter (body)
- Palette : `--bg: #faf8f4`, `--cta: #c0502d`, `--accent: #2b6e5f`
- Tous les CTA pointent vers : `https://www.tryemsense.com/products/product-1`
- Mobile-first responsive, sticky CTA avec threshold dynamique (recalculé dans rAF)

## Images (dossier `images/`)
### Lifestyle (générées avec OpenAI gpt-image-1, 1536x1024)
- `hero.png` — Femme 60s promenant son golden retriever, matin ensoleillé
- `sleeping.png` — Femme dormant paisiblement
- `walking.png` — Couple 60s marchant main dans la main, golden hour
- `grandchildren.png` — Grand-mère jouant avec petit-enfant dans le jardin
- `freedom.png` — Femme sortant de chez elle, matin lumineux
- `identity.png` — Portrait femme confiante dans jardin fleuri (1024x1536, vertical)

### Produit
- `product_beauty.png` — Image officielle téléchargée du CDN tryemsense
- `product_inuse.png` — Générée gpt-image-1 (à remplacer par Gemini pour meilleur respect du design)
- `product_closeup.png` — Générée gpt-image-1 (à remplacer par Gemini pour meilleur respect du design)

## Scripts (NE PAS PUSH)
- `generate_images.py` — Génération des 6 lifestyle via OpenAI gpt-image-1
- `gen_product_gemini.py` — Tentative génération produit via Gemini (gemini-3-pro-image-preview) — 503 au moment du test

## GitHub Pages
- **Repo** : https://github.com/WesleyTHL/emsense-lp
- **Branche** : `main`
- **Domaine custom** : `footpaincare.com`
- **HTTPS** : enforced (Let's Encrypt)
- **DNS** (configuré sur Spaceship) :
  - A record : `@` → `185.199.108.153`
  - CNAME : `www` → `wesleythl.github.io`
- Fichier `CNAME` dans le repo

## Clés API utilisées
- **OpenAI** : clé `sk-proj-Ici...` (dans generate_images.py — NE PAS COMMIT)
- **Gemini** : KEY_ALT `AIzaSyATcP...` et KEY `AIzaSyC9Fan...` (dans .env plafonniers)

## TODO
- [ ] Régénérer `product_inuse.png` et `product_closeup.png` avec Gemini quand l'API sera disponible (meilleur respect du design réel du produit)
- [ ] Vérifier le rendu live sur `https://footpaincare.com/` une fois le SSL provisionné
