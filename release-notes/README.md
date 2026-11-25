# Note di produzione

Per il tag `vX.Y.Z` fare prima commit di `release-notes/X.Y.Z.json`:

```json
{"version_code": 37, "notes": "Novità reali della versione.", "rollout_percent": 100}
```

Usare il vero versionCode, superiore a tutti quelli caricati, e note italiane
di 1–500 caratteri. rollout_percent è facoltativo (predefinito 100).
L'avvio manuale accetta ancora file TXT oppure il campo notes di questo JSON.
Le note storiche in release/ restano inalterate. Non pubblicare note segnaposto.
